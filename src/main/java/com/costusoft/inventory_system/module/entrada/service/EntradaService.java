package com.costusoft.inventory_system.module.entrada.service;

import com.costusoft.inventory_system.module.entrada.dto.EntradaDTO;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import org.springframework.data.domain.Pageable;

public interface EntradaService {

    /** Registra una entrada y suma stock a cada insumo involucrado */
    EntradaDTO.Response crear(EntradaDTO.Request request);

    /** Lista todas las entradas paginadas ordenadas por fecha desc */
    PageDTO<EntradaDTO.Response> listar(Pageable pageable);

    /** Obtiene una entrada por ID con sus detalles */
    EntradaDTO.Response obtenerPorId(Long id);

    /**
     * Actualiza una entrada:
     * 1. Revierte el stock de los detalles originales
     * 2. Aplica el nuevo stock con los detalles actualizados
     */
    EntradaDTO.Response actualizar(Long id, EntradaDTO.Request request);

    /**
     * Elimina una entrada.
     * ADVERTENCIA: no revierte el stock automaticamente —
     * se debe registrar una salida de ajuste si corresponde.
     */
    void eliminar(Long id);
}
