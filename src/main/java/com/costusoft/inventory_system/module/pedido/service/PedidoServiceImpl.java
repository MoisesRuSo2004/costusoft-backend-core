package com.costusoft.inventory_system.module.pedido.service;

import com.costusoft.inventory_system.entity.*;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.exception.StockInsuficienteException;
import com.costusoft.inventory_system.module.calculadora.dto.CalculadoraDTO;
import com.costusoft.inventory_system.module.calculadora.service.CalculadoraService;
import com.costusoft.inventory_system.module.pedido.dto.PedidoDTO;
import com.costusoft.inventory_system.repo.*;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Implementación del servicio de pedidos.
 *
 * Reglas críticas:
 *   - Stock solo se toca al ENTREGAR (confirma Salida → BODEGA descuenta).
 *   - Al CANCELAR se rechaza la Salida vinculada (stock intacto).
 *   - Solo BORRADOR puede editarse — estados posteriores son inmutables.
 *   - calcular() puede re-ejecutarse en BORRADOR o CALCULADO para actualizar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository          pedidoRepository;
    private final PedidoHistorialRepository  historialRepository;
    private final ColegioRepository          colegioRepository;
    private final UniformeRepository         uniformeRepository;
    private final SalidaRepository           salidaRepository;
    private final InsumoRepository           insumoRepository;
    private final CalculadoraService         calculadoraService;

    // ── Validación inline de colegio ─────────────────────────────────────
    // Se valida aquí para no generar dependencia circular con ColegioService

    private static final DateTimeFormatter DT_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ══════════════════════════════════════════════════════════════════════
    //  CRUD BASE
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public PedidoDTO.Response crear(PedidoDTO.Request request, String username) {
        Colegio colegio = resolverOCrearColegio(request);

        Pedido pedido = Pedido.builder()
                .colegio(colegio)
                .fechaEstimadaEntrega(request.getFechaEstimadaEntrega())
                .observaciones(request.getObservaciones())
                .creadoPor(username)
                .estado(EstadoPedido.BORRADOR)
                .build();

        // Agregar detalles sin tocar stock
        buildDetalles(request.getDetalles()).forEach(pedido::agregarDetalle);

        Pedido saved = pedidoRepository.save(pedido);

        // Generar número legible después del save (ya tenemos el ID)
        saved.setNumeroPedido(generarNumeroPedido(saved.getId()));
        saved = pedidoRepository.save(saved);

        registrarHistorial(saved, null, EstadoPedido.BORRADOR,
                "Pedido creado", null, username);

        log.info("Pedido creado — id: {} | numero: {} | colegio: '{}' | prendas: {}",
                saved.getId(), saved.getNumeroPedido(),
                colegio.getNombre(), saved.getDetalles().size());

        return toResponse(saved, false);
    }

    @Override
    public PedidoDTO.Response actualizar(Long id, PedidoDTO.Request request, String username) {
        Pedido pedido = pedidoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (!pedido.esEditable()) {
            throw new BusinessException(
                    "Solo se pueden editar pedidos en BORRADOR. Estado actual: " + pedido.getEstado());
        }

        Colegio colegio = resolverOCrearColegio(request);
        pedido.setColegio(colegio);
        pedido.setFechaEstimadaEntrega(request.getFechaEstimadaEntrega());
        pedido.setObservaciones(request.getObservaciones());

        // Limpiar resultados de cálculo anterior al editar
        pedido.setFactorCumplimiento(null);
        pedido.setDisponibleCompleto(null);
        pedido.setInsumoLimitante(null);

        pedido.limpiarDetalles();
        buildDetalles(request.getDetalles()).forEach(pedido::agregarDetalle);

        Pedido updated = pedidoRepository.save(pedido);

        registrarHistorial(updated, EstadoPedido.BORRADOR, EstadoPedido.BORRADOR,
                "Pedido actualizado", null, username);

        log.info("Pedido actualizado — id: {} | prendas: {}", id, updated.getDetalles().size());
        return toResponse(updated, false);
    }

    @Override
    public void eliminar(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (!pedido.esBorrador()) {
            throw new BusinessException(
                    "Solo se pueden eliminar pedidos en BORRADOR. Estado actual: " + pedido.getEstado());
        }

        pedidoRepository.delete(pedido);
        log.warn("Pedido eliminado — id: {} | numero: {}", id, pedido.getNumeroPedido());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONSULTAS
    // ══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public PedidoDTO.Response obtenerPorId(Long id) {
        Pedido pedido = pedidoRepository.findByIdFull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
        return toResponse(pedido, true); // incluye resumenInsumos
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<PedidoDTO.Response> listar(Pageable pageable) {
        return PageDTO.from(
                pedidoRepository.findAllByOrderByCreatedAtDesc(pageable),
                p -> toResponse(p, false));
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<PedidoDTO.Response> listarPorEstado(EstadoPedido estado, Pageable pageable) {
        return PageDTO.from(
                pedidoRepository.findByEstadoOrderByCreatedAtDesc(estado, pageable),
                p -> toResponse(p, false));
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<PedidoDTO.Response> listarPorColegio(Long colegioId, Pageable pageable) {
        if (!colegioRepository.existsById(colegioId)) {
            throw new ResourceNotFoundException("Colegio", colegioId);
        }
        return PageDTO.from(
                pedidoRepository.findByColegioIdOrderByCreatedAtDesc(colegioId, pageable),
                p -> toResponse(p, false));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoDTO.HistorialResponse> obtenerHistorial(Long id) {
        if (!pedidoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pedido", id);
        }
        return historialRepository.findByPedidoIdOrderByFechaAccionDesc(id)
                .stream()
                .map(this::toHistorialResponse)
                .toList();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TRANSICIONES DE ESTADO
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Ejecuta la calculadora sobre todas las prendas del pedido.
     * Permite re-calcular si el estado es BORRADOR o CALCULADO.
     */
    @Override
    public PedidoDTO.Response calcular(Long id, String username) {
        Pedido pedido = pedidoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (!pedido.esBorrador() && !pedido.esCalculado()) {
            throw new BusinessException(
                    "Solo se puede recalcular un pedido en BORRADOR o CALCULADO. Estado: " + pedido.getEstado());
        }

        if (pedido.getDetalles().isEmpty()) {
            throw new BusinessException("El pedido no tiene prendas. Agregue al menos una prenda antes de calcular.");
        }

        // Construir request para la calculadora usando los detalles del pedido
        List<CalculadoraDTO.PrendaRequest> prendas = pedido.getDetalles().stream()
                .map(d -> CalculadoraDTO.PrendaRequest.builder()
                        .uniformeId(d.getUniforme().getId())
                        .cantidad(d.getCantidad())
                        .build())
                .toList();

        CalculadoraDTO.PedidoRequest calcRequest = new CalculadoraDTO.PedidoRequest();
        calcRequest.setPrendas(prendas);

        // Ejecutar la calculadora (read-only, no modifica stock)
        CalculadoraDTO.PedidoResponse calcResult = calculadoraService.calcularPedido(calcRequest);

        // Almacenar resultados globales en el pedido
        pedido.setFactorCumplimiento(calcResult.getFactorCumplimiento());
        pedido.setDisponibleCompleto(calcResult.isDisponibleCompleto());
        pedido.setInsumoLimitante(calcResult.getInsumoLimitante());

        // Actualizar cada detalle con su resultado individual
        Map<Long, CalculadoraDTO.ResultadoPrenda> resultMap = new HashMap<>();
        calcResult.getPrendas().forEach(rp -> resultMap.put(rp.getUniformeId(), rp));

        for (DetallePedido detalle : pedido.getDetalles()) {
            CalculadoraDTO.ResultadoPrenda rp = resultMap.get(detalle.getUniforme().getId());
            if (rp != null) {
                detalle.setCantidadMaximaFabricable(rp.getCantidadMaxima());
                detalle.setDisponibleIndividual(rp.isDisponibleIndividual());
            }
        }

        EstadoPedido estadoAnterior = pedido.getEstado();
        pedido.setEstado(EstadoPedido.CALCULADO);

        Pedido saved = pedidoRepository.save(pedido);

        String obs = String.format("Factor de cumplimiento: %.0f%% | Limitante: %s",
                calcResult.getFactorCumplimiento().multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP),
                calcResult.getInsumoLimitante() != null ? calcResult.getInsumoLimitante() : "ninguno");

        registrarHistorial(saved, estadoAnterior, EstadoPedido.CALCULADO,
                "Calculadora ejecutada", obs, username);

        log.info("Pedido calculado — id: {} | factor: {} | limitante: '{}'",
                id, calcResult.getFactorCumplimiento(), calcResult.getInsumoLimitante());

        // Para el response con resumen, necesitamos el árbol completo
        Pedido full = pedidoRepository.findByIdFull(saved.getId())
                .orElse(saved);
        return toResponse(full, true);
    }

    /**
     * Confirma el pedido: ya no es editable, listo para iniciar producción.
     * CALCULADO → CONFIRMADO
     */
    @Override
    public PedidoDTO.Response confirmar(Long id, String username) {
        Pedido pedido = pedidoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (!pedido.esCalculado()) {
            throw new BusinessException(
                    "Solo se puede confirmar un pedido CALCULADO. Estado actual: " + pedido.getEstado()
                    + ". Ejecute /calcular primero.");
        }

        pedido.setEstado(EstadoPedido.CONFIRMADO);
        Pedido saved = pedidoRepository.save(pedido);

        registrarHistorial(saved, EstadoPedido.CALCULADO, EstadoPedido.CONFIRMADO,
                "Pedido confirmado", null, username);

        log.info("Pedido confirmado — id: {} | por: {}", id, username);
        return toResponse(saved, false);
    }

    /**
     * Inicia la producción: agrega todos los insumos necesarios del pedido
     * en una Salida PENDIENTE. BODEGA confirmará cuando retiren físicamente.
     * CONFIRMADO → EN_PRODUCCION
     */
    @Override
    public PedidoDTO.Response iniciarProduccion(Long id, String username) {
        Pedido pedido = pedidoRepository.findByIdWithDetallesAndInsumos(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (!pedido.esConfirmado()) {
            throw new BusinessException(
                    "Solo se puede iniciar producción de un pedido CONFIRMADO. Estado: " + pedido.getEstado());
        }

        // Agregar los insumos de todas las prendas (respetando insumos compartidos)
        Map<Long, InsumoAgregado> agregado = agregarInsumos(pedido.getDetalles());

        if (agregado.isEmpty()) {
            throw new BusinessException(
                    "El pedido no tiene insumos configurados en ninguna prenda. "
                    + "Configure los insumos requeridos en los uniformes antes de iniciar producción.");
        }

        // Crear la Salida PENDIENTE
        Salida salida = new Salida();
        salida.setFecha(LocalDate.now());
        salida.setDescripcion("Producción del pedido " + pedido.getNumeroPedido()
                + " — Colegio: " + pedido.getColegio().getNombre());
        salida.setColegio(pedido.getColegio());
        salida.setEstado(EstadoMovimiento.PENDIENTE);

        for (InsumoAgregado acc : agregado.values()) {
            // Convertir BigDecimal a Integer redondeando hacia arriba
            int cantidadInt = acc.totalNecesario.setScale(0, RoundingMode.CEILING).intValue();
            DetalleSalida detalle = DetalleSalida.builder()
                    .insumo(acc.insumo)
                    .cantidad(cantidadInt)
                    .nombreInsumoSnapshot(acc.insumo.getNombre())
                    .build();
            salida.agregarDetalle(detalle);
        }

        Salida salidaGuardada = salidaRepository.save(salida);

        pedido.setSalida(salidaGuardada);
        pedido.setEstado(EstadoPedido.EN_PRODUCCION);
        Pedido saved = pedidoRepository.save(pedido);

        registrarHistorial(saved, EstadoPedido.CONFIRMADO, EstadoPedido.EN_PRODUCCION,
                "Producción iniciada",
                "Salida PENDIENTE generada — id: " + salidaGuardada.getId()
                + " | " + agregado.size() + " tipo(s) de insumo",
                username);

        log.info("Pedido en producción — id: {} | salida: {} | insumos: {}",
                id, salidaGuardada.getId(), agregado.size());
        return toResponse(saved, false);
    }

    /**
     * Marca el pedido como listo para entrega física.
     * EN_PRODUCCION → LISTO_PARA_ENTREGA
     */
    @Override
    public PedidoDTO.Response marcarListo(Long id, String username) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (!pedido.esEnProduccion()) {
            throw new BusinessException(
                    "El pedido debe estar EN_PRODUCCION para marcarlo listo. Estado: " + pedido.getEstado());
        }

        pedido.setEstado(EstadoPedido.LISTO_PARA_ENTREGA);
        Pedido saved = pedidoRepository.save(pedido);

        registrarHistorial(saved, EstadoPedido.EN_PRODUCCION, EstadoPedido.LISTO_PARA_ENTREGA,
                "Producción completada, listo para entrega", null, username);

        log.info("Pedido listo para entrega — id: {}", id);
        return toResponse(saved, false);
    }

    /**
     * Registra la entrega al colegio.
     * Confirma la Salida vinculada → BODEGA descuenta el stock.
     * LISTO_PARA_ENTREGA → ENTREGADO
     */
    @Override
    public PedidoDTO.Response entregar(Long id, String username) {
        Pedido pedido = pedidoRepository.findByIdFull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (!pedido.esListoParaEntrega()) {
            throw new BusinessException(
                    "El pedido debe estar LISTO_PARA_ENTREGA para entregarlo. Estado: " + pedido.getEstado());
        }

        // Confirmar la Salida vinculada → descuenta stock
        if (pedido.getSalida() != null) {
            Salida salida = pedido.getSalida();
            if (salida.isPendiente()) {
                confirmarSalidaDirectamente(salida, username);
            }
        }

        pedido.setEstado(EstadoPedido.ENTREGADO);
        Pedido saved = pedidoRepository.save(pedido);

        registrarHistorial(saved, EstadoPedido.LISTO_PARA_ENTREGA, EstadoPedido.ENTREGADO,
                "Pedido entregado al colegio. Stock descontado.", null, username);

        log.info("Pedido ENTREGADO — id: {} | colegio: '{}'",
                id, pedido.getColegio().getNombre());
        return toResponse(saved, false);
    }

    /**
     * Cancela el pedido en cualquier estado no-final.
     * Rechaza la Salida si existía (stock intacto).
     */
    @Override
    public PedidoDTO.Response cancelar(Long id, String motivo, String username) {
        Pedido pedido = pedidoRepository.findByIdFull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (pedido.esFinal()) {
            throw new BusinessException(
                    "No se puede cancelar un pedido " + pedido.getEstado()
                    + ". Solo se pueden cancelar pedidos no finalizados.");
        }

        EstadoPedido estadoAnterior = pedido.getEstado();

        // Rechazar la Salida vinculada si existe y sigue PENDIENTE
        if (pedido.getSalida() != null && pedido.getSalida().isPendiente()) {
            Salida salida = pedido.getSalida();
            salida.setEstado(EstadoMovimiento.RECHAZADA);
            salida.setMotivoRechazo("Pedido " + pedido.getNumeroPedido() + " cancelado: " + motivo);
            salida.setConfirmadaPor(username);
            salida.setConfirmadaAt(LocalDateTime.now());
            salidaRepository.save(salida);
            log.info("Salida {} rechazada por cancelación del pedido {}", salida.getId(), pedido.getNumeroPedido());
        }

        pedido.setEstado(EstadoPedido.CANCELADO);
        Pedido saved = pedidoRepository.save(pedido);

        registrarHistorial(saved, estadoAnterior, EstadoPedido.CANCELADO,
                "Pedido cancelado", motivo, username);

        log.info("Pedido CANCELADO — id: {} | antes: {} | motivo: {}", id, estadoAnterior, motivo);
        return toResponse(saved, false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Resuelve el colegio del pedido:
     *  - Si viene colegioId → busca en BD (error si no existe)
     *  - Si viene nuevoColegio → crea el colegio al vuelo dentro de la misma transacción
     *  - Si ninguno → error de validación
     */
    private Colegio resolverOCrearColegio(PedidoDTO.Request request) {
        if (request.getColegioId() != null) {
            return colegioRepository.findById(request.getColegioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Colegio", request.getColegioId()));
        }

        if (request.getNuevoColegio() != null) {
            PedidoDTO.NuevoColegioRequest nc = request.getNuevoColegio();
            String nombre = nc.getNombre().trim();

            if (colegioRepository.existsByNombreIgnoreCase(nombre)) {
                throw new BusinessException(
                        "Ya existe un colegio con el nombre '" + nombre + "'. "
                        + "Usa su colegioId en lugar de crear uno nuevo.");
            }

            Colegio nuevo = new Colegio();
            nuevo.setNombre(nombre);
            nuevo.setDireccion(nc.getDireccion());
            Colegio creado = colegioRepository.save(nuevo);

            log.info("Colegio creado inline desde pedido — id: {} | nombre: '{}'",
                    creado.getId(), creado.getNombre());
            return creado;
        }

        throw new BusinessException(
                "Debe proporcionar 'colegioId' (colegio existente) "
                + "o 'nuevoColegio' (nombre + dirección para crear uno nuevo).");
    }

    private List<DetallePedido> buildDetalles(List<PedidoDTO.DetalleRequest> requests) {
        List<DetallePedido> detalles = new ArrayList<>();
        for (PedidoDTO.DetalleRequest dr : requests) {
            Uniforme uniforme = uniformeRepository.findById(dr.getUniformeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Uniforme", dr.getUniformeId()));

            // Talla: la del request si la trae, o la del uniforme como fallback
            String talla = (dr.getTalla() != null && !dr.getTalla().isBlank())
                    ? dr.getTalla().trim()
                    : uniforme.getTalla();

            detalles.add(DetallePedido.builder()
                    .uniforme(uniforme)
                    .cantidad(dr.getCantidad())
                    .talla(talla)
                    .nombreUniformeSnapshot(
                            uniforme.getPrenda()
                            + (talla != null ? " T." + talla : ""))
                    .build());
        }
        return detalles;
    }

    /**
     * Agrega los insumos de todas las prendas del pedido en un mapa consolidado.
     * Insumos compartidos se suman — no se duplican.
     */
    private Map<Long, InsumoAgregado> agregarInsumos(List<DetallePedido> detalles) {
        Map<Long, InsumoAgregado> mapa = new LinkedHashMap<>();

        for (DetallePedido detalle : detalles) {
            for (UniformeInsumo ui : detalle.getUniforme().getInsumosRequeridos()) {
                Insumo insumo = ui.getInsumo();
                BigDecimal necesario = ui.getCantidadBase()
                        .multiply(BigDecimal.valueOf(detalle.getCantidad()));

                mapa.merge(
                        insumo.getId(),
                        new InsumoAgregado(insumo, ui.getUnidadMedida(), necesario),
                        (existing, nuevo) -> {
                            existing.totalNecesario = existing.totalNecesario.add(nuevo.totalNecesario);
                            return existing;
                        });
            }
        }
        return mapa;
    }

    /**
     * Confirma directamente la Salida vinculada al pedido.
     * Valida stock de todos los insumos antes de descontar ninguno.
     */
    private void confirmarSalidaDirectamente(Salida salida, String username) {
        // Cargar detalles frescos con insumos
        Salida salidaConDetalles = salidaRepository.findByIdWithDetalles(salida.getId())
                .orElse(salida);

        // Validar stock de TODOS antes de descontar NINGUNO
        for (DetalleSalida ds : salidaConDetalles.getDetalles()) {
            Insumo insumo = insumoRepository.findById(ds.getInsumo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", ds.getInsumo().getId()));
            if (ds.getCantidad() > insumo.getStock()) {
                throw new StockInsuficienteException(
                        insumo.getNombre(), insumo.getStock(), ds.getCantidad());
            }
        }

        // Descontar stock
        for (DetalleSalida ds : salidaConDetalles.getDetalles()) {
            Insumo insumo = insumoRepository.findById(ds.getInsumo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", ds.getInsumo().getId()));
            insumo.decrementarStock(ds.getCantidad());
            insumoRepository.save(insumo);
        }

        salidaConDetalles.setEstado(EstadoMovimiento.CONFIRMADA);
        salidaConDetalles.setConfirmadaPor(username);
        salidaConDetalles.setConfirmadaAt(LocalDateTime.now());
        salidaRepository.save(salidaConDetalles);
    }

    private void registrarHistorial(Pedido pedido, EstadoPedido anterior, EstadoPedido nuevo,
                                    String accion, String observacion, String realizadoPor) {
        PedidoHistorial entrada = PedidoHistorial.builder()
                .pedido(pedido)
                .estadoAnterior(anterior)
                .estadoNuevo(nuevo)
                .accion(accion)
                .observacion(observacion)
                .realizadoPor(realizadoPor)
                .fechaAccion(LocalDateTime.now())
                .build();
        historialRepository.save(entrada);
    }

    private String generarNumeroPedido(Long id) {
        return "PED-" + LocalDate.now().getYear() + "-" + String.format("%05d", id);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MAPEO A RESPONSE
    // ══════════════════════════════════════════════════════════════════════

    private PedidoDTO.Response toResponse(Pedido pedido, boolean incluirResumen) {
        List<PedidoDTO.DetalleResponse> detallesResp = pedido.getDetalles().stream()
                .map(this::toDetalleResponse)
                .toList();

        Integer porcentaje = pedido.getFactorCumplimiento() != null
                ? pedido.getFactorCumplimiento()
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue()
                : null;

        List<PedidoDTO.ResumenInsumo> resumen = null;
        if (incluirResumen && pedido.getEstado() != EstadoPedido.BORRADOR
                && !pedido.getDetalles().isEmpty()) {
            resumen = calcularResumenInsumos(pedido);
        }

        PedidoDTO.ColegioInfo colegioInfo = null;
        if (pedido.getColegio() != null) {
            colegioInfo = PedidoDTO.ColegioInfo.builder()
                    .id(pedido.getColegio().getId())
                    .nombre(pedido.getColegio().getNombre())
                    .direccion(pedido.getColegio().getDireccion())
                    .build();
        }

        return PedidoDTO.Response.builder()
                .id(pedido.getId())
                .numeroPedido(pedido.getNumeroPedido())
                .estado(pedido.getEstado() != null ? pedido.getEstado().name() : null)
                .estadoDescripcion(describirEstado(pedido.getEstado()))
                .fechaCreacion(pedido.getCreatedAt() != null ? pedido.getCreatedAt().format(DT_FMT) : null)
                .fechaEstimadaEntrega(pedido.getFechaEstimadaEntrega() != null
                        ? pedido.getFechaEstimadaEntrega().format(DATE_FMT) : null)
                .observaciones(pedido.getObservaciones())
                .disponibleCompleto(pedido.getDisponibleCompleto())
                .factorCumplimiento(pedido.getFactorCumplimiento())
                .porcentajeCumplimiento(porcentaje)
                .insumoLimitante(pedido.getInsumoLimitante())
                .colegio(colegioInfo)
                .creadoPor(pedido.getCreadoPor())
                .salidaId(pedido.getSalida() != null ? pedido.getSalida().getId() : null)
                .detalles(detallesResp)
                .resumenInsumos(resumen)
                .updatedAt(pedido.getUpdatedAt() != null ? pedido.getUpdatedAt().format(DT_FMT) : null)
                .build();
    }

    private PedidoDTO.DetalleResponse toDetalleResponse(DetallePedido d) {
        Uniforme u = d.getUniforme();
        // Talla: la del detalle (especificada por el usuario) tiene prioridad sobre la del uniforme
        String talla = d.getTalla() != null ? d.getTalla()
                     : (u != null ? u.getTalla() : null);
        return PedidoDTO.DetalleResponse.builder()
                .id(d.getId())
                .uniformeId(u != null ? u.getId() : null)
                .nombreUniforme(d.getNombreUniformeSnapshot() != null
                        ? d.getNombreUniformeSnapshot()
                        : (u != null ? u.getPrenda() : null))
                .tipo(u != null ? u.getTipo() : null)
                .talla(talla)
                .genero(u != null ? u.getGenero() : null)
                .cantidad(d.getCantidad())
                .cantidadMaximaFabricable(d.getCantidadMaximaFabricable())
                .disponibleIndividual(d.getDisponibleIndividual())
                .build();
    }

    private PedidoDTO.HistorialResponse toHistorialResponse(PedidoHistorial h) {
        return PedidoDTO.HistorialResponse.builder()
                .id(h.getId())
                .estadoAnterior(h.getEstadoAnterior() != null ? h.getEstadoAnterior().name() : null)
                .estadoNuevo(h.getEstadoNuevo() != null ? h.getEstadoNuevo().name() : null)
                .accion(h.getAccion())
                .observacion(h.getObservacion())
                .realizadoPor(h.getRealizadoPor())
                .fechaAccion(h.getFechaAccion() != null ? h.getFechaAccion().format(DT_FMT) : null)
                .build();
    }

    /**
     * Recalcula el resumen de insumos consolidados desde los detalles cargados.
     * Requiere que los detalles tengan uniforme.insumosRequeridos.insumo cargados.
     */
    private List<PedidoDTO.ResumenInsumo> calcularResumenInsumos(Pedido pedido) {
        Map<Long, InsumoAgregado> agregado = agregarInsumos(pedido.getDetalles());
        List<PedidoDTO.ResumenInsumo> resumen = new ArrayList<>();

        for (InsumoAgregado acc : agregado.values()) {
            BigDecimal stock   = BigDecimal.valueOf(acc.insumo.getStock());
            BigDecimal faltante = acc.totalNecesario.subtract(stock).max(BigDecimal.ZERO);
            boolean suficiente  = stock.compareTo(acc.totalNecesario) >= 0;
            boolean alerta      = acc.insumo.tieneStockBajo();

            String estado;
            if (stock.compareTo(BigDecimal.ZERO) == 0) estado = "Sin stock";
            else if (!suficiente) estado = "Insuficiente";
            else estado = "Disponible";

            resumen.add(PedidoDTO.ResumenInsumo.builder()
                    .insumoId(acc.insumo.getId())
                    .nombre(acc.insumo.getNombre())
                    .unidadMedida(acc.unidadMedida)
                    .stockActual(stock)
                    .totalNecesario(acc.totalNecesario.setScale(3, RoundingMode.HALF_UP))
                    .faltante(faltante.setScale(3, RoundingMode.HALF_UP))
                    .suficiente(suficiente)
                    .estado(estado)
                    .alertaStockMinimo(alerta)
                    .build());
        }
        return resumen;
    }

    private String describirEstado(EstadoPedido estado) {
        if (estado == null) return null;
        return switch (estado) {
            case BORRADOR           -> "En edición";
            case CALCULADO          -> "Stock verificado";
            case CONFIRMADO         -> "Confirmado — listo para producción";
            case EN_PRODUCCION      -> "En producción";
            case LISTO_PARA_ENTREGA -> "Listo para entrega";
            case ENTREGADO          -> "Entregado al colegio";
            case CANCELADO          -> "Cancelado";
        };
    }

    /** Estructura auxiliar para agregar insumos compartidos entre prendas. */
    private static class InsumoAgregado {
        final Insumo insumo;
        final String unidadMedida;
        BigDecimal totalNecesario;

        InsumoAgregado(Insumo insumo, String unidadMedida, BigDecimal totalNecesario) {
            this.insumo         = insumo;
            this.unidadMedida   = unidadMedida;
            this.totalNecesario = totalNecesario;
        }
    }
}
