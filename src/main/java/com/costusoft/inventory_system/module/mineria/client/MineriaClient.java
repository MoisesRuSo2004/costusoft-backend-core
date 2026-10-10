package com.costusoft.inventory_system.module.mineria.client;

import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ServicioNoDisponibleException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.mineria.dto.MineriaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente del modulo de mineria de datos del microservicio Python (/mineria/*).
 *
 * Comparte URL y token con la prediccion clasica. Tiempos de espera explicitos:
 * el analisis con validacion puede tardar varios segundos.
 */
@Slf4j
@Component
public class MineriaClient {

    private final RestClient restClient;

    public MineriaClient(
            @Value("${prediccion.service.url:http://localhost:8001}") String serviceUrl,
            @Value("${prediccion.service.token:mi-token-secreto-interno-cambiar-en-produccion}") String token) {

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(10_000);
        // Render "duerme" el microservicio: despertar + calcular puede pasar del minuto.
        fabrica.setReadTimeout(120_000);

        this.restClient = RestClient.builder()
                .requestFactory(fabrica)
                .baseUrl(serviceUrl)
                .defaultHeader("X-API-Token", token)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public JsonNode anomalias(MineriaDTO.AnomaliasRequest solicitud) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("desde", solicitud.getDesde() == null ? null : solicitud.getDesde().toString());
        cuerpo.put("hasta", solicitud.getHasta() == null ? null : solicitud.getHasta().toString());
        cuerpo.put("insumo_ids", solicitud.getInsumoIds());
        cuerpo.put("tipo", solicitud.getTipo());
        cuerpo.put("sensibilidad", solicitud.getSensibilidad());
        cuerpo.put("limite", solicitud.getLimite());
        cuerpo.put("validar", solicitud.getValidar());
        cuerpo.put("anomalias_simuladas", solicitud.getAnomaliasSimuladas());
        return enviar("/mineria/anomalias", cuerpo);
    }

    /** POST al microservicio con errores traducidos a mensajes claros. */
    private JsonNode enviar(String ruta, Map<String, Object> cuerpo) {
        try {
            return restClient.post()
                    .uri(ruta)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("Ninguno de los insumos indicados existe.");
            }
            log.error("Mineria {} respondio {}: {}", ruta, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("El servicio de mineria de datos rechazo la solicitud.");
        } catch (RestClientException e) {
            log.error("Mineria no disponible ({}): {}", ruta, e.getMessage());
            throw ServicioNoDisponibleException.de("motor de minería de datos");
        }
    }
}
