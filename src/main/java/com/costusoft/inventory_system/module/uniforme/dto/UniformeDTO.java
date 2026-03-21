package com.costusoft.inventory_system.module.uniforme.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTOs del modulo Uniforme.
 *
 * Un uniforme pertenece a un colegio y puede requerir
 * multiples insumos con cantidades especificas (UniformeInsumo).
 */
public class UniformeDTO {

    // ── Request ──────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "El nombre de la prenda es obligatorio")
        @Size(max = 100, message = "La prenda no puede superar 100 caracteres")
        private String prenda;

        @Size(max = 50, message = "El tipo no puede superar 50 caracteres")
        private String tipo;

        @Size(max = 10, message = "La talla no puede superar 10 caracteres")
        private String talla;

        @Size(max = 20, message = "El genero no puede superar 20 caracteres")
        private String genero;

        @NotNull(message = "El ID del colegio es obligatorio")
        private Long colegioId;

        // Lista de insumos requeridos — opcional en creacion
        @Valid
        private List<InsumoRequeridoRequest> insumosRequeridos;
    }

    // ── Insumo requerido en el Request ────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InsumoRequeridoRequest {

        @NotNull(message = "El ID del insumo es obligatorio")
        private Long insumoId;

        @NotNull(message = "La cantidad base es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad base debe ser mayor a cero")
        private BigDecimal cantidadBase; // BigDecimal — coincide con UniformeInsumo.cantidadBase

        @NotBlank(message = "La unidad de medida es obligatoria")
        @Size(max = 30)
        private String unidadMedida;
    }

    // ── Response ─────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private final Long id;
        private final String prenda;
        private final String tipo;
        private final String talla;
        private final String genero;
        private final Long colegioId;
        private final String colegioNombre;
        private final List<InsumoRequeridoResponse> insumosRequeridos;
        private final String createdAt;
        private final String updatedAt;
    }

    // ── Insumo requerido en el Response ──────────────────────────────────

    @Getter
    @Builder
    public static class InsumoRequeridoResponse {

        private final Long id;
        private final Long insumoId;
        private final String nombreInsumo;
        private final BigDecimal cantidadBase; // BigDecimal — consistente con entidad y request
        private final String unidadMedida;
    }
}