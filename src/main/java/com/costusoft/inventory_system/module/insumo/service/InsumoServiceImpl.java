package com.costusoft.inventory_system.module.insumo.service;

import com.costusoft.inventory_system.entity.Insumo;
import com.costusoft.inventory_system.repo.InsumoRepository;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.InsumoConMovimientosException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.insumo.dto.InsumoDTO;
import com.costusoft.inventory_system.module.insumo.mapper.InsumoMapper;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementacion del servicio de insumos.
 *
 * Toda la logica de negocio vive aqui:
 * - Validacion de duplicados por nombre
 * - Actualizacion parcial via mapper (NullValuePropertyMappingStrategy.IGNORE)
 * - Metodos de dominio en la entidad para incrementar/decrementar stock
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InsumoServiceImpl implements InsumoService {

    private final InsumoRepository insumoRepository;
    private final InsumoMapper insumoMapper;

    // ── Crear ────────────────────────────────────────────────────────────

    @Override
    public InsumoDTO.Response crear(InsumoDTO.Request request) {
        validarNombreUnico(request.getNombre(), null);

        Insumo insumo = insumoMapper.toEntity(request);

        // stockMinimo por defecto si no viene en el request
        if (insumo.getStockMinimo() == null) {
            insumo.setStockMinimo(10);
        }

        Insumo guardado = insumoRepository.save(insumo);
        log.info("Insumo creado — id: {} | nombre: '{}'", guardado.getId(), guardado.getNombre());

        return toResponseWithFlags(guardado);
    }

    // ── Listar paginado ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageDTO<InsumoDTO.Response> listar(Pageable pageable) {
        Page<Insumo> page = insumoRepository.findAll(pageable);
        Map<Long, InsumoDTO.Response> responsesPorId = toResponsesWithFlags(page.getContent()).stream()
                .collect(Collectors.toMap(InsumoDTO.Response::getId, Function.identity()));
        return PageDTO.from(page, insumo -> responsesPorId.get(insumo.getId()));
    }

    // ── Obtener por ID ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public InsumoDTO.Response obtenerPorId(Long id) {
        return toResponseWithFlags(findOrThrow(id));
    }

    // ── Actualizar ───────────────────────────────────────────────────────

    @Override
    public InsumoDTO.Response actualizar(Long id, InsumoDTO.Request request) {
        Insumo insumo = findOrThrow(id);

        validarNombreUnico(request.getNombre(), id);

        insumoMapper.updateEntityFromRequest(request, insumo);

        Insumo actualizado = insumoRepository.save(insumo);
        log.info("Insumo actualizado — id: {} | nombre: '{}'", actualizado.getId(), actualizado.getNombre());

        return toResponseWithFlags(actualizado);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────

    @Override
    public void eliminar(Long id) {
        Insumo insumo = findOrThrow(id);
        if (insumoRepository.tieneDetallesEntrada(id) || insumoRepository.tieneDetallesSalida(id)) {
            throw new InsumoConMovimientosException(insumo.getNombre());
        }
        insumoRepository.delete(insumo);
        log.info("Insumo eliminado — id: {} | nombre: '{}'", id, insumo.getNombre());
    }

    // ── Inhabilitar / Reactivar ──────────────────────────────────────────

    @Override
    public InsumoDTO.Response inhabilitar(Long id) {
        Insumo insumo = findOrThrow(id);
        insumo.setActivo(!Boolean.TRUE.equals(insumo.getActivo()));
        Insumo actualizado = insumoRepository.save(insumo);
        String accion = Boolean.TRUE.equals(actualizado.getActivo()) ? "reactivado" : "inhabilitado";
        log.info("Insumo {} — id: {} | nombre: '{}'", accion, id, insumo.getNombre());
        return toResponseWithFlags(actualizado);
    }

    // ── Buscar por nombre (solo activos) ────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<InsumoDTO.Response> buscarPorNombre(String nombre) {
        return toResponsesWithFlags(
                insumoRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre.trim()));
    }

    // ── Stock bajo ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<InsumoDTO.Response> obtenerConStockBajo() {
        return toResponsesWithFlags(insumoRepository.findInsumosConStockBajo());
    }

    // ── Ajuste manual de stock ───────────────────────────────────────────

    @Override
    public InsumoDTO.Response ajustarStock(Long id, InsumoDTO.StockUpdateRequest request) {
        Insumo insumo = findOrThrow(id);

        int stockAnterior = insumo.getStock();
        insumo.setStock(request.getNuevoStock());

        Insumo actualizado = insumoRepository.save(insumo);

        log.info("Stock ajustado — insumo: '{}' | anterior: {} | nuevo: {} | observacion: '{}'",
                insumo.getNombre(), stockAnterior, request.getNuevoStock(), request.getObservacion());

        return toResponseWithFlags(actualizado);
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    /** Convierte entidad a DTO e inyecta el flag de eliminabilidad */
    private InsumoDTO.Response toResponseWithFlags(Insumo insumo) {
        InsumoDTO.Response response = insumoMapper.toResponse(insumo);
        boolean tieneMov = insumoRepository.tieneDetallesEntrada(insumo.getId())
                        || insumoRepository.tieneDetallesSalida(insumo.getId());
        response.setPuedeEliminar(!tieneMov);
        return response;
    }

    /**
     * Version por lote de {@link #toResponseWithFlags}: calcula el flag de
     * eliminabilidad de todos los insumos con 2 consultas en total, en lugar
     * de 2 por insumo (N+1). Mantiene el orden de la lista recibida.
     */
    private List<InsumoDTO.Response> toResponsesWithFlags(List<Insumo> insumos) {
        if (insumos.isEmpty()) {
            return List.of();
        }
        List<Long> ids = insumos.stream().map(Insumo::getId).toList();
        Set<Long> conMovimientos = new HashSet<>(insumoRepository.findIdsConDetallesEntrada(ids));
        conMovimientos.addAll(insumoRepository.findIdsConDetallesSalida(ids));

        return insumos.stream()
                .map(insumo -> {
                    InsumoDTO.Response response = insumoMapper.toResponse(insumo);
                    response.setPuedeEliminar(!conMovimientos.contains(insumo.getId()));
                    return response;
                })
                .toList();
    }

    private Insumo findOrThrow(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo", id));
    }

    /**
     * Valida que el nombre no este en uso por otro insumo.
     *
     * @param nombre     nombre a validar
     * @param idExcluido ID del insumo actual (null en creacion, not-null en
     *                   actualizacion)
     */
    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean existe = (idExcluido == null)
                ? insumoRepository.existsByNombreIgnoreCase(nombre.trim())
                : insumoRepository.existsByNombreIgnoreCaseAndIdNot(nombre.trim(), idExcluido);

        if (existe) {
            throw new BusinessException(
                    "Ya existe un insumo con el nombre '" + nombre.trim() + "'");
        }
    }
}