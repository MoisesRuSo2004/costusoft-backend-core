package com.costusoft.inventory_system.module.prediccion.client;

import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ServicioNoDisponibleException;
import com.costusoft.inventory_system.module.prediccion.dto.PrediccionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

        // Render puede tardar ~60 s en despertar el servicio; sin limite, una
        // peticion colgada dejaria un hilo del servidor esperando para siempre.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
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
            if (ServicioNoDisponibleException.esTemporal(e)) throw ServicioNoDisponibleException.de("motor de predicción");
            throw new BusinessException("No se pudo calcular la predicción de este insumo.");
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
            if (ServicioNoDisponibleException.esTemporal(e)) throw ServicioNoDisponibleException.de("motor de predicción");
            throw new BusinessException("No se pudo calcular la predicción de los insumos.");
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