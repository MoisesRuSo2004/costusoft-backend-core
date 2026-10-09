package com.costusoft.inventory_system.module.mineria.service;

import com.costusoft.inventory_system.module.mineria.client.MineriaClient;
import com.costusoft.inventory_system.module.mineria.dto.MineriaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mineria de datos: deteccion de anomalias en los movimientos,
 * calculado por el microservicio Python.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MineriaService {

    private final MineriaClient mineriaClient;

    public JsonNode anomalias(MineriaDTO.AnomaliasRequest solicitud) {
        log.info("Anomalias — {} {}..{} | sensibilidad: {}",
                solicitud.getTipo(), solicitud.getDesde(), solicitud.getHasta(), solicitud.getSensibilidad());
        return mineriaClient.anomalias(solicitud);
    }
}
