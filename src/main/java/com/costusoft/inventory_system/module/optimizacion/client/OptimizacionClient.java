package com.costusoft.inventory_system.module.optimizacion.client;

import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.module.optimizacion.dto.OptimizacionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class OptimizacionClient {

    private final RestClient restClient;

    public OptimizacionClient(
            @Value("${optimizacion.service.url:http://localhost:8002}") String serviceUrl,
            @Value("${optimizacion.service.token:mi-token-secreto-interno-cambiar-en-produccion}") String token) {

        this.restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(serviceUrl)
                .defaultHeader("X-API-Token", token)
                .defaultHeader("Content-Type", "application/json")
                .build();

        log.info("OptimizacionClient inicializado — url: {}", serviceUrl);
    }

    public OptimizacionDTO.Response optimizar(OptimizacionDTO.Request request) {
        try {
            return restClient.post()
                    .uri("/optimizar")
                    .body(request)
                    .retrieve()
                    .body(OptimizacionDTO.Response.class);
        } catch (Exception e) {
            log.error("Error llamando optimizacion service: {}", e.getMessage(), e);
            throw new BusinessException(
                    "El servicio de optimización no está disponible. " +
                            "Verifique que el microservicio Python esté corriendo en el puerto 8002.");
        }
    }

    public OptimizacionDTO.HistorialResponse historial(int limit) {
        try {
            return restClient.get()
                    .uri("/historial?limit={limit}", limit)
                    .retrieve()
                    .body(OptimizacionDTO.HistorialResponse.class);
        } catch (Exception e) {
            log.error("Error obteniendo historial de optimización: {}", e.getMessage(), e);
            throw new BusinessException("No se pudo obtener el historial de optimización.");
        }
    }

    public byte[] descargarPdf(int id) {
        try {
            return restClient.get()
                    .uri("/historial/{id}/pdf", id)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            log.error("Error descargando PDF de optimización id={}: {}", id, e.getMessage(), e);
            throw new BusinessException("No se pudo generar el PDF de optimización.");
        }
    }

    public boolean isServiceUp() {
        try {
            String response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(String.class);
            return response != null;
        } catch (Exception e) {
            log.warn("Optimizacion service no disponible: {}", e.getMessage());
            return false;
        }
    }
}
