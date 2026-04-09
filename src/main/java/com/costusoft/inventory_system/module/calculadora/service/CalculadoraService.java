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
 * La talla es SIEMPRE obligatoria porque los insumos de UniformeInsumo
 * están definidos por talla. Sin talla correcta = cálculo incorrecto.
 *
 * Ejemplos de tallas según el Excel real de la empresa:
 * Prendas adulto : S, M, L, XL
 * Prendas niño : 06-08, 10-12, 14-16
 * Ed. Física : igual a los anteriores según prenda
 *
 * Educación Física es UNISEX → genero puede ser null, es completamente válido.
 *
 * Dos operaciones (ambas de solo lectura — NO modifican el inventario):
 * 1. verificarDisponibilidad() — una prenda + talla
 * 2. calcularPedido() — múltiples (prenda, talla), consolida insumos
 * compartidos
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalculadoraService {

    private static final BigDecimal CERO = BigDecimal.ZERO;
    private static final BigDecimal UNO = BigDecimal.ONE;
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    private final UniformeRepository uniformeRepository;
    private final ColegioRepository colegioRepository;

    // ══════════════════════════════════════════════════════════════════════
    // MODO 1 — Verificación simple (una prenda + talla)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ¿Puedo fabricar {@code cantidad} unidades de la prenda {@code uniformeId}
     * en la talla {@code talla} con el stock actual?
     *
     * Solo evalúa los insumos configurados para ESA talla específica.
     */
    public CalculadoraDTO.Response verificarDisponibilidad(CalculadoraDTO.Request request) {
        Uniforme uniforme = uniformeRepository.findByIdWithInsumos(request.getUniformeId())
                .orElseThrow(() -> new ResourceNotFoundException("Uniforme", request.getUniformeId()));

        String talla = request.getTalla().trim().toUpperCase();
        List<UniformeInsumo> insumosParaTalla = filtrarPorTalla(uniforme, talla);

        if (insumosParaTalla.isEmpty()) {
            throw new BusinessException(
                    "La prenda '" + uniforme.getPrenda() + "' no tiene insumos configurados "
                            + "para la talla '" + talla + "'. "
                            + "Verifique la configuración o consulte GET /api/uniformes/" + uniforme.getId()
                            + "/tallas");
        }

        int cantidad = request.getCantidad();
        List<CalculadoraDTO.DetalleInsumo> detalles = new ArrayList<>();
        boolean todoDisponible = true;
        int maxFabricable = Integer.MAX_VALUE;

        for (UniformeInsumo ui : insumosParaTalla) {
            Insumo insumo = ui.getInsumo();
            BigDecimal cantBase = ui.getCantidadBase();
            BigDecimal necesario = cantBase.multiply(BigDecimal.valueOf(cantidad));
            BigDecimal stock = BigDecimal.valueOf(insumo.getStock());
            BigDecimal restante = stock.subtract(necesario);
            boolean suficiente = restante.compareTo(CERO) >= 0;

            if (!suficiente)
                todoDisponible = false;

            // Cuántas unidades se pueden fabricar con este insumo
            int maxPorEsteInsumo = cantBase.compareTo(CERO) == 0
                    ? Integer.MAX_VALUE
                    : stock.divide(cantBase, 0, RoundingMode.FLOOR).intValue();
            if (maxPorEsteInsumo < maxFabricable)
                maxFabricable = maxPorEsteInsumo;

            detalles.add(CalculadoraDTO.DetalleInsumo.builder()
                    .insumoId(insumo.getId())
                    .nombreInsumo(insumo.getNombre())
                    .unidadMedida(ui.getUnidadMedida())
                    .cantidadNecesaria(necesario.setScale(3, RoundingMode.HALF_UP))
                    .stockActual(stock)
                    .stockRestante(restante.max(CERO).setScale(3, RoundingMode.HALF_UP))
                    .suficiente(suficiente)
                    .estado(resolverEstado(stock, suficiente))
                    .build());
        }

        if (maxFabricable == Integer.MAX_VALUE)
            maxFabricable = 0;

        log.debug("Calculadora.verificar — prenda:'{}' | talla:{} | cant:{} | ok:{} | max:{}",
                uniforme.getPrenda(), talla, cantidad, todoDisponible, maxFabricable);

        return CalculadoraDTO.Response.builder()
                .uniformeId(uniforme.getId())
                .nombrePrenda(uniforme.getPrenda())
                .talla(talla)
                .tipo(uniforme.getTipo())
                .genero(uniforme.getGenero()) // null si es Ed. Física (unisex) → OK
                .cantidadSolicitada(cantidad)
                .cantidadMaximaFabricable(maxFabricable)
                .disponible(todoDisponible)
                .detalles(detalles)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // MODO 2 — Cálculo de pedido (múltiples prendas + tallas)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Calcula si hay stock para completar un pedido con múltiples (prenda, talla).
     *
     * Insumos compartidos se acumulan entre prendas:
     * Suéter-M → 1m Tela lacoste
     * Suéter-XL → 1m Tela lacoste
     * Total acumulado = 2m Tela lacoste (comparado contra el stock real)
     *
     * Calcula el factor de cumplimiento global y el insumo cuello de botella.
     *
     * Modos de uso:
     * a) prendas[] con {uniformeId, cantidad, talla} → control total por prenda
     * b) colegioId + cantidad + talla → todas las prendas del colegio
     *
     * Prendas de Educación Física (genero=null/unisex) se procesan igual que
     * cualquier otra.
     */
    public CalculadoraDTO.PedidoResponse calcularPedido(CalculadoraDTO.PedidoRequest request) {
        List<CalculadoraDTO.PrendaRequest> prendasList;
        Map<Long, Uniforme> uniformeMap;

        // ── Resolver la lista de prendas según el modo de uso ───────────────────
        if (request.getPrendas() != null && !request.getPrendas().isEmpty()) {
            // Modo B: lista explícita
            prendasList = request.getPrendas();
            uniformeMap = cargarUniformes(
                    prendasList.stream().map(CalculadoraDTO.PrendaRequest::getUniformeId).toList());
        } else if (request.getColegioId() != null) {
            // Modo A: todas las prendas del colegio
            if (request.getCantidad() == null || request.getCantidad() < 1) {
                throw new BusinessException(
                        "Cuando usas 'colegioId' también debes indicar 'cantidad' (mínimo 1).");
            }
            if (request.getTalla() == null || request.getTalla().isBlank()) {
                throw new BusinessException(
                        "Cuando usas 'colegioId' debes indicar la 'talla' (ej. 'M', '06-08'). "
                                + "Para múltiples tallas usa la lista 'prendas' con talla individual por prenda.");
            }
            if (!colegioRepository.existsById(request.getColegioId())) {
                throw new ResourceNotFoundException("Colegio", request.getColegioId());
            }
            List<Uniforme> uniformes = uniformeRepository.findByColegioIdWithInsumos(request.getColegioId());
            if (uniformes.isEmpty()) {
                throw new BusinessException(
                        "El colegio id=" + request.getColegioId() + " no tiene prendas configuradas.");
            }
            uniformeMap = new LinkedHashMap<>();
            uniformes.forEach(u -> uniformeMap.put(u.getId(), u));

            final int cantGlobal = request.getCantidad();
            final String tallaGlobal = request.getTalla().trim().toUpperCase();
            prendasList = uniformes.stream()
                    .map(u -> CalculadoraDTO.PrendaRequest.builder()
                            .uniformeId(u.getId())
                            .cantidad(cantGlobal)
                            .talla(tallaGlobal)
                            .build())
                    .toList();
        } else {
            throw new BusinessException(
                    "Debes proporcionar 'prendas' [{uniformeId, cantidad, talla}] "
                            + "o 'colegioId' + 'cantidad' + 'talla'.");
        }

        // ── Validar que cada prenda tiene insumos para la talla solicitada ──────
        for (CalculadoraDTO.PrendaRequest pr : prendasList) {
            Uniforme u = uniformeMap.get(pr.getUniformeId());
            String talla = pr.getTalla() != null ? pr.getTalla().trim().toUpperCase() : null;
            if (talla == null || talla.isBlank()) {
                throw new BusinessException(
                        "La prenda '" + u.getPrenda() + "' (id=" + u.getId() + ") no tiene talla especificada.");
            }
            if (filtrarPorTalla(u, talla).isEmpty()) {
                throw new BusinessException(
                        "La prenda '" + u.getPrenda() + "' (id=" + u.getId() + ") no tiene insumos configurados "
                                + "para la talla '" + talla + "'. "
                                + "Tallas disponibles: GET /api/uniformes/" + u.getId() + "/tallas");
            }
        }

        // ── Paso 1: calcular por prenda y acumular insumos compartidos ──────────
        Map<Long, InsumoAcumulado> acumulado = new LinkedHashMap<>();
        List<CalculadoraDTO.ResultadoPrenda> resultadosPrendas = new ArrayList<>();

        for (CalculadoraDTO.PrendaRequest pr : prendasList) {
            Uniforme uniforme = uniformeMap.get(pr.getUniformeId());
            String talla = pr.getTalla().trim().toUpperCase();
            int cantPrenda = pr.getCantidad();
            // Solo los insumos de ESTA TALLA
            List<UniformeInsumo> insumosParaTalla = filtrarPorTalla(uniforme, talla);

            List<CalculadoraDTO.DetalleInsumo> detallesPrenda = new ArrayList<>();
            boolean prendaIndividualOk = true;

            for (UniformeInsumo ui : insumosParaTalla) {
                Insumo insumo = ui.getInsumo();
                Long insumoId = insumo.getId();
                BigDecimal cantBase = ui.getCantidadBase();
                BigDecimal necesario = cantBase.multiply(BigDecimal.valueOf(cantPrenda));
                BigDecimal stock = BigDecimal.valueOf(insumo.getStock());
                BigDecimal restante = stock.subtract(necesario);
                boolean sufInd = restante.compareTo(CERO) >= 0;

                if (!sufInd)
                    prendaIndividualOk = false;

                detallesPrenda.add(CalculadoraDTO.DetalleInsumo.builder()
                        .insumoId(insumoId)
                        .nombreInsumo(insumo.getNombre())
                        .unidadMedida(ui.getUnidadMedida())
                        .cantidadNecesaria(necesario.setScale(3, RoundingMode.HALF_UP))
                        .stockActual(stock)
                        .stockRestante(restante.max(CERO).setScale(3, RoundingMode.HALF_UP))
                        .suficiente(sufInd)
                        .estado(resolverEstado(stock, sufInd))
                        .build());

                // Acumular para el resumen global — SUMA entre prendas que comparten insumo
                acumulado.merge(
                        insumoId,
                        new InsumoAcumulado(insumo, ui.getUnidadMedida(), necesario),
                        (existente, nuevo) -> {
                            existente.totalNecesario = existente.totalNecesario.add(nuevo.totalNecesario);
                            return existente;
                        });
            }

            // cantidadMaxima se rellena en el paso 3, luego de conocer el factor global
            resultadosPrendas.add(CalculadoraDTO.ResultadoPrenda.builder()
                    .uniformeId(uniforme.getId())
                    .prenda(uniforme.getPrenda())
                    .talla(talla)
                    .tipo(uniforme.getTipo())
                    .genero(uniforme.getGenero()) // null para Ed. Física (unisex) → OK
                    .cantidadSolicitada(cantPrenda)
                    .cantidadMaxima(0) // placeholder — se recalcula abajo
                    .disponibleIndividual(prendaIndividualOk)
                    .insumos(detallesPrenda)
                    .build());
        }

        // ── Paso 2: calcular factor global y construir resumen de insumos ───────
        BigDecimal factorGlobal = UNO;
        String insumoLimitanteNombre = null;
        List<CalculadoraDTO.ResumenInsumo> resumen = new ArrayList<>();

        for (InsumoAcumulado acc : acumulado.values()) {
            BigDecimal stock = BigDecimal.valueOf(acc.insumo.getStock());
            BigDecimal faltante = acc.totalNecesario.subtract(stock).max(CERO);
            boolean suficiente = stock.compareTo(acc.totalNecesario) >= 0;

            // factor de este insumo = min(stock / totalNecesario, 1)
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

        // ── Paso 3: aplicar factor global → cantidadMaxima por prenda ───────────
        final BigDecimal factorFinal = factorGlobal;
        List<CalculadoraDTO.ResultadoPrenda> prendasFinales = resultadosPrendas.stream()
                .map(rp -> CalculadoraDTO.ResultadoPrenda.builder()
                        .uniformeId(rp.getUniformeId())
                        .prenda(rp.getPrenda())
                        .talla(rp.getTalla())
                        .tipo(rp.getTipo())
                        .genero(rp.getGenero())
                        .cantidadSolicitada(rp.getCantidadSolicitada())
                        .cantidadMaxima(
                                factorFinal
                                        .multiply(BigDecimal.valueOf(rp.getCantidadSolicitada()))
                                        .setScale(0, RoundingMode.FLOOR)
                                        .intValue())
                        .disponibleIndividual(rp.isDisponibleIndividual())
                        .insumos(rp.getInsumos())
                        .build())
                .toList();

        boolean disponibleCompleto = factorGlobal.compareTo(UNO) >= 0;
        int porcentaje = factorGlobal
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.FLOOR)
                .intValue();

        log.info("Calculadora.pedido — prendas:{} | factor:{} | limitante:'{}'",
                prendasFinales.size(),
                factorGlobal.setScale(3, RoundingMode.HALF_UP),
                insumoLimitanteNombre);

        return CalculadoraDTO.PedidoResponse.builder()
                .disponibleCompleto(disponibleCompleto)
                .factorCumplimiento(factorGlobal.setScale(4, RoundingMode.HALF_UP))
                .porcentajeCumplimiento(Math.min(porcentaje, 100))
                .insumoLimitante(disponibleCompleto ? null : insumoLimitanteNombre)
                .prendas(prendasFinales)
                .resumenInsumos(resumen)
                .build();
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    /**
     * Filtra los insumos de un uniforme por talla.
     * Comparación case-insensitive y con trim para evitar espacios.
     * Educación Física (genero=null) pasa por aquí igual que cualquier prenda.
     */
    private List<UniformeInsumo> filtrarPorTalla(Uniforme uniforme, String talla) {
        return uniforme.getInsumosRequeridos().stream()
                .filter(ui -> talla.equalsIgnoreCase(
                        ui.getTalla() != null ? ui.getTalla().trim() : ""))
                .toList();
    }

    private Map<Long, Uniforme> cargarUniformes(List<Long> ids) {
        List<Uniforme> uniformes = uniformeRepository.findByIdInWithInsumos(ids);
        if (uniformes.size() != ids.size()) {
            Set<Long> encontrados = new HashSet<>();
            uniformes.forEach(u -> encontrados.add(u.getId()));
            ids.stream()
                    .filter(id -> !encontrados.contains(id))
                    .findFirst()
                    .ifPresent(id -> {
                        throw new ResourceNotFoundException("Uniforme", id);
                    });
        }
        Map<Long, Uniforme> map = new LinkedHashMap<>();
        uniformes.forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private String resolverEstado(BigDecimal stockActual, boolean suficiente) {
        if (stockActual.compareTo(CERO) == 0)
            return "Sin stock";
        if (!suficiente)
            return "Insuficiente";
        return "Disponible";
    }

    /** Estructura interna para acumular insumos compartidos entre prendas. */
    private static class InsumoAcumulado {
        final Insumo insumo;
        final String unidadMedida;
        BigDecimal totalNecesario;

        InsumoAcumulado(Insumo insumo, String unidadMedida, BigDecimal totalNecesario) {
            this.insumo = insumo;
            this.unidadMedida = unidadMedida;
            this.totalNecesario = totalNecesario;
        }
    }
}