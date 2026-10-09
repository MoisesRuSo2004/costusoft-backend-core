package com.costusoft.inventory_system.module.mineria.service;

import com.costusoft.inventory_system.module.mineria.client.MineriaClient;
import com.costusoft.inventory_system.module.mineria.dto.MineriaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mineria de datos: pronostico dinamico (y en adelante anomalias y patrones),
 * calculado por el microservicio Python.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MineriaService {

    private final MineriaClient mineriaClient;

    public JsonNode pronostico(MineriaDTO.PronosticoRequest solicitud) {
        log.info("Pronostico dinamico — insumos: {} | horizonte: {} d | escenario: {}",
                solicitud.getInsumoIds(), solicitud.getHorizonteDias(), solicitud.getEscenario() != null);
        return mineriaClient.pronostico(solicitud);
    }

    public JsonNode anomalias(MineriaDTO.AnomaliasRequest solicitud) {
        log.info("Anomalias — {} {}..{} | sensibilidad: {}",
                solicitud.getTipo(), solicitud.getDesde(), solicitud.getHasta(), solicitud.getSensibilidad());
        return mineriaClient.anomalias(solicitud);
    }
}
