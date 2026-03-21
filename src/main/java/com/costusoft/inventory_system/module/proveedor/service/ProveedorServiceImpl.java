package com.costusoft.inventory_system.module.proveedor.service;

import com.costusoft.inventory_system.entity.Proveedor;
import com.costusoft.inventory_system.repo.ProveedorRepository;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.proveedor.dto.ProveedorDTO;
import com.costusoft.inventory_system.module.proveedor.mapper.ProveedorMapper;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;

    @Override
    public ProveedorDTO.Response crear(ProveedorDTO.Request request) {
        validarNombreUnico(request.getNombre(), null);
        validarNitUnico(request.getNit(), null);

        Proveedor proveedor = proveedorMapper.toEntity(request);
        Proveedor guardado = proveedorRepository.save(proveedor);

        log.info("Proveedor creado — id: {} | nombre: '{}'", guardado.getId(), guardado.getNombre());
        return proveedorMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<ProveedorDTO.Response> listar(Pageable pageable) {
        return PageDTO.from(proveedorRepository.findAll(pageable), proveedorMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorDTO.Response obtenerPorId(Long id) {
        return proveedorMapper.toResponse(findOrThrow(id));
    }

    @Override
    public ProveedorDTO.Response actualizar(Long id, ProveedorDTO.Request request) {
        Proveedor proveedor = findOrThrow(id);

        validarNombreUnico(request.getNombre(), id);
        validarNitUnico(request.getNit(), id);

        proveedorMapper.updateEntityFromRequest(request, proveedor);
        Proveedor actualizado = proveedorRepository.save(proveedor);

        log.info("Proveedor actualizado — id: {}", id);
        return proveedorMapper.toResponse(actualizado);
    }

    @Override
    public void eliminar(Long id) {
        Proveedor proveedor = findOrThrow(id);
        proveedorRepository.delete(proveedor);
        log.info("Proveedor eliminado — id: {} | nombre: '{}'", id, proveedor.getNombre());
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private Proveedor findOrThrow(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", id));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean existe = (idExcluido == null)
                ? proveedorRepository.existsByNombreIgnoreCase(nombre.trim())
                : proveedorRepository.existsByNombreIgnoreCaseAndIdNot(nombre.trim(), idExcluido);

        if (existe) {
            throw new BusinessException("Ya existe un proveedor con el nombre '" + nombre.trim() + "'");
        }
    }

    private void validarNitUnico(String nit, Long idExcluido) {
        boolean existe = (idExcluido == null)
                ? proveedorRepository.existsByNit(nit.trim())
                : proveedorRepository.existsByNitAndIdNot(nit.trim(), idExcluido);

        if (existe) {
            throw new BusinessException("Ya existe un proveedor con el NIT '" + nit.trim() + "'");
        }
    }
}
