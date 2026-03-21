package com.costusoft.inventory_system.module.proveedor.service;

import com.costusoft.inventory_system.module.proveedor.dto.ProveedorDTO;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import org.springframework.data.domain.Pageable;

public interface ProveedorService {

    ProveedorDTO.Response crear(ProveedorDTO.Request request);

    PageDTO<ProveedorDTO.Response> listar(Pageable pageable);

    ProveedorDTO.Response obtenerPorId(Long id);

    ProveedorDTO.Response actualizar(Long id, ProveedorDTO.Request request);

    void eliminar(Long id);
}