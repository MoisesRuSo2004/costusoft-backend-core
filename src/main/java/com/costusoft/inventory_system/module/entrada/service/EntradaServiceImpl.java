package com.costusoft.inventory_system.module.entrada.service;

import com.costusoft.inventory_system.entity.*;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.entrada.dto.EntradaDTO;
import com.costusoft.inventory_system.module.entrada.mapper.EntradaMapper;
import com.costusoft.inventory_system.repo.EntradaRepository;
import com.costusoft.inventory_system.repo.InsumoRepository;
import com.costusoft.inventory_system.repo.ProveedorRepository;
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
 * Implementación del servicio de entradas — flujo con rol BODEGA.
 *
 * Reglas críticas de stock:
 *   - crear()    → estado PENDIENTE. Stock INTACTO.
 *   - actualizar()→ solo PENDIENTE. Stock INTACTO.
 *   - confirmar() → PENDIENTE → CONFIRMADA. Stock INCREMENTADO.
 *   - rechazar()  → PENDIENTE → RECHAZADA.  Stock INTACTO.
 *   - eliminar()  → solo PENDIENTE o RECHAZADA (CONFIRMADA protegida).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EntradaServiceImpl implements EntradaService {

    private final EntradaRepository entradaRepository;
    private final InsumoRepository insumoRepository;
    private final ProveedorRepository proveedorRepository;
    private final EntradaMapper entradaMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Crear ────────────────────────────────────────────────────────────

    @Override
    public EntradaDTO.Response crear(EntradaDTO.Request request) {
        Entrada entrada = new Entrada();
        entrada.setFecha(request.getFecha() != null ? request.getFecha() : LocalDate.now());
        entrada.setDescripcion(request.getDescripcion());
        entrada.setEstado(EstadoMovimiento.PENDIENTE);

        if (request.getProveedorId() != null) {
            Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor", request.getProveedorId()));
            entrada.setProveedor(proveedor);
        }

        // Construir detalles SIN tocar stock — stock se mueve al confirmar
        List<DetalleEntrada> detalles = buildDetalles(request.getDetalles());
        detalles.forEach(entrada::agregarDetalle);

        Entrada guardada = entradaRepository.save(entrada);
        log.info("Entrada PENDIENTE creada — id: {} | detalles: {}", guardada.getId(), detalles.size());

        return toResponse(guardada);
    }

    // ── Listar ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageDTO<EntradaDTO.Response> listar(Pageable pageable) {
        return PageDTO.from(
                entradaRepository.findAllByOrderByFechaDesc(pageable),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<EntradaDTO.Response> listarPorEstado(EstadoMovimiento estado, Pageable pageable) {
        return PageDTO.from(
                entradaRepository.findByEstadoOrderByFechaDesc(estado, pageable),
                this::toResponse);
    }

    // ── Obtener por ID ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public EntradaDTO.Response obtenerPorId(Long id) {
        Entrada entrada = entradaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada", id));
        return toResponse(entrada);
    }

    // ── Actualizar ───────────────────────────────────────────────────────

    @Override
    public EntradaDTO.Response actualizar(Long id, EntradaDTO.Request request) {
        Entrada entrada = entradaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada", id));

        if (!entrada.isPendiente()) {
            throw new BusinessException(
                    "Solo se pueden editar entradas PENDIENTES. Estado actual: " + entrada.getEstado());
        }

        entrada.setFecha(request.getFecha() != null ? request.getFecha() : entrada.getFecha());
        entrada.setDescripcion(request.getDescripcion());

        if (request.getProveedorId() != null) {
            Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor", request.getProveedorId()));
            entrada.setProveedor(proveedor);
        } else {
            entrada.setProveedor(null);
        }

        // Reemplazar detalles sin tocar stock
        entrada.limpiarDetalles();
        List<DetalleEntrada> nuevosDetalles = buildDetalles(request.getDetalles());
        nuevosDetalles.forEach(entrada::agregarDetalle);

        Entrada actualizada = entradaRepository.save(entrada);
        log.info("Entrada PENDIENTE actualizada — id: {}", id);

        return toResponse(actualizada);
    }

    // ── Confirmar ────────────────────────────────────────────────────────

    @Override
    public EntradaDTO.Response confirmar(Long id, String username) {
        Entrada entrada = entradaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada", id));

        if (!entrada.isPendiente()) {
            throw new BusinessException(
                    "La entrada ya fue procesada. Estado actual: " + entrada.getEstado());
        }

        // Sumar stock a cada insumo del detalle
        for (DetalleEntrada detalle : entrada.getDetalles()) {
            Insumo insumo = insumoRepository.findById(detalle.getInsumo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", detalle.getInsumo().getId()));
            insumo.incrementarStock(detalle.getCantidad());
            insumoRepository.save(insumo);
        }

        entrada.setEstado(EstadoMovimiento.CONFIRMADA);
        entrada.setConfirmadaPor(username);
        entrada.setConfirmadaAt(LocalDateTime.now());

        Entrada confirmada = entradaRepository.save(entrada);
        log.info("Entrada CONFIRMADA — id: {} | por: {} | insumos actualizados: {}",
                id, username, entrada.getDetalles().size());

        return toResponse(confirmada);
    }

    // ── Rechazar ─────────────────────────────────────────────────────────

    @Override
    public EntradaDTO.Response rechazar(Long id, String motivo, String username) {
        Entrada entrada = entradaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada", id));

        if (!entrada.isPendiente()) {
            throw new BusinessException(
                    "La entrada ya fue procesada. Estado actual: " + entrada.getEstado());
        }

        entrada.setEstado(EstadoMovimiento.RECHAZADA);
        entrada.setMotivoRechazo(motivo);
        entrada.setConfirmadaPor(username);
        entrada.setConfirmadaAt(LocalDateTime.now());

        Entrada rechazada = entradaRepository.save(entrada);
        log.info("Entrada RECHAZADA — id: {} | por: {} | motivo: {}", id, username, motivo);

        return toResponse(rechazada);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────

    @Override
    public void eliminar(Long id) {
        Entrada entrada = entradaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada", id));

        if (entrada.isConfirmada()) {
            throw new BusinessException(
                    "No se puede eliminar una entrada CONFIRMADA. El stock ya fue incrementado. "
                    + "Registre una salida de ajuste si corresponde.");
        }

        entradaRepository.delete(entrada);
        log.warn("Entrada eliminada — id: {} | estado previo: {}", id, entrada.getEstado());
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    /**
     * Construye los DetalleEntrada resolviendo cada insumo desde BD.
     * No modifica el stock (se hace al confirmar).
     */
    private List<DetalleEntrada> buildDetalles(List<EntradaDTO.DetalleRequest> detalleRequests) {
        List<DetalleEntrada> detalles = new ArrayList<>();

        for (EntradaDTO.DetalleRequest dr : detalleRequests) {
            Insumo insumo = insumoRepository.findById(dr.getInsumoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", dr.getInsumoId()));

            DetalleEntrada detalle = DetalleEntrada.builder()
                    .insumo(insumo)
                    .cantidad(dr.getCantidad())
                    .nombreInsumoSnapshot(insumo.getNombre())
                    .build();

            detalles.add(detalle);
        }

        return detalles;
    }

    /** Construye el Response completo incluyendo los nuevos campos de estado. */
    private EntradaDTO.Response toResponse(Entrada entrada) {
        List<EntradaDTO.DetalleResponse> detallesResponse = entrada.getDetalles()
                .stream()
                .map(entradaMapper::detalleToResponse)
                .toList();

        return EntradaDTO.Response.builder()
                .id(entrada.getId())
                .fecha(entrada.getFecha() != null ? entrada.getFecha().toString() : null)
                .descripcion(entrada.getDescripcion())
                .proveedorNombre(entrada.getProveedor() != null
                        ? entrada.getProveedor().getNombre() : null)
                .detalles(detallesResponse)
                .estado(entrada.getEstado() != null ? entrada.getEstado().name() : null)
                .confirmadaPor(entrada.getConfirmadaPor())
                .motivoRechazo(entrada.getMotivoRechazo())
                .confirmadaAt(entrada.getConfirmadaAt() != null
                        ? entrada.getConfirmadaAt().format(DT_FMT) : null)
                .createdAt(entrada.getCreatedAt() != null
                        ? entrada.getCreatedAt().format(DT_FMT) : null)
                .build();
    }
}
