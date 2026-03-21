package com.costusoft.inventory_system.module.colegio.service;

import com.costusoft.inventory_system.entity.Colegio;
import com.costusoft.inventory_system.repo.ColegioRepository;
import com.costusoft.inventory_system.repo.UniformeRepository;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.colegio.dto.ColegioDTO;
import com.costusoft.inventory_system.module.colegio.mapper.ColegioMapper;
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
public class ColegioServiceImpl implements ColegioService {

    private final ColegioRepository colegioRepository;
    private final UniformeRepository uniformeRepository;
    private final ColegioMapper colegioMapper;

    @Override
    public ColegioDTO.Response crear(ColegioDTO.Request request) {
        validarNombreUnico(request.getNombre(), null);

        Colegio colegio = colegioMapper.toEntity(request);
        Colegio guardado = colegioRepository.save(colegio);

        log.info("Colegio creado — id: {} | nombre: '{}'", guardado.getId(), guardado.getNombre());
        return colegioMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<ColegioDTO.Response> listar(Pageable pageable) {
        return PageDTO.from(colegioRepository.findAll(pageable), colegioMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ColegioDTO.ResponseConUniformes obtenerPorId(Long id) {
        Colegio colegio = findOrThrow(id);
        // Forzar inicializacion de la coleccion lazy dentro de la transaccion
        colegio.getUniformes().size();
        return colegioMapper.toResponseConUniformes(colegio);
    }

    @Override
    public ColegioDTO.Response actualizar(Long id, ColegioDTO.Request request) {
        Colegio colegio = findOrThrow(id);
        validarNombreUnico(request.getNombre(), id);

        colegioMapper.updateEntityFromRequest(request, colegio);
        Colegio actualizado = colegioRepository.save(colegio);

        log.info("Colegio actualizado — id: {}", id);
        return colegioMapper.toResponse(actualizado);
    }

    @Override
    public void eliminar(Long id) {
        Colegio colegio = findOrThrow(id);

        // Verificar si tiene uniformes asociados antes de eliminar
        long totalUniformes = uniformeRepository.findByColegioId(id).size();
        if (totalUniformes > 0) {
            throw new BusinessException(
                    "No se puede eliminar el colegio '" + colegio.getNombre() +
                            "' porque tiene " + totalUniformes + " uniforme(s) asociado(s). " +
                            "Elimine primero los uniformes.");
        }

        colegioRepository.delete(colegio);
        log.info("Colegio eliminado — id: {} | nombre: '{}'", id, colegio.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public long contarColegios() {
        return colegioRepository.count();
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private Colegio findOrThrow(Long id) {
        return colegioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colegio", id));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean existe = (idExcluido == null)
                ? colegioRepository.existsByNombreIgnoreCase(nombre.trim())
                : colegioRepository.existsByNombreIgnoreCaseAndIdNot(nombre.trim(), idExcluido);

        if (existe) {
            throw new BusinessException(
                    "Ya existe un colegio con el nombre '" + nombre.trim() + "'");
        }
    }
}
