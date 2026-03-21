package com.costusoft.inventory_system.module.insumo.service;

import com.costusoft.inventory_system.module.insumo.dto.InsumoDTO;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Contrato del servicio de insumos.
 *
 * Define todas las operaciones disponibles para el modulo.
 * La implementacion (InsumoServiceImpl) contiene la logica real.
 */
public interface InsumoService {

    /** Crear un nuevo insumo — lanza BusinessException si el nombre ya existe */
    InsumoDTO.Response crear(InsumoDTO.Request request);

    /** Listar todos los insumos paginados */
    PageDTO<InsumoDTO.Response> listar(Pageable pageable);

    /** Obtener un insumo por ID — lanza ResourceNotFoundException si no existe */
    InsumoDTO.Response obtenerPorId(Long id);

    /**
     * Actualizar un insumo existente — lanza ResourceNotFoundException si no existe
     */
    InsumoDTO.Response actualizar(Long id, InsumoDTO.Request request);

    /** Eliminar un insumo — lanza ResourceNotFoundException si no existe */
    void eliminar(Long id);

    /** Buscar insumos por nombre (para autocompletado) */
    List<InsumoDTO.Response> buscarPorNombre(String nombre);

    /** Listar insumos con stock igual o menor a su stockMinimo */
    List<InsumoDTO.Response> obtenerConStockBajo();

    /** Ajuste manual de stock (sin entrada/salida formal) */
    InsumoDTO.Response ajustarStock(Long id, InsumoDTO.StockUpdateRequest request);
}
