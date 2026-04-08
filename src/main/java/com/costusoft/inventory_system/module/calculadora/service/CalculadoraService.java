package com.costusoft.inventory_system.module.calculadora.service;

import com.costusoft.inventory_system.entity.Insumo;
import com.costusoft.inventory_system.entity.Uniforme;
import com.costusoft.inventory_system.entity.UniformeInsumo;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.calculadora.dto.CalculadoraDTO;
import com.costusoft.inventory_system.repo.ColegioRepository;
import com.costusoft.inventory_system.repo.UniformeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/**
 * Servicio de la calculadora de disponibilidad de insumos.
 *
 * Dos operaciones:
 *
 * 1. verificarDisponibilidad() — ¿Puedo fabricar N unidades de una prenda X?
 *    Evalúa cada insumo de la prenda y calcula el máximo fabricable.
 *
 * 2. calcularPedido() — ¿Puedo completar un pedido con múltiples prendas?
 *    Agrega el consumo de insumos compartidos entre prendas.
 *    Calcula el factor de cumplimiento y el insumo limitante (cuello de botella).
 *
 * Ambas operaciones son de SOLO LECTURA — no modifican ningún dato.
 * Usa BigDecimal en todos los cálculos para evitar errores de punto flotante.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalculadoraService {

    private static final BigDecimal CERO = BigDecimal.ZERO;
    private static final BigDecimal UNO  = BigDecimal.ONE;
    private static final MathContext MC  = new MathContext(10, RoundingMode.HALF_UP);

    private final UniformeRepository uniformeRepository;
    private final ColegioRepository  colegioRepository;

    // ══════════════════════════════════════════════════════════════════════
    //  MODO 1 — Verificación simple (una prenda)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifica si hay stock suficiente para fabricar {@code cantidad} unidades
     * de la prenda indicada.
     *
     * Calcula también el máximo fabricable: cuántas unidades podrían hacerse
     * con el stock actual, limitado por el insumo más escaso.
     */
    public CalculadoraDTO.Response verificarDisponibilidad(CalculadoraDTO.Request request) {
        Uniforme uniforme = uniformeRepository.findByIdWithInsumos(request.getUniformeId())
                .orElseThrow(() -> new ResourceNotFoundException("Uniforme", request.getUniformeId()));

        if (uniforme.getInsumosRequeridos().isEmpty()) {
            throw new BusinessException(
                    "La prenda '" + uniforme.getPrenda() + "' no tiene insumos configurados. "
                    + "Agregue los insumos requeridos antes de usar la calculadora.");
        }

        int cantidad = request.getCantidad();
        List<CalculadoraDTO.DetalleInsumo> detalles = new ArrayList<>();
        boolean todoDisponible = true;
        int maxFabricable = Integer.MAX_VALUE;

        for (UniformeInsumo ui : uniforme.getInsumosRequeridos()) {
            Insumo insumo = ui.getInsumo();

            BigDecimal cantBase    = ui.getCantidadBase();
            BigDecimal necesario   = cantBase.multiply(BigDecimal.valueOf(cantidad));
            BigDecimal stockActual = BigDecimal.valueOf(insumo.getStock());
            BigDecimal restante    = stockActual.subtract(necesario);
            boolean suficiente     = restante.compareTo(CERO) >= 0;

            if (!suficiente) todoDisponible = false;

            String estado = resolverEstado(stockActual, suficiente);

            // Máximo fabricable por este insumo: floor(stock / cantidadBase)
            int maxPorEsteInsumo = cantBase.compareTo(CERO) == 0
                    ? Integer.MAX_VALUE
                    : stockActual.divide(cantBase, 0, RoundingMode.FLOOR).intValue();

            if (maxPorEsteInsumo < maxFabricable) {
                maxFabricable = maxPorEsteInsumo;
            }

            detalles.add(CalculadoraDTO.DetalleInsumo.builder()
                    .insumoId(insumo.getId())
                    .nombreInsumo(insumo.getNombre())
                    .unidadMedida(ui.getUnidadMedida())
                    .cantidadNecesaria(necesario)
                    .stockActual(stockActual)
                    .stockRestante(restante.max(CERO))
                    .suficiente(suficiente)
                    .estado(estado)
                    .build());
        }

        if (maxFabricable == Integer.MAX_VALUE) maxFabricable = 0;

        log.debug("Calculadora.verificar — prenda: '{}' | cantidad: {} | disponible: {} | maxFabricable: {}",
                uniforme.getPrenda(), cantidad, todoDisponible, maxFabricable);

        return CalculadoraDTO.Response.builder()
                .uniformeId(uniforme.getId())
                .nombrePrenda(uniforme.getPrenda())
                .talla(uniforme.getTalla())
                .tipo(uniforme.getTipo())
                .genero(uniforme.getGenero())
                .cantidadSolicitada(cantidad)
                .cantidadMaximaFabricable(maxFabricable)
                .disponible(todoDisponible)
                .detalles(detalles)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MODO 2 — Cálculo de pedido (múltiples prendas)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Calcula si hay stock para completar un pedido que involucra múltiples prendas.
     *
     * Maneja insumos compartidos: si "Tela azul" la usan Camisa (2m) y Pantalón (1.5m),
     * el resumen mostrará totalNecesario = 3.5m y comparará contra el stock real.
     *
     * Calcula el factorCumplimiento (0–1) y el insumo limitante (cuello de botella).
     * Factor = 1.0 significa que el pedido puede atenderse completo.
     *
     * Request acepta:
     *   a) colegioId + cantidad → todas las prendas del colegio × cantidad
     *   b) prendas[]            → lista explícita con cantidad individual por prenda
     */
    public CalculadoraDTO.PedidoResponse calcularPedido(CalculadoraDTO.PedidoRequest request) {
        // Resolver prendas Y cargar uniformes en un solo bloque para evitar doble query
        List<CalculadoraDTO.PrendaRequest> prendasList;
        Map<Long, Uniforme> uniformeMap;

        if (request.getPrendas() != null && !request.getPrendas().isEmpty()) {
            // Lista explícita: carga batch por IDs
            prendasList = request.getPrendas();
            List<Long> ids = prendasList.stream().map(CalculadoraDTO.PrendaRequest::getUniformeId).toList();
            uniformeMap = cargarUniformes(ids);
        } else if (request.getColegioId() != null) {
            // Por colegio: un solo query que ya trae insumos, no se vuelve a cargar
            if (request.getCantidad() == null || request.getCantidad() < 1) {
                throw new BusinessException("Debe indicar la cantidad de uniformes cuando usa colegioId.");
            }
            if (!colegioRepository.existsById(request.getColegioId())) {
                throw new ResourceNotFoundException("Colegio", request.getColegioId());
            }
            List<Uniforme> uniformes = uniformeRepository.findByColegioIdWithInsumos(request.getColegioId());
            if (uniformes.isEmpty()) {
                throw new BusinessException(
                        "El colegio con id=" + request.getColegioId() + " no tiene prendas configuradas.");
            }
            uniformeMap = new LinkedHashMap<>();
            uniformes.forEach(u -> uniformeMap.put(u.getId(), u));
            final int cant = request.getCantidad();
            prendasList = uniformes.stream()
                    .map(u -> CalculadoraDTO.PrendaRequest.builder()
                            .uniformeId(u.getId()).cantidad(cant).build())
                    .toList();
        } else {
            throw new BusinessException(
                    "Debe proporcionar 'prendas' (lista de {uniformeId, cantidad}) o 'colegioId' + 'cantidad'.");
        }

        // Validar que todos los uniformes tienen insumos configurados
        for (CalculadoraDTO.PrendaRequest pr : prendasList) {
            Uniforme u = uniformeMap.get(pr.getUniformeId());
            if (u.getInsumosRequeridos().isEmpty()) {
                throw new BusinessException(
                        "La prenda '" + u.getPrenda() + "' (id=" + u.getId() + ") no tiene insumos configurados.");
            }
        }

        // ── Paso 1: calcular por prenda y acumular agregado por insumo ──────────

        // Map ordenado: insumoId → datos acumulados
        Map<Long, InsumoAcumulado> acumulado = new LinkedHashMap<>();
        List<CalculadoraDTO.ResultadoPrenda> resultadosPrendas = new ArrayList<>();

        for (CalculadoraDTO.PrendaRequest pr : prendasList) {
            Uniforme uniforme = uniformeMap.get(pr.getUniformeId());
            int cantPrenda = pr.getCantidad();
            List<CalculadoraDTO.DetalleInsumo> detallesPrenda = new ArrayList<>();
            boolean prendaIndividualOk = true;

            for (UniformeInsumo ui : uniforme.getInsumosRequeridos()) {
                Insumo insumo = ui.getInsumo();
                Long insumoId = insumo.getId();

                BigDecimal cantBase   = ui.getCantidadBase();
                BigDecimal necesario  = cantBase.multiply(BigDecimal.valueOf(cantPrenda));
                BigDecimal stockBD    = BigDecimal.valueOf(insumo.getStock());
                BigDecimal restante   = stockBD.subtract(necesario);
                boolean sufIndividual = restante.compareTo(CERO) >= 0;

                if (!sufIndividual) prendaIndividualOk = false;

                detallesPrenda.add(CalculadoraDTO.DetalleInsumo.builder()
                        .insumoId(insumoId)
                        .nombreInsumo(insumo.getNombre())
                        .unidadMedida(ui.getUnidadMedida())
                        .cantidadNecesaria(necesario)
                        .stockActual(stockBD)
                        .stockRestante(restante.max(CERO))
                        .suficiente(sufIndividual)
                        .estado(resolverEstado(stockBD, sufIndividual))
                        .build());

                // Acumular: si el insumo ya estaba, sumar al totalNecesario
                acumulado.merge(
                        insumoId,
                        new InsumoAcumulado(insumo, ui.getUnidadMedida(), necesario),
                        (existing, nuevo) -> {
                            existing.totalNecesario = existing.totalNecesario.add(nuevo.totalNecesario);
                            return existing;
                        });
            }

            resultadosPrendas.add(CalculadoraDTO.ResultadoPrenda.builder()
                    .uniformeId(uniforme.getId())
                    .prenda(uniforme.getPrenda())
                    .talla(uniforme.getTalla())
                    .tipo(uniforme.getTipo())
                    .genero(uniforme.getGenero())
                    .cantidadSolicitada(cantPrenda)
                    .cantidadMaxima(0)          // se rellena después con el factor global
                    .disponibleIndividual(prendaIndividualOk)
                    .insumos(detallesPrenda)
                    .build());
        }

        // ── Paso 2: calcular factor de cumplimiento (cuello de botella) ──────────

        BigDecimal factorGlobal = UNO;
        String insumoLimitanteNombre = null;

        List<CalculadoraDTO.ResumenInsumo> resumen = new ArrayList<>();

        for (InsumoAcumulado acc : acumulado.values()) {
            BigDecimal stock    = BigDecimal.valueOf(acc.insumo.getStock());
            BigDecimal faltante = acc.totalNecesario.subtract(stock).max(CERO);
            boolean suficiente  = stock.compareTo(acc.totalNecesario) >= 0;

            // factorInsumo = min(1, stock / totalNecesario)
            BigDecimal factorInsumo = acc.totalNecesario.compareTo(CERO) == 0
                    ? UNO
                    : stock.divide(acc.totalNecesario, MC).min(UNO);

            if (factorInsumo.compareTo(factorGlobal) < 0) {
                factorGlobal = factorInsumo;
                insumoLimitanteNombre = acc.insumo.getNombre();
            }

            resumen.add(CalculadoraDTO.ResumenInsumo.builder()
                    .insumoId(acc.insumo.getId())
                    .nombreInsumo(acc.insumo.getNombre())
                    .unidadMedida(acc.unidadMedida)
                    .stockActual(stock)
                    .totalNecesario(acc.totalNecesario.setScale(3, RoundingMode.HALF_UP))
                    .faltante(faltante.setScale(3, RoundingMode.HALF_UP))
                    .suficiente(suficiente)
                    .estado(resolverEstado(stock, suficiente))
                    .build());
        }

        // ── Paso 3: calcular cantidadMaxima por prenda usando el factor global ───

        final BigDecimal factorFinal = factorGlobal;
        List<CalculadoraDTO.ResultadoPrenda> prendas = resultadosPrendas.stream()
                .map(rp -> CalculadoraDTO.ResultadoPrenda.builder()
                        .uniformeId(rp.getUniformeId())
                        .prenda(rp.getPrenda())
                        .talla(rp.getTalla())
                        .tipo(rp.getTipo())
                        .genero(rp.getGenero())
                        .cantidadSolicitada(rp.getCantidadSolicitada())
                        .cantidadMaxima(factorFinal
                                .multiply(BigDecimal.valueOf(rp.getCantidadSolicitada()))
                                .setScale(0, RoundingMode.FLOOR)
                                .intValue())
                        .disponibleIndividual(rp.isDisponibleIndividual())
                        .insumos(rp.getInsumos())
                        .build())
                .toList();

        boolean disponibleCompleto = factorGlobal.compareTo(UNO) >= 0;
        int porcentaje = factorGlobal.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.FLOOR)
                .intValue();

        log.debug("Calculadora.pedido — prendas: {} | factor: {} | limitante: '{}'",
                prendas.size(), factorGlobal.setScale(3, RoundingMode.HALF_UP),
                insumoLimitanteNombre);

        return CalculadoraDTO.PedidoResponse.builder()
                .disponibleCompleto(disponibleCompleto)
                .factorCumplimiento(factorGlobal.setScale(4, RoundingMode.HALF_UP))
                .porcentajeCumplimiento(Math.min(porcentaje, 100))
                .insumoLimitante(disponibleCompleto ? null : insumoLimitanteNombre)
                .prendas(prendas)
                .resumenInsumos(resumen)
                .build();
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    /**
     * Carga uniformes por IDs con sus insumos en un solo query de FETCH JOIN.
     */
    private Map<Long, Uniforme> cargarUniformes(List<Long> ids) {
        List<Uniforme> uniformes = uniformeRepository.findByIdInWithInsumos(ids);
        if (uniformes.size() != ids.size()) {
            // Detectar cuál falta
            Set<Long> encontrados = new HashSet<>();
            uniformes.forEach(u -> encontrados.add(u.getId()));
            ids.stream()
                    .filter(id -> !encontrados.contains(id))
                    .findFirst()
                    .ifPresent(id -> { throw new ResourceNotFoundException("Uniforme", id); });
        }
        Map<Long, Uniforme> map = new LinkedHashMap<>();
        uniformes.forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private String resolverEstado(BigDecimal stockActual, boolean suficiente) {
        if (stockActual.compareTo(CERO) == 0) return "Sin stock";
        if (!suficiente) return "Insuficiente";
        return "Disponible";
    }

    /** Estructura auxiliar para la acumulación de insumos compartidos. */
    private static class InsumoAcumulado {
        final Insumo insumo;
        final String unidadMedida;
        BigDecimal totalNecesario;

        InsumoAcumulado(Insumo insumo, String unidadMedida, BigDecimal totalNecesario) {
            this.insumo        = insumo;
            this.unidadMedida  = unidadMedida;
            this.totalNecesario = totalNecesario;
        }
    }
}
