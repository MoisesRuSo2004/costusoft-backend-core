package com.costusoft.inventory_system.module.entrada.service;

import com.costusoft.inventory_system.entity.EstadoMovimiento;
import com.costusoft.inventory_system.module.entrada.dto.EntradaDTO;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import org.springframework.data.domain.Pageable;

public interface EntradaService {

    /**
     * Registra una solicitud de entrada con estado PENDIENTE.
     * NO modifica el stock — el stock se incrementa al confirmar.
     */
    EntradaDTO.Response crear(EntradaDTO.Request request);

    /** Lista todas las entradas paginadas ordenadas por fecha desc. */
    PageDTO<EntradaDTO.Response> listar(Pageable pageable);

    /** Lista entradas filtradas por estado (PENDIENTE, CONFIRMADA, RECHAZADA). */
    PageDTO<EntradaDTO.Response> listarPorEstado(EstadoMovimiento estado, Pageable pageable);

    /** Obtiene una entrada por ID con sus detalles. */
    EntradaDTO.Response obtenerPorId(Long id);

    /**
     * Actualiza una entrada PENDIENTE.
     * Solo se permite si estado == PENDIENTE (no hay stock que revertir).
     * Lanza BusinessException si la entrada ya fue confirmada o rechazada.
     */
    EntradaDTO.Response actualizar(Long id, EntradaDTO.Request request);

    /**
     * Confirma la entrada: verifica físicamente y suma el stock de cada insumo.
     * Solo BODEGA o ADMIN pueden confirmar.
     * Lanza BusinessException si ya fue procesada.
     *
     * @param id          ID de la entrada
     * @param username    Username del BODEGA/ADMIN que confirma
     */
    EntradaDTO.Response confirmar(Long id, String username);

    /**
     * Rechaza la entrada con un motivo. Stock intacto.
     * Solo BODEGA o ADMIN pueden rechazar.
     * Lanza BusinessException si ya fue procesada.
     *
     * @param id          ID de la entrada
     * @param motivo      Motivo del rechazo (obligatorio)
     * @param username    Username del BODEGA/ADMIN que rechaza
     */
    EntradaDTO.Response rechazar(Long id, String motivo, String username);

    /**
     * Elimina una entrada. Solo ADMIN.
     * No se permite eliminar entradas CONFIRMADAS (el stock ya fue aplicado).
     */
    void eliminar(Long id);
}
