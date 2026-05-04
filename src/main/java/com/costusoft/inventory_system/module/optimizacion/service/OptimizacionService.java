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

    public OptimizacionDTO.Response ejecutar(String talla, boolean incluirDemanda, String usuario) {
        log.info("Ejecutando optimización: talla={}, incluirDemanda={}, usuario={}", talla, incluirDemanda, usuario);
        OptimizacionDTO.Request request = OptimizacionDTO.Request.of(talla, incluirDemanda, usuario);
        return optimizacionClient.optimizar(request);
    }

    public OptimizacionDTO.HistorialResponse historial(int limit) {
        log.info("Obteniendo historial de optimización (limit={})", limit);
        return optimizacionClient.historial(limit);
    }

    public byte[] descargarPdf(int id) {
        log.info("Descargando PDF de optimización id={}", id);
        return optimizacionClient.descargarPdf(id);
    }

    public boolean servicioDisponible() {
        return optimizacionClient.isServiceUp();
    }
}
