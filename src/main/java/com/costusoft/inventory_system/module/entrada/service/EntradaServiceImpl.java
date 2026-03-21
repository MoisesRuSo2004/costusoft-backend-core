package com.costusoft.inventory_system.module.entrada.service;

import com.costusoft.inventory_system.entity.DetalleEntrada;
import com.costusoft.inventory_system.entity.Entrada;
import com.costusoft.inventory_system.entity.Insumo;
import com.costusoft.inventory_system.entity.Proveedor;
import com.costusoft.inventory_system.repo.EntradaRepository;
import com.costusoft.inventory_system.repo.InsumoRepository;
import com.costusoft.inventory_system.repo.ProveedorRepository;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.entrada.dto.EntradaDTO;
import com.costusoft.inventory_system.module.entrada.mapper.EntradaMapper;
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
 * Implementacion del servicio de entradas.
 *
 * Reglas criticas de stock:
 * - Crear: suma stock a cada insumo del detalle
 * - Actualizar: revierte stock original → aplica nuevo stock
 * - Eliminar: NO revierte stock (operacion de auditoria — requiere salida de
 * ajuste)
 *
 * Todo corre bajo @Transactional para garantizar atomicidad:
 * si falla cualquier paso, NINGUN cambio de stock persiste.
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

    // ── Crear ────────────────────────────────────────────────────────────

    @Override
    public EntradaDTO.Response crear(EntradaDTO.Request request) {
        Entrada entrada = new Entrada();
        entrada.setFecha(request.getFecha() != null ? request.getFecha() : LocalDate.now());
        entrada.setDescripcion(request.getDescripcion());

        // Resolver proveedor si viene
        if (request.getProveedorId() != null) {
            Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor", request.getProveedorId()));
            entrada.setProveedor(proveedor);
        }

        // Construir detalles y sumar stock
        List<DetalleEntrada> detalles = buildDetallesYSumarStock(request.getDetalles());
        detalles.forEach(entrada::agregarDetalle);

        Entrada guardada = entradaRepository.save(entrada);
        log.info("Entrada creada — id: {} | detalles: {}", guardada.getId(), detalles.size());

        return toResponseConDetalles(guardada);
    }

    // ── Listar ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageDTO<EntradaDTO.Response> listar(Pageable pageable) {
        return PageDTO.from(
                entradaRepository.findAllByOrderByFechaDesc(pageable),
                this::toResponseConDetalles);
    }

    // ── Obtener por ID ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public EntradaDTO.Response obtenerPorId(Long id) {
        Entrada entrada = entradaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada", id));
        return toResponseConDetalles(entrada);
    }

    // ── Actualizar ───────────────────────────────────────────────────────

    @Override
    public EntradaDTO.Response actualizar(Long id, EntradaDTO.Request request) {
        Entrada entrada = entradaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada", id));

        // 1. Revertir stock de los detalles originales
        revertirStock(entrada.getDetalles());

        // 2. Limpiar detalles existentes (orphanRemoval los elimina de BD)
        entrada.limpiarDetalles();

        // 3. Actualizar campos del padre
        entrada.setFecha(request.getFecha() != null ? request.getFecha() : entrada.getFecha());
        entrada.setDescripcion(request.getDescripcion());

        if (request.getProveedorId() != null) {
            Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor", request.getProveedorId()));
            entrada.setProveedor(proveedor);
        } else {
            entrada.setProveedor(null);
        }

        // 4. Construir nuevos detalles y sumar nuevo stock
        List<DetalleEntrada> nuevosDetalles = buildDetallesYSumarStock(request.getDetalles());
        nuevosDetalles.forEach(entrada::agregarDetalle);

        Entrada actualizada = entradaRepository.save(entrada);
        log.info("Entrada actualizada — id: {}", id);

        return toResponseConDetalles(actualizada);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────

    @Override
    public void eliminar(Long id) {
        Entrada entrada = entradaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada", id));
        entradaRepository.delete(entrada);
        log.warn("Entrada eliminada — id: {}. Stock NO revertido automaticamente.", id);
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    /**
     * Construye los DetalleEntrada resolviendo cada insumo desde BD
     * y suma la cantidad al stock usando el metodo de dominio.
     */
    private List<DetalleEntrada> buildDetallesYSumarStock(
            List<EntradaDTO.DetalleRequest> detalleRequests) {

        List<DetalleEntrada> detalles = new ArrayList<>();

        for (EntradaDTO.DetalleRequest dr : detalleRequests) {
            Insumo insumo = insumoRepository.findById(dr.getInsumoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", dr.getInsumoId()));

            // Suma stock usando el metodo de dominio (valida cantidad > 0)
            insumo.incrementarStock(dr.getCantidad());
            insumoRepository.save(insumo);

            DetalleEntrada detalle = DetalleEntrada.builder()
                    .insumo(insumo)
                    .cantidad(dr.getCantidad())
                    .nombreInsumoSnapshot(insumo.getNombre()) // snapshot para historico
                    .build();

            detalles.add(detalle);
        }

        return detalles;
    }

    /**
     * Revierte el stock sumando de vuelta las cantidades de cada detalle.
     * Se usa antes de aplicar los nuevos detalles en una actualizacion.
     */
    private void revertirStock(List<DetalleEntrada> detalles) {
        for (DetalleEntrada detalle : detalles) {
            Insumo insumo = insumoRepository.findById(detalle.getInsumo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Insumo", detalle.getInsumo().getId()));

            // Revertir: quitar lo que habia sumado antes
            insumo.decrementarStock(detalle.getCantidad());
            insumoRepository.save(insumo);
        }
    }

    /**
     * Convierte una Entrada a Response mapeando tambien cada DetalleEntrada.
     */
    private EntradaDTO.Response toResponseConDetalles(Entrada entrada) {
        List<EntradaDTO.DetalleResponse> detallesResponse = entrada.getDetalles()
                .stream()
                .map(entradaMapper::detalleToResponse)
                .toList();

        return EntradaDTO.Response.builder()
                .id(entrada.getId())
                .fecha(entrada.getFecha() != null ? entrada.getFecha().toString() : null)
                .descripcion(entrada.getDescripcion())
                .proveedorNombre(entrada.getProveedor() != null
                        ? entrada.getProveedor().getNombre()
                        : null)
                .detalles(detallesResponse)
                .createdAt(entrada.getCreatedAt() != null
                        ? entrada.getCreatedAt().toString()
                        : null)
                .build();
    }
}
