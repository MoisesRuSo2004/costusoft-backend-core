package com.costusoft.inventory_system.module.salida.service;

import com.costusoft.inventory_system.entity.Colegio;
import com.costusoft.inventory_system.entity.DetalleSalida;
import com.costusoft.inventory_system.entity.Insumo;
import com.costusoft.inventory_system.entity.Salida;
import com.costusoft.inventory_system.repo.ColegioRepository;
import com.costusoft.inventory_system.repo.InsumoRepository;
import com.costusoft.inventory_system.repo.SalidaRepository;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.exception.StockInsuficienteException;
import com.costusoft.inventory_system.module.salida.dto.SalidaDTO;
import com.costusoft.inventory_system.module.salida.mapper.SalidaMapper;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementacion del servicio de salidas.
 *
 * Punto critico de seguridad de stock:
 * - PRIMERO valida stock de TODOS los insumos del detalle
 * - LUEGO descuenta — garantizando que no quede stock negativo
 * aunque la transaccion falle a mitad.
 *
 * Si cualquier insumo no tiene stock suficiente se lanza
 * StockInsuficienteException antes de modificar nada.
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

    // ── Crear ────────────────────────────────────────────────────────────

    @Override
    public SalidaDTO.Response crear(SalidaDTO.Request request) {
        Salida salida = new Salida();
        salida.setFecha(request.getFecha() != null ? request.getFecha() : LocalDate.now());
        salida.setDescripcion(request.getDescripcion());

        // Resolver colegio destino si viene
        if (request.getColegioId() != null) {
            Colegio colegio = colegioRepository.findById(request.getColegioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Colegio", request.getColegioId()));
            salida.setColegio(colegio);
        }

        // Validar stock de todos los insumos ANTES de descontar
        validarStockSuficiente(request.getDetalles());

        // Construir detalles y descontar stock
        List<DetalleSalida> detalles = buildDetallesYDescontarStock(request.getDetalles());
        detalles.forEach(salida::agregarDetalle);

        Salida guardada = salidaRepository.save(salida);
        log.info("Salida creada — id: {} | detalles: {}", guardada.getId(), detalles.size());

        return toResponseConDetalles(guardada);
    }

    // ── Listar ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageDTO<SalidaDTO.Response> listar(Pageable pageable) {
        return PageDTO.from(
                salidaRepository.findAllByOrderByFechaDesc(pageable),
                this::toResponseConDetalles);
    }

    // ── Obtener por ID ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SalidaDTO.Response obtenerPorId(Long id) {
        Salida salida = salidaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));
        return toResponseConDetalles(salida);
    }

    // ── Actualizar ───────────────────────────────────────────────────────

    @Override
    public SalidaDTO.Response actualizar(Long id, SalidaDTO.Request request) {
        Salida salida = salidaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));

        // 1. Revertir stock original (sumar de vuelta)
        revertirStock(salida.getDetalles());

        // 2. Limpiar detalles (orphanRemoval los elimina)
        salida.limpiarDetalles();

        // 3. Actualizar campos del padre
        salida.setFecha(request.getFecha() != null ? request.getFecha() : salida.getFecha());
        salida.setDescripcion(request.getDescripcion());

        if (request.getColegioId() != null) {
            Colegio colegio = colegioRepository.findById(request.getColegioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Colegio", request.getColegioId()));
            salida.setColegio(colegio);
        } else {
            salida.setColegio(null);
        }

        // 4. Validar nuevo stock ANTES de descontar
        validarStockSuficiente(request.getDetalles());

        // 5. Construir nuevos detalles y descontar
        List<DetalleSalida> nuevosDetalles = buildDetallesYDescontarStock(request.getDetalles());
        nuevosDetalles.forEach(salida::agregarDetalle);

        Salida actualizada = salidaRepository.save(salida);
        log.info("Salida actualizada — id: {}", id);

        return toResponseConDetalles(actualizada);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────

    @Override
    public void eliminar(Long id) {
        Salida salida = salidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salida", id));
        salidaRepository.delete(salida);
        log.warn("Salida eliminada — id: {}. Stock NO revertido automaticamente.", id);
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    /**
     * Valida que TODOS los insumos tengan stock suficiente
     * ANTES de hacer cualquier descuento.
     *
     * Falla rapido: lanza excepcion al primer insumo con stock insuficiente.
     */
    private void validarStockSuficiente(List<SalidaDTO.DetalleRequest> detalles) {
        for (SalidaDTO.DetalleRequest dr : detalles) {
            Insumo insumo = insumoRepository.findById(dr.getInsumoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", dr.getInsumoId()));

            if (dr.getCantidad() > insumo.getStock()) {
                throw new StockInsuficienteException(
                        insumo.getNombre(),
                        insumo.getStock(),
                        dr.getCantidad());
            }
        }
    }

    /**
     * Construye los DetalleSalida y descuenta el stock de cada insumo.
     * Solo llamar DESPUES de validarStockSuficiente().
     */
    private List<DetalleSalida> buildDetallesYDescontarStock(
            List<SalidaDTO.DetalleRequest> detalleRequests) {

        List<DetalleSalida> detalles = new ArrayList<>();

        for (SalidaDTO.DetalleRequest dr : detalleRequests) {
            Insumo insumo = insumoRepository.findById(dr.getInsumoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", dr.getInsumoId()));

            // decrementarStock esta en la entidad y valida stock no negativo
            insumo.decrementarStock(dr.getCantidad());
            insumoRepository.save(insumo);

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
     * Revierte el stock sumando de vuelta las cantidades descontadas.
     * Se usa antes de aplicar los nuevos detalles en una actualizacion.
     */
    private void revertirStock(List<DetalleSalida> detalles) {
        for (DetalleSalida detalle : detalles) {
            Insumo insumo = insumoRepository.findById(detalle.getInsumo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Insumo", detalle.getInsumo().getId()));

            insumo.incrementarStock(detalle.getCantidad());
            insumoRepository.save(insumo);
        }
    }

    /**
     * Construye el Response completo con detalles mapeados.
     */
    private SalidaDTO.Response toResponseConDetalles(Salida salida) {
        List<SalidaDTO.DetalleResponse> detallesResponse = salida.getDetalles()
                .stream()
                .map(salidaMapper::detalleToResponse)
                .toList();

        return SalidaDTO.Response.builder()
                .id(salida.getId())
                .fecha(salida.getFecha() != null ? salida.getFecha().toString() : null)
                .descripcion(salida.getDescripcion())
                .colegioNombre(salida.getColegio() != null
                        ? salida.getColegio().getNombre()
                        : null)
                .detalles(detallesResponse)
                .createdAt(salida.getCreatedAt() != null
                        ? salida.getCreatedAt().toString()
                        : null)
                .build();
    }
}