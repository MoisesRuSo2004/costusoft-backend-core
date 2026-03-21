package com.costusoft.inventory_system.module.uniforme.service;

import com.costusoft.inventory_system.module.uniforme.dto.UniformeDTO;

import java.util.List;

public interface UniformeService {

    UniformeDTO.Response crear(UniformeDTO.Request request);

    List<UniformeDTO.Response> listarPorColegio(Long colegioId);

    UniformeDTO.Response obtenerPorId(Long id);

    UniformeDTO.Response actualizar(Long id, UniformeDTO.Request request);

    void eliminar(Long id);

    long contarUniformes();
}
