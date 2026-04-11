package com.costusoft.inventory_system.module.ia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * DTOs del módulo IA.
 *
 * Separados en dos grupos:
 * - DTOs de negocio → lo que el frontend envía y recibe
 * - DTOs de Groq API → lo que se envía/recibe de la API de Groq
 *
 * El frontend NUNCA ve los DTOs de Groq; solo ve ConsultaRequest /
 * ConsultaResponse.
 */
public class IaDTO {

    // ════════════════════════════════════════════════════════════════════════
    // TIPOS DE CONSULTA
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Tipos de análisis disponibles.
     * Cada tipo determina qué datos se obtienen de la BD y qué prompt se construye.
     */
    public enum TipoConsulta {
        STOCK_BAJO,
        RESUMEN_INVENTARIO,
        PEDIDOS_ACTIVOS,
        ENTRADAS_PENDIENTES,
        SALIDAS_PENDIENTES,
        ANALISIS_GENERAL,
        /**
         * Combina predicciones ML (Prophet + XGBoost) con análisis en lenguaje natural
         */
        PREDICCION_RIESGO,
        /** Rendimiento y comportamiento histórico de proveedores */
        ANALISIS_PROVEEDORES,
        /**
         * Cruza consumo real (últimos 30 días) vs período anterior — detecta picos
         * anómalos
         */
        ANOMALIAS_CONSUMO
    }

    // ════════════════════════════════════════════════════════════════════════
    // DTOs DE NEGOCIO (frontend ↔ backend)
    // ════════════════════════════════════════════════════════════════════════

    /** Request del frontend: qué tipo de análisis quiere ejecutar. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConsultaRequest {

        @NotNull(message = "El tipo de consulta es obligatorio")
        private TipoConsulta tipo;
    }

    /** Response al frontend: respuesta en lenguaje natural + metadatos. */
    @Getter
    @Builder
    public static class ConsultaResponse {

        /** Respuesta en español generada por el modelo. */
        private final String respuesta;

        /** Tipo de consulta ejecutada. */
        private final String tipo;

        /** Nombre del modelo Groq usado. */
        private final String modelo;

        /** Total de tokens consumidos (prompt + completion). */
        private final Integer tokensUsados;

        /** Tiempo total de respuesta en milisegundos. */
        private final Long tiempoMs;
    }

    // ════════════════════════════════════════════════════════════════════════
    // DTOs CHAT LIBRE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Request del chat libre: el usuario escribe en lenguaje natural.
     * El backend inyecta el contexto completo del inventario antes de llamar a
     * Groq.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ChatRequest {

        @NotBlank(message = "La pregunta no puede estar vacía")
        @Size(max = 500, message = "La pregunta no puede superar 500 caracteres")
        private String pregunta;
    }

    /** Response del chat: respuesta conversacional + metadatos. */
    @Getter
    @Builder
    public static class ChatResponse {

        private final String respuesta;
        private final String modelo;
        private final Integer tokensUsados;
        private final Long tiempoMs;
    }

    // ════════════════════════════════════════════════════════════════════════
    // DTOs ORDEN DE COMPRA
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Request para generar una orden de compra.
     * Si se indica proveedorId, la carta se dirige a ese proveedor específico.
     * Si no, se genera un documento general de requisición.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrdenCompraRequest {

        /** ID del proveedor al que se dirige la orden. Opcional. */
        private Long proveedorId;

        /** Nombre de la empresa emisora para el encabezado de la carta. Opcional. */
        @Size(max = 150, message = "El nombre de la empresa no puede superar 150 caracteres")
        private String nombreEmpresa;

        /** Observaciones adicionales a incluir en la carta. Opcional. */
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        private String observaciones;
    }

    /** Response con el texto de la orden generado + metadatos. */
    @Getter
    @Builder
    public static class OrdenCompraResponse {

        /** Texto completo de la orden de compra en español. */
        private final String textoOrden;

        /** Cantidad de insumos incluidos en la orden. */
        private final Integer insumosIncluidos;

        private final String modelo;
        private final Integer tokensUsados;
        private final Long tiempoMs;
    }

    // ════════════════════════════════════════════════════════════════════════
    // DTOs GROQ API — REQUEST
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Cuerpo del request que se envía a Groq.
     * Compatible con la API de OpenAI Chat Completions.
     */
    @Getter
    @Builder
    public static class GroqChatRequest {

        private final String model;

        private final List<GroqMessage> messages;

        @JsonProperty("max_tokens")
        private final Integer maxTokens;

        /** 0.0 = determinista / 1.0 = creativo. Para análisis de datos: 0.1–0.3 */
        private final Double temperature;
    }

    /** Mensaje individual en la conversación (system / user / assistant). */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroqMessage {
        private String role;
        private String content;
    }

    // ════════════════════════════════════════════════════════════════════════
    // DTOs GROQ API — RESPONSE
    // ════════════════════════════════════════════════════════════════════════

    /** Respuesta completa de Groq. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class GroqChatResponse {

        private String id;
        private String model;
        private List<GroqChoice> choices;
        private GroqUsage usage;
    }

    /** Una opción de respuesta (Groq puede devolver N; usamos choices[0]). */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class GroqChoice {

        private Integer index;
        private GroqMessage message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /** Consumo de tokens del request. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class GroqUsage {

        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
