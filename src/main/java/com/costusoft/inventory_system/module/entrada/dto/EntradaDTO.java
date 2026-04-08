package com.costusoft.inventory_system.module.entrada.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTOs del módulo Entrada.
 *
 * Flujo BODEGA:
 *   - Request     → USER/ADMIN crea solicitud PENDIENTE (sin tocar stock)
 *   - RechazarRequest → BODEGA/ADMIN rechaza con motivo
 *   - Response    → incluye estado, confirmadaPor, motivoRechazo para que el
 *                   frontend actualice la UI sin una segunda consulta
 */
public class EntradaDTO {

    // ── Request ──────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotNull(message = "La fecha es obligatoria")
        private LocalDate fecha;

        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        private String descripcion;

        // ID del proveedor — opcional
        private Long proveedorId;

        @NotNull(message = "Los detalles son obligatorios")
        @NotEmpty(message = "La entrada debe tener al menos un detalle")
        @Valid
        private List<DetalleRequest> detalles;
    }

    // ── Detalle Request ──────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetalleRequest {

        @NotNull(message = "El ID del insumo es obligatorio")
        private Long insumoId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        private Integer cantidad;
    }

    // ── Rechazar Request ─────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RechazarRequest {

        @NotBlank(message = "El motivo de rechazo es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        private String motivo;
    }

    // ── Response ─────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private final Long id;
        private final String fecha;
        private final String descripcion;
        private final String proveedorNombre;
        private final List<DetalleResponse> detalles;

        // ── Campos de flujo de aprobación ──────────────────────────────
        private final String estado;
        private final String confirmadaPor;
        private final String motivoRechazo;
        private final String confirmadaAt;

        private final String createdAt;
    }

    // ── Detalle Response ─────────────────────────────────────────────────

    @Getter
    @Builder
    public static class DetalleResponse {

        private final Long id;
        private final Long insumoId;
        private final String nombreInsumo;
        private final Integer cantidad;
        private final String unidadMedida;
    }
}
