package com.costusoft.inventory_system.module.insumo.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTOs del modulo Insumo.
 *
 * RequestDTO — entrada: crear / actualizar un insumo.
 * ResponseDTO — salida: datos que el frontend recibe.
 *
 * Separar request/response evita exponer campos internos
 * (createdAt, updatedAt, riesgo calculado) en la creacion.
 */
public class InsumoDTO {

    // ── Request (crear / actualizar) ────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "El nombre del insumo es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        private String nombre;

        @NotNull(message = "El stock inicial es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        private Integer stock;

        @NotBlank(message = "La unidad de medida es obligatoria")
        @Size(max = 30, message = "La unidad de medida no puede superar 30 caracteres")
        private String unidadMedida;

        @Size(max = 50, message = "El tipo no puede superar 50 caracteres")
        private String tipo;

        @Min(value = 0, message = "El stock minimo no puede ser negativo")
        private Integer stockMinimo;
    }

    // ── Response (lo que retorna la API) ────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private final Long id;
        private final String nombre;
        private final Integer stock;
        private final String unidadMedida;
        private final String tipo;
        private final Integer stockMinimo;
        private final String riesgo;
        private final boolean stockBajo;
        private final String createdAt;
        private final String updatedAt;
    }

    // ── Stock Update (ajuste puntual de stock) ──────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StockUpdateRequest {

        @NotNull(message = "El nuevo stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        private Integer nuevoStock;

        @Size(max = 255, message = "La observacion no puede superar 255 caracteres")
        private String observacion;
    }
}
