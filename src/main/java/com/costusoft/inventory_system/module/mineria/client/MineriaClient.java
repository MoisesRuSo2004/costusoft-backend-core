package com.costusoft.inventory_system.module.mineria.client;

import com.costusoft.inventory_system.exception.BusinessException;
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente del modulo de mineria de datos del microservicio Python (/mineria/*).
 *
 * Comparte URL y token con la prediccion clasica. Tiempos de espera explicitos:
 * un pronostico con escenario y backtesting puede tardar varios segundos.
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

    public JsonNode pronostico(MineriaDTO.PronosticoRequest solicitud) {
        try {
            return restClient.post()
                    .uri("/mineria/pronostico")
                    .body(aCuerpoPython(solicitud))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("Ninguno de los insumos indicados existe.");
            }
            log.error("Mineria respondio {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("El servicio de mineria de datos rechazo la solicitud.");
        } catch (RestClientException e) {
            log.error("Mineria no disponible: {}", e.getMessage());
            throw new BusinessException(
                    "El servicio de mineria de datos no esta disponible en este momento. Intenta de nuevo en un minuto.");
        }
    }

    /** El microservicio usa snake_case (Pydantic); el frontend envia camelCase. */
    private static Map<String, Object> aCuerpoPython(MineriaDTO.PronosticoRequest s) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("insumo_ids", s.getInsumoIds());
        cuerpo.put("horizonte_dias", s.getHorizonteDias());
        cuerpo.put("agrupacion", s.getAgrupacion());
        cuerpo.put("plazo_proveedor_dias", s.getPlazoProveedorDias());
        if (s.getEscenario() != null) {
            Map<String, Object> escenario = new HashMap<>();
            escenario.put("prendas", s.getEscenario().getPrendas().stream()
                    .map(p -> {
                        Map<String, Object> prenda = new HashMap<>();
                        prenda.put("uniforme_id", p.getUniformeId());
                        prenda.put("talla", p.getTalla());
                        prenda.put("cantidad", p.getCantidad());
                        return prenda;
                    })
                    .toList());
            if (s.getEscenario().getFecha() != null) {
                escenario.put("fecha", s.getEscenario().getFecha().toString());
            }
            cuerpo.put("escenario", escenario);
        }
        return cuerpo;
    }
}
