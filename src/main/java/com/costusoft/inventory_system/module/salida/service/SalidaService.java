package com.costusoft.inventory_system.module.salida.service;

import com.costusoft.inventory_system.module.salida.dto.SalidaDTO;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import org.springframework.data.domain.Pageable;

public interface SalidaService {

    /**
     * Registra una salida y descuenta stock de cada insumo involucrado.
     * Lanza StockInsuficienteException si algun insumo no tiene stock suficiente.
     * La operacion es atomica: si falla uno, ninguno se descuenta.
     */
    SalidaDTO.Response crear(SalidaDTO.Request request);

    PageDTO<SalidaDTO.Response> listar(Pageable pageable);

    SalidaDTO.Response obtenerPorId(Long id);

    /**
     * Actualiza una salida:
     * 1. Revierte el stock de los detalles originales (suma de vuelta)
     * 2. Valida y aplica los nuevos descuentos
     */
    SalidaDTO.Response actualizar(Long id, SalidaDTO.Request request);

    void eliminar(Long id);
}
