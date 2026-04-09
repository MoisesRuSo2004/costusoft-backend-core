package com.costusoft.inventory_system.module.uniforme.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTOs del módulo Uniforme.
 *
 * Un Uniforme representa un TIPO DE PRENDA asociado a un colegio
 * (ej. "Sueter Diario Hombre — Colegio Consolata").
 *
 * Los insumos requeridos se definen POR TALLA: la misma prenda puede
 * necesitar cantidades distintas de tela/botones según si es talla S o XL.
 *
 * Flujo de creación:
 * POST /api/uniformes → crea la prenda e inserta insumos por talla
 * GET /api/uniformes/{id}/tallas → lista las tallas configuradas
 * GET /api/uniformes/colegio/{id} → prendas del colegio con sus insumos
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

        /**
         * Tipo de uniforme. Ej: "Diario", "Educacion Fisica".
         */
        @Size(max = 50, message = "El tipo no puede superar 50 caracteres")
        private String tipo;

        /**
         * Género al que aplica. Ej: "Hombre", "Mujer", "Unisex".
         */
        @Size(max = 20, message = "El género no puede superar 20 caracteres")
        private String genero;

        @NotNull(message = "El ID del colegio es obligatorio")
        private Long colegioId;

        /**
         * Insumos requeridos agrupados por talla.
         * Cada elemento especifica: insumoId + cantidadBase + unidadMedida + talla.
         * Múltiples entradas con la misma talla son válidas (distintos insumos).
         * Múltiples entradas con el mismo insumo pero distinta talla también son
         * válidas.
         */
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
        private BigDecimal cantidadBase;

        @NotBlank(message = "La unidad de medida es obligatoria")
        @Size(max = 30)
        private String unidadMedida;

        /**
         * Talla a la que aplica este insumo.
         * Ejemplos: "S", "M", "L", "XL", "06-08", "10-12", "14-16", "UNICA".
         */
        @NotBlank(message = "La talla del insumo es obligatoria")
        @Size(max = 10, message = "La talla no puede superar 10 caracteres")
        private String talla;
    }

    // ── Response ─────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private final Long id;
        private final String prenda;
        private final String tipo;
        private final String genero;
        private final Long colegioId;
        private final String colegioNombre;

        /**
         * Tallas disponibles para esta prenda (extraídas de insumosRequeridos).
         * Ordenadas: S < M < L < XL, o numéricas. Útil para poblar el dropdown de
         * talla.
         */
        private final List<String> tallas;

        /** Lista completa de insumos requeridos con su talla. */
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
        private final BigDecimal cantidadBase;
        private final String unidadMedida;

        /**
         * Talla a la que aplica este par (insumo, cantidad).
         */
        private final String talla;
    }
}
