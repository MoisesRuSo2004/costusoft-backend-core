package com.costusoft.inventory_system.module.salida.service;

import com.costusoft.inventory_system.entity.*;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.exception.StockInsuficienteException;
import com.costusoft.inventory_system.module.salida.dto.SalidaDTO;
import com.costusoft.inventory_system.module.salida.mapper.SalidaMapper;
import com.costusoft.inventory_system.repo.ColegioRepository;
import com.costusoft.inventory_system.repo.InsumoRepository;
import com.costusoft.inventory_system.repo.SalidaRepository;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio de salidas — flujo con rol BODEGA.
 *
 * Reglas críticas de stock:
 *   - crear()    → estado PENDIENTE. Stock INTACTO.
 *   - actualizar()→ solo PENDIENTE. Stock INTACTO.
 *   - confirmar() → PENDIENTE → CONFIRMADA. Valida stock ANTES y DESCUENTA.
 *   - rechazar()  → PENDIENTE → RECHAZADA.  Stock INTACTO.
 *   - eliminar()  → solo PENDIENTE o RECHAZADA (CONFIRMADA protegida).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalidaServiceImpl implements SalidaService {

    private final SalidaRepository salidaRepository;
    private final InsumoRepository insumoRepository;
    private final ColegioRepository colegioRepository;
    private final SalidaMapper salidaMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Crear ────────────────────────────────────────────────────────────

    @Override
    public SalidaDTO.Response crear(SalidaDTO.Request request) {
        Salida salida = new Salida();
        salida.setFecha(request.getFecha() != null ? request.getFecha() : LocalDate.now());
        salida.setDescripcion(request.getDescripcion());
        salida.setEstado(EstadoMovimiento.PENDIENTE);

        if (request.getColegioId() != null) {
            Colegio colegio = colegioRepository.findById(request.getColegioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Colegio", request.getColegioId()));
            salida.setColegio(colegio);
        }

        // Construir detalles SIN descontar stock — stock se mueve al confirmar
        List<DetalleSalida> detalles = buildDetalles(request.getDetalles());
        detalles.forEach(salida::agregarDetalle);

        Salida guardada = salidaRepository.save(salida);
        log.info("Salida PENDIENTE creada — id: {} | detalles: {}", guardada.getId(), detalles.size());

        return toResponse(guardada);
    }

    // ── Listar ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageDTO<SalidaDTO.Response> listar(Pageable pageable) {
        return PageDTO.from(
                salidaRepository.findAllByOrderByFechaDesc(pageable),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<SalidaDTO.Response> listarPorEstado(EstadoMovimiento estado, Pageable pageable) {
        return PageDTO.from(
                salidaRepository.findByEstadoOrderByFechaDesc(estado, pageable),
                this::toResponse);
    }

    // ── Obtener por ID ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SalidaDTO.Response obtenerPorId(Long id) {
        Salida salida = salidaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));
        return toResponse(salida);
    }

    // ── Actualizar ───────────────────────────────────────────────────────

    @Override
    public SalidaDTO.Response actualizar(Long id, SalidaDTO.Request request) {
        Salida salida = salidaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));

        if (!salida.isPendiente()) {
            throw new BusinessException(
                    "Solo se pueden editar salidas PENDIENTES. Estado actual: " + salida.getEstado());
        }

        salida.setFecha(request.getFecha() != null ? request.getFecha() : salida.getFecha());
        salida.setDescripcion(request.getDescripcion());

        if (request.getColegioId() != null) {
            Colegio colegio = colegioRepository.findById(request.getColegioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Colegio", request.getColegioId()));
            salida.setColegio(colegio);
        } else {
            salida.setColegio(null);
        }

        // Reemplazar detalles sin tocar stock
        salida.limpiarDetalles();
        List<DetalleSalida> nuevosDetalles = buildDetalles(request.getDetalles());
        nuevosDetalles.forEach(salida::agregarDetalle);

        Salida actualizada = salidaRepository.save(salida);
        log.info("Salida PENDIENTE actualizada — id: {}", id);

        return toResponse(actualizada);
    }

    // ── Confirmar ────────────────────────────────────────────────────────

    @Override
    public SalidaDTO.Response confirmar(Long id, String username) {
        Salida salida = salidaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));

        if (!salida.isPendiente()) {
            throw new BusinessException(
                    "La salida ya fue procesada. Estado actual: " + salida.getEstado());
        }

        // Validar stock de TODOS los insumos ANTES de descontar ninguno
        validarStockSuficiente(salida.getDetalles());

        // Descontar stock de cada insumo
        for (DetalleSalida detalle : salida.getDetalles()) {
            Insumo insumo = insumoRepository.findById(detalle.getInsumo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", detalle.getInsumo().getId()));
            insumo.decrementarStock(detalle.getCantidad());
            insumoRepository.save(insumo);
        }

        salida.setEstado(EstadoMovimiento.CONFIRMADA);
        salida.setConfirmadaPor(username);
        salida.setConfirmadaAt(LocalDateTime.now());

        Salida confirmada = salidaRepository.save(salida);
        log.info("Salida CONFIRMADA — id: {} | por: {} | insumos descontados: {}",
                id, username, salida.getDetalles().size());

        return toResponse(confirmada);
    }

    // ── Rechazar ─────────────────────────────────────────────────────────

    @Override
    public SalidaDTO.Response rechazar(Long id, String motivo, String username) {
        Salida salida = salidaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));

        if (!salida.isPendiente()) {
            throw new BusinessException(
                    "La salida ya fue procesada. Estado actual: " + salida.getEstado());
        }

        salida.setEstado(EstadoMovimiento.RECHAZADA);
        salida.setMotivoRechazo(motivo);
        salida.setConfirmadaPor(username);
        salida.setConfirmadaAt(LocalDateTime.now());

        Salida rechazada = salidaRepository.save(salida);
        log.info("Salida RECHAZADA — id: {} | por: {} | motivo: {}", id, username, motivo);

        return toResponse(rechazada);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────

    @Override
    public void eliminar(Long id) {
        Salida salida = salidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));

        if (salida.isConfirmada()) {
            throw new BusinessException(
                    "No se puede eliminar una salida CONFIRMADA. El stock ya fue descontado. "
                    + "Registre una entrada de ajuste si corresponde.");
        }

        salidaRepository.delete(salida);
        log.warn("Salida eliminada — id: {} | estado previo: {}", id, salida.getEstado());
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    /**
     * Construye los DetalleSalida resolviendo cada insumo desde BD.
     * No modifica el stock (se hace al confirmar).
     */
    private List<DetalleSalida> buildDetalles(List<SalidaDTO.DetalleRequest> detalleRequests) {
        List<DetalleSalida> detalles = new ArrayList<>();

        for (SalidaDTO.DetalleRequest dr : detalleRequests) {
            Insumo insumo = insumoRepository.findById(dr.getInsumoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", dr.getInsumoId()));

            DetalleSalida detalle = DetalleSalida.builder()
                    .insumo(insumo)
                    .cantidad(dr.getCantidad())
                    .nombreInsumoSnapshot(insumo.getNombre())
                    .build();

            detalles.add(detalle);
        }

        return detalles;
    }

    /**
     * Valida que TODOS los insumos del detalle tengan stock suficiente.
     * Falla rápido: lanza excepción al primer insumo con stock insuficiente.
     * Garantiza que NO se descuente nada si alguno falla.
     */
    private void validarStockSuficiente(List<DetalleSalida> detalles) {
        for (DetalleSalida detalle : detalles) {
            Insumo insumo = insumoRepository.findById(detalle.getInsumo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", detalle.getInsumo().getId()));

            if (detalle.getCantidad() > insumo.getStock()) {
                throw new StockInsuficienteException(
                        insumo.getNombre(),
                        insumo.getStock(),
                        detalle.getCantidad());
            }
        }
    }

    /** Construye el Response completo incluyendo los nuevos campos de estado. */
    private SalidaDTO.Response toResponse(Salida salida) {
        List<SalidaDTO.DetalleResponse> detallesResponse = salida.getDetalles()
                .stream()
                .map(salidaMapper::detalleToResponse)
                .toList();

        return SalidaDTO.Response.builder()
                .id(salida.getId())
                .fecha(salida.getFecha() != null ? salida.getFecha().toString() : null)
                .descripcion(salida.getDescripcion())
                .colegioNombre(salida.getColegio() != null
                        ? salida.getColegio().getNombre() : null)
                .detalles(detallesResponse)
                .estado(salida.getEstado() != null ? salida.getEstado().name() : null)
                .confirmadaPor(salida.getConfirmadaPor())
                .motivoRechazo(salida.getMotivoRechazo())
                .confirmadaAt(salida.getConfirmadaAt() != null
                        ? salida.getConfirmadaAt().format(DT_FMT) : null)
                .createdAt(salida.getCreatedAt() != null
                        ? salida.getCreatedAt().format(DT_FMT) : null)
                .build();
    }
}
