package com.costusoft.inventory_system.module.ia.client;

import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.module.ia.dto.IaDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP que se comunica con la API de Groq.
 *
 * Groq expone una API compatible con OpenAI Chat Completions.
 * Usamos Spring RestClient (Spring 6.1) — mismo patrón que PrediccionClient.
 *
 * Autenticación: Bearer token en header Authorization.
 * No requiere dependencias externas — RestClient viene con spring-boot-starter-web.
 */
@Slf4j
@Component
public class GroqClient {

    private final RestClient restClient;
    private final String model;

    public GroqClient(
            @Value("${groq.api.url:https://api.groq.com/openai/v1}") String apiUrl,
            @Value("${groq.api.key:}") String apiKey,
            @Value("${groq.api.model:llama-3.3-70b-versatile}") String model) {

        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        log.info("GroqClient inicializado — url: {} | modelo: {}", apiUrl, model);
    }

    // ── Chat completions ──────────────────────────────────────────────────

    /**
     * Envía un request de chat a Groq y retorna la respuesta completa.
     *
     * @param request Request con modelo, mensajes, maxTokens y temperature
     * @return Respuesta con texto generado y metadatos de uso
     * @throws BusinessException si Groq no está disponible o la key es inválida
     */
    public IaDTO.GroqChatResponse consultar(IaDTO.GroqChatRequest request) {
        try {
            log.debug("Enviando request a Groq — modelo: {} | tokens_max: {}",
                    request.getModel(), request.getMaxTokens());

            IaDTO.GroqChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(IaDTO.GroqChatResponse.class);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new BusinessException("Groq devolvió una respuesta vacía o inválida.");
            }

            log.debug("Respuesta Groq recibida — tokens_usados: {}",
                    response.getUsage() != null ? response.getUsage().getTotalTokens() : "N/A");

            return response;

        } catch (RestClientException e) {
            log.error("Error llamando a Groq API: {}", e.getMessage());
            throw new BusinessException(
                    "El servicio de inteligencia artificial no está disponible en este momento. " +
                    "Verifique la configuración de la API key o intente nuevamente.");
        }
    }

    // ── Health check ─────────────────────────────────────────────────────

    /**
     * Verifica que la API key esté configurada y el servicio sea alcanzable.
     * Groq no expone un endpoint /health gratuito, así que verificamos
     * que el cliente esté inicializado y la key no esté vacía.
     */
    public boolean isServiceUp() {
        return restClient != null && !model.isBlank();
    }

    // ── Getter ────────────────────────────────────────────────────────────

    public String getModel() {
        return model;
    }
}
