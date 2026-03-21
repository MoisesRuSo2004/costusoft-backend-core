package com.costusoft.inventory_system.module.calculadora.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTOs del modulo Calculadora.
 *
 * Verifica si hay stock suficiente para fabricar N unidades de un uniforme.
 */
public class CalculadoraDTO {

    // ── Request ──────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {

        @NotNull(message = "El ID del uniforme es obligatorio")
        private Long uniformeId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        private Integer cantidad;
    }

    // ── Response ─────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private final Long uniformeId;
        private final String nombrePrenda;
        private final int cantidadSolicitada;
        private final boolean disponible;
        private final List<DetalleInsumo> detalles;
    }

    // ── Detalle por insumo ────────────────────────────────────────────────

    @Getter
    @Builder
    public static class DetalleInsumo {

        private final String nombreInsumo;
        private final String unidadMedida;
        private final BigDecimal cantidadNecesaria; // cantidadBase * cantidad pedida
        private final BigDecimal stockActual; // stock como BigDecimal para comparar
        private final BigDecimal stockRestante; // stockActual - cantidadNecesaria (min 0)
        private final boolean suficiente;
        private final String estado; // "Disponible" | "Insuficiente" | "Sin stock"
    }
}