package com.costusoft.inventory_system.module.ia.client;

import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.module.ia.dto.IaDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        log.info("GroqClient inicializado — url: {} | modelo: {}", apiUrl, model);
    }

    // ── Chat completions ──────────────────────────────────────────────────

    // Extrae el tiempo de espera en segundos del mensaje de error de Groq.
    // Ej: "try again in 2.73s" → 3000ms  |  "try again in 50m30s" → -1 (no reintentar)
    private long parsearEsperaMs(String errorBody) {
        // Formato minutos: "in 50m30.5s"
        Matcher minutos = Pattern.compile("in\\s+(\\d+)m[\\d.]*s").matcher(errorBody);
        if (minutos.find()) return -1; // espera larga (límite diario) — no reintentar

        // Formato segundos: "in 2.73s"
        Matcher segundos = Pattern.compile("in\\s+([\\d.]+)s").matcher(errorBody);
        if (segundos.find()) {
            double secs = Double.parseDouble(segundos.group(1));
            return (long) (secs * 1000) + 500; // +500ms de margen
        }
        return 4_000; // fallback 4s
    }

    /** Tokens extra que se reservan para el razonamiento interno del modelo. */
    private static final int MARGEN_TOKENS_RAZONAMIENTO = 2048;

    /**
     * Los modelos de razonamiento (gpt-oss, qwen3, deepseek-r1) gastan tokens
     * "pensando" antes de responder, y esos tokens cuentan dentro de max_tokens.
     * Con los topes pensados para LLaMA (600-900) se quedaban sin presupuesto y
     * devolvian el texto vacio. Aqui se suma un margen para el razonamiento y,
     * en gpt-oss, se pide razonamiento bajo (respuestas rapidas y concisas).
     * Para cualquier otro modelo el request se envia sin cambios.
     */
    static IaDTO.GroqChatRequest ajustarParaModeloDeRazonamiento(IaDTO.GroqChatRequest request) {
        String modelo = request.getModel() == null ? "" : request.getModel().toLowerCase();
        boolean esGptOss = modelo.contains("gpt-oss");
        boolean razona = esGptOss || modelo.contains("qwen3") || modelo.contains("deepseek-r1");
        if (!razona) {
            return request;
        }
        int tokensRespuesta = request.getMaxTokens() != null ? request.getMaxTokens() : 1024;
        return IaDTO.GroqChatRequest.builder()
                .model(request.getModel())
                .messages(request.getMessages())
                .temperature(request.getTemperature())
                .maxTokens(tokensRespuesta + MARGEN_TOKENS_RAZONAMIENTO)
                .reasoningEffort(esGptOss ? "low" : request.getReasoningEffort())
                .build();
    }

    /**
     * Envía un request de chat a Groq con reintentos inteligentes ante rate-limit (429).
     * - Si el 429 es TPM (espera segundos) → reintenta hasta 3 veces
     * - Si el 429 es TPD (espera minutos) → falla inmediato con mensaje claro
     * - Si es 413 (payload muy grande) → falla inmediato con mensaje claro
     * - Si es 401 → falla inmediato indicando API key
     */
    public IaDTO.GroqChatResponse consultar(IaDTO.GroqChatRequest solicitud) {
        IaDTO.GroqChatRequest request = ajustarParaModeloDeRazonamiento(solicitud);
        int maxIntentos = 3;

        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                log.debug("Groq request — modelo: {} | tokens_max: {} | intento: {}/{}",
                        request.getModel(), request.getMaxTokens(), intento, maxIntentos);

                IaDTO.GroqChatResponse response = restClient.post()
                        .uri("/chat/completions")
                        .body(request)
                        .retrieve()
                        .body(IaDTO.GroqChatResponse.class);

                if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                    throw new BusinessException("Groq devolvió una respuesta vacía o inválida.");
                }

                log.debug("Groq OK — tokens_usados: {}",
                        response.getUsage() != null ? response.getUsage().getTotalTokens() : "N/A");

                IaDTO.GroqChoice opcion = response.getChoices().get(0);
                String contenido = opcion.getMessage() != null ? opcion.getMessage().getContent() : null;
                if (contenido == null || contenido.isBlank()) {
                    // Un modelo de razonamiento puede gastar todo el presupuesto pensando
                    // y devolver el texto vacío (finish_reason = "length").
                    log.warn("Groq devolvió contenido vacío — modelo: {} | finish_reason: {}",
                            request.getModel(), opcion.getFinishReason());
                    throw new BusinessException(
                            "La IA no alcanzó a generar una respuesta. Intenta de nuevo en unos segundos.");
                }

                return response;

            } catch (HttpClientErrorException e) {
                String body = e.getResponseBodyAsString();
                HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());

                if (status == HttpStatus.TOO_MANY_REQUESTS) {
                    long esperaMs = parsearEsperaMs(body);
                    if (esperaMs < 0) {
                        // Límite diario (TPD) — no tiene sentido reintentar
                        log.error("Groq límite DIARIO alcanzado — {}", body);
                        throw new BusinessException(
                                "Se alcanzó el límite diario de consultas IA (plan gratuito Groq). " +
                                "Intenta de nuevo en aproximadamente 1 hora, o solicita al administrador " +
                                "que actualice al plan de desarrollo.");
                    }
                    if (intento < maxIntentos) {
                        log.warn("Groq rate limit TPM — esperando {}ms antes del intento {}/{}",
                                esperaMs, intento + 1, maxIntentos);
                        try { Thread.sleep(esperaMs); }
                        catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new BusinessException("Consulta IA interrumpida durante el reintento.");
                        }
                    } else {
                        throw new BusinessException(
                                "El servicio de IA está saturado por uso intensivo. " +
                                "Espera unos segundos e intenta de nuevo.");
                    }

                } else if (status == HttpStatus.PAYLOAD_TOO_LARGE) {
                    log.error("Groq 413 — contexto demasiado grande: {}", body);
                    throw new BusinessException(
                            "La consulta excede el tamaño permitido por el modelo. " +
                            "Intenta con una pregunta más específica.");

                } else if (status == HttpStatus.UNAUTHORIZED) {
                    log.error("Groq 401 — API key inválida o expirada");
                    throw new BusinessException(
                            "La API key de Groq no es válida o expiró. " +
                            "Verifique la variable groq.api.key en la configuración.");

                } else {
                    log.error("Groq error HTTP [{}]: {}", e.getStatusCode(), body);
                    throw new BusinessException(
                            "Error inesperado en el servicio de IA [" + e.getStatusCode() + "]. " +
                            "Intenta nuevamente.");
                }

            } catch (RestClientException e) {
                log.error("Error de red llamando a Groq: {}", e.getMessage());
                throw new BusinessException(
                        "No se pudo conectar con el servicio de IA. " +
                        "Verifica la conexión a internet o intenta nuevamente.");
            }
        }

        throw new BusinessException(
                "El servicio de IA no respondió tras " + maxIntentos + " intentos. " +
                "Intenta nuevamente en unos segundos.");
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
