package com.costusoft.inventory_system.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Wrapper estandar para todas las respuestas de la API.
 *
 * Estructura garantizada para cualquier endpoint:
 * {
 * "success": true | false,
 * "message": "...",
 * "data": { ... } | null,
 * "timestamp": "2024-01-01T12:00:00"
 * }
 *
 * Uso:
 * return ResponseEntity.ok(ApiResponse.ok("Insumo creado", insumoDTO));
 * return ResponseEntity.badRequest().body(ApiResponse.error("Nombre
 * duplicado"));
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // ── Factory methods ─────────────────────────────────────────────────

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}