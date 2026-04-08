package com.costusoft.inventory_system.module.salida.service;

import com.costusoft.inventory_system.entity.EstadoMovimiento;
import com.costusoft.inventory_system.module.salida.dto.SalidaDTO;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import org.springframework.data.domain.Pageable;

public interface SalidaService {

    /**
     * Registra una solicitud de salida con estado PENDIENTE.
     * NO modifica el stock — el stock se decrementa al confirmar.
     */
    SalidaDTO.Response crear(SalidaDTO.Request request);

    /** Lista todas las salidas paginadas ordenadas por fecha desc. */
    PageDTO<SalidaDTO.Response> listar(Pageable pageable);

    /** Lista salidas filtradas por estado (PENDIENTE, CONFIRMADA, RECHAZADA). */
    PageDTO<SalidaDTO.Response> listarPorEstado(EstadoMovimiento estado, Pageable pageable);

    /** Obtiene una salida por ID con sus detalles. */
    SalidaDTO.Response obtenerPorId(Long id);

    /**
     * Actualiza una salida PENDIENTE.
     * Solo se permite si estado == PENDIENTE (no hay stock que revertir).
     * Lanza BusinessException si la salida ya fue confirmada o rechazada.
     */
    SalidaDTO.Response actualizar(Long id, SalidaDTO.Request request);

    /**
     * Confirma la salida: valida stock y descuenta de cada insumo.
     * Solo BODEGA o ADMIN pueden confirmar.
     * Lanza StockInsuficienteException si algún insumo no tiene stock suficiente.
     * Lanza BusinessException si ya fue procesada.
     *
     * @param id       ID de la salida
     * @param username Username del BODEGA/ADMIN que confirma
     */
    SalidaDTO.Response confirmar(Long id, String username);

    /**
     * Rechaza la salida con un motivo. Stock intacto.
     * Solo BODEGA o ADMIN pueden rechazar.
     * Lanza BusinessException si ya fue procesada.
     *
     * @param id       ID de la salida
     * @param motivo   Motivo del rechazo (obligatorio)
     * @param username Username del BODEGA/ADMIN que rechaza
     */
    SalidaDTO.Response rechazar(Long id, String motivo, String username);

    /**
     * Elimina una salida. Solo ADMIN.
     * No se permite eliminar salidas CONFIRMADAS (el stock ya fue descontado).
     */
    void eliminar(Long id);
}
