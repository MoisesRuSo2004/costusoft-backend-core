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

    /**
     * Retorna las tallas configuradas para una prenda.
     * Útil para poblar el dropdown de talla en el frontend antes de calcular.
     */
    List<String> listarTallas(Long uniformeId);
}
