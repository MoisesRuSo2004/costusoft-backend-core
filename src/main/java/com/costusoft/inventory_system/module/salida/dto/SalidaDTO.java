package com.costusoft.inventory_system.module.salida.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTOs del modulo Salida.
 *
 * Una salida descuenta stock. Si cualquier insumo no tiene
 * stock suficiente, se lanza StockInsuficienteException
 * y NINGUN descuento persiste (transaccion atomica).
 */
public class SalidaDTO {

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

        // Colegio destino — opcional, para trazabilidad
        private Long colegioId;

        @NotNull(message = "Los detalles son obligatorios")
        @NotEmpty(message = "La salida debe tener al menos un detalle")
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

    // ── Response ─────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private final Long id;
        private final String fecha;
        private final String descripcion;
        private final String colegioNombre;
        private final List<DetalleResponse> detalles;
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
