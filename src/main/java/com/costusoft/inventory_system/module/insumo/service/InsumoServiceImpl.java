package com.costusoft.inventory_system.module.insumo.service;

import com.costusoft.inventory_system.entity.Insumo;
import com.costusoft.inventory_system.repo.InsumoRepository;
import com.costusoft.inventory_system.exception.BusinessException;
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

import java.util.List;

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

        return insumoMapper.toResponse(guardado);
    }

    // ── Listar paginado ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageDTO<InsumoDTO.Response> listar(Pageable pageable) {
        Page<Insumo> page = insumoRepository.findAll(pageable);
        return PageDTO.from(page, insumoMapper::toResponse);
    }

    // ── Obtener por ID ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public InsumoDTO.Response obtenerPorId(Long id) {
        Insumo insumo = findOrThrow(id);
        return insumoMapper.toResponse(insumo);
    }

    // ── Actualizar ───────────────────────────────────────────────────────

    @Override
    public InsumoDTO.Response actualizar(Long id, InsumoDTO.Request request) {
        Insumo insumo = findOrThrow(id);

        // Validar nombre unico excluyendo el propio registro
        validarNombreUnico(request.getNombre(), id);

        // Actualiza solo los campos no nulos del request sobre la entidad existente
        insumoMapper.updateEntityFromRequest(request, insumo);

        Insumo actualizado = insumoRepository.save(insumo);
        log.info("Insumo actualizado — id: {} | nombre: '{}'", actualizado.getId(), actualizado.getNombre());

        return insumoMapper.toResponse(actualizado);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────

    @Override
    public void eliminar(Long id) {
        Insumo insumo = findOrThrow(id);
        insumoRepository.delete(insumo);
        log.info("Insumo eliminado — id: {} | nombre: '{}'", id, insumo.getNombre());
    }

    // ── Buscar por nombre ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<InsumoDTO.Response> buscarPorNombre(String nombre) {
        return insumoRepository
                .findByNombreContainingIgnoreCase(nombre.trim())
                .stream()
                .map(insumoMapper::toResponse)
                .toList();
    }

    // ── Stock bajo ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<InsumoDTO.Response> obtenerConStockBajo() {
        return insumoRepository
                .findInsumosConStockBajo()
                .stream()
                .map(insumoMapper::toResponse)
                .toList();
    }

    // ── Ajuste manual de stock ───────────────────────────────────────────

    @Override
    public InsumoDTO.Response ajustarStock(Long id, InsumoDTO.StockUpdateRequest request) {
        Insumo insumo = findOrThrow(id);

        int stockAnterior = insumo.getStock();
        insumo.setStock(request.getNuevoStock());

        Insumo actualizado = insumoRepository.save(insumo);

        log.info("Stock ajustado — insumo: '{}' | anterior: {} | nuevo: {} | observacion: '{}'",
                insumo.getNombre(),
                stockAnterior,
                request.getNuevoStock(),
                request.getObservacion());

        return insumoMapper.toResponse(actualizado);
    }

    // ── Helpers privados ─────────────────────────────────────────────────

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