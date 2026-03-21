package com.costusoft.inventory_system.module.reporte.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * DTOs del modulo Reporte.
 *
 * FiltroRequest — parametros de filtrado para generar el reporte.
 * ItemResponse — una fila del reporte (un insumo con sus movimientos).
 * ResumenResponse — totales del reporte para el pie de pagina.
 */
public class ReporteDTO {

    // ── Filtro Request ────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FiltroRequest {

        @NotBlank(message = "La fecha de inicio es obligatoria")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Formato de fecha invalido. Use yyyy-MM-dd")
        private String fechaInicio;

        @NotBlank(message = "La fecha de fin es obligatoria")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Formato de fecha invalido. Use yyyy-MM-dd")
        private String fechaFin;

        /**
         * Tipo de informe:
         * GENERAL — todos los insumos con entradas y salidas
         * ENTRADAS — solo insumos que tuvieron entradas en el periodo
         * SALIDAS — solo insumos que tuvieron salidas en el periodo
         * STOCK_BAJO — solo insumos con stock bajo al momento del reporte
         */
        @NotBlank(message = "El tipo de informe es obligatorio")
        private String tipoInforme;

        // Filtro opcional por proveedor (solo aplica a ENTRADAS y GENERAL)
        private Long proveedorId;

        // Filtro opcional por colegio (solo aplica a SALIDAS y GENERAL)
        private Long colegioId;
    }

    // ── Item del reporte (una fila) ───────────────────────────────────────

    @Getter
    @Builder
    public static class ItemResponse {

        private final Long insumoId;
        private final String nombreInsumo;
        private final String unidadMedida;
        private final int entradas;
        private final int salidas;
        private final int stockActual;
        private final int stockMinimo;
        private final boolean stockBajo;
    }

    // ── Resumen total del reporte ─────────────────────────────────────────

    @Getter
    @Builder
    public static class ResumenResponse {

        private final int totalInsumos;
        private final int totalEntradas;
        private final int totalSalidas;
        private final int insumosConStockBajo;
        private final int insumosConStockCero;
        private final String fechaInicio;
        private final String fechaFin;
        private final String tipoInforme;
    }

    // ── Response completo ─────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private final List<ItemResponse> items;
        private final ResumenResponse resumen;
    }
}