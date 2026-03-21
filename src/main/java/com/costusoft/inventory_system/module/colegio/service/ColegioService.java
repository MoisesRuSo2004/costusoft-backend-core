package com.costusoft.inventory_system.module.colegio.service;

import com.costusoft.inventory_system.module.colegio.dto.ColegioDTO;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import org.springframework.data.domain.Pageable;

public interface ColegioService {

    ColegioDTO.Response crear(ColegioDTO.Request request);

    PageDTO<ColegioDTO.Response> listar(Pageable pageable);

    ColegioDTO.ResponseConUniformes obtenerPorId(Long id);

    ColegioDTO.Response actualizar(Long id, ColegioDTO.Request request);

    void eliminar(Long id);

    long contarColegios();
}