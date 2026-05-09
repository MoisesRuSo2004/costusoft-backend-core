package com.costusoft.inventory_system.module.optimizacion.service;

import com.costusoft.inventory_system.module.optimizacion.client.OptimizacionClient;
import com.costusoft.inventory_system.module.optimizacion.dto.OptimizacionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizacionService {

    private final OptimizacionClient optimizacionClient;

    public OptimizacionDTO.Response ejecutar(Long colegioId, boolean incluirDemanda, String usuario) {
        log.info("Ejecutando optimización: colegioId={}, incluirDemanda={}, usuario={}", colegioId, incluirDemanda, usuario);
        OptimizacionDTO.Request request = OptimizacionDTO.Request.of(colegioId, incluirDemanda, usuario);
        return optimizacionClient.optimizar(request);
    }

    public OptimizacionDTO.HistorialResponse historial(int limit, Long colegioId) {
        log.info("Obteniendo historial de optimización (limit={}, colegioId={})", limit, colegioId);
        return optimizacionClient.historial(limit, colegioId);
    }

    public byte[] descargarPdf(int id) {
        log.info("Descargando PDF de optimización id={}", id);
        return optimizacionClient.descargarPdf(id);
    }

    public boolean servicioDisponible() {
        return optimizacionClient.isServiceUp();
    }
}
