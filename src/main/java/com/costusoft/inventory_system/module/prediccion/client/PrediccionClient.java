package com.costusoft.inventory_system.module.prediccion.client;

import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.module.prediccion.dto.PrediccionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP que Spring Boot usa para llamar al microservicio Python.
 *
 * Usa RestClient (nuevo en Spring 6.1) — mas limpio que RestTemplate.
 *
 * El token secreto viaja en el header X-API-Token para que
 * solo Spring Boot pueda llamar el servicio Python directamente.
 */
@Slf4j
@Component
public class PrediccionClient {

    private final RestClient restClient;

    public PrediccionClient(
            @Value("${prediccion.service.url:http://localhost:8001}") String serviceUrl,
            @Value("${prediccion.service.token:mi-token-secreto-interno-cambiar-en-produccion}") String token) {

        this.restClient = RestClient.builder()
                .baseUrl(serviceUrl)
                .defaultHeader("X-API-Token", token)
                .defaultHeader("Content-Type", "application/json")
                .build();

        log.info("PrediccionClient inicializado — url: {}", serviceUrl);
    }

    // ── Prediccion individual ────────────────────────────────────────────

    public PrediccionDTO.Response predecirInsumo(Long insumoId) {
        try {
            return restClient.get()
                    .uri("/predict/{id}", insumoId)
                    .retrieve()
                    .body(PrediccionDTO.Response.class);
        } catch (RestClientException e) {
            log.error("Error llamando prediccion service para insumo {}: {}", insumoId, e.getMessage());
            throw new BusinessException(
                    "El servicio de prediccion no esta disponible. " +
                            "Verifique que el microservicio Python este corriendo en el puerto 8001.");
        }
    }

    // ── Prediccion masiva ─────────────────────────────────────────────────

    public PrediccionDTO.MasivaResponse predecirTodos() {
        try {
            return restClient.get()
                    .uri("/predict/todos")
                    .retrieve()
                    .body(PrediccionDTO.MasivaResponse.class);
        } catch (RestClientException e) {
            log.error("Error llamando prediccion masiva: {}", e.getMessage());
            throw new BusinessException(
                    "El servicio de prediccion no esta disponible.");
        }
    }

    // ── Disparar reentrenamiento ──────────────────────────────────────────

    public PrediccionDTO.EntrenamientoResponse entrenar() {
        try {
            return restClient.post()
                    .uri("/entrenar")
                    .retrieve()
                    .body(PrediccionDTO.EntrenamientoResponse.class);
        } catch (RestClientException e) {
            log.error("Error disparando reentrenamiento: {}", e.getMessage());
            throw new BusinessException("No se pudo iniciar el reentrenamiento del modelo.");
        }
    }

    // ── Health check ──────────────────────────────────────────────────────

    public boolean isServiceUp() {
        try {
            String response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(String.class);
            return response != null;
        } catch (Exception e) {
            log.warn("Prediccion service no disponible: {}", e.getMessage());
            return false;
        }
    }
}