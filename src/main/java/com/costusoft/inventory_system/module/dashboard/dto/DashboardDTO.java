package com.costusoft.inventory_system.module.dashboard.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta del dashboard.
 *
 * Agrupa todas las metricas en un solo endpoint para que
 * el frontend haga UNA sola llamada al cargar la pantalla principal.
 *
 * Secciones:
 * - Contadores generales
 * - Alertas de stock bajo
 * - Graficos de movimientos por mes
 * - Ultimos movimientos (actividad reciente)
 * - Distribucion de uniformes por colegio
 */
@Getter
@Builder
public class DashboardDTO {

    // ── Contadores generales ─────────────────────────────────────────────

    private final long totalInsumos;
    private final long totalEntradas;
    private final long totalSalidas;
    private final long totalColegios;
    private final long totalUniformes;
    private final long totalProveedores;
    private final long totalUsuarios;

    // ── Alertas ──────────────────────────────────────────────────────────

    private final int insumosConStockBajo;
    private final int insumosConStockCero;
    private final List<AlertaStockDTO> alertasStock;

    // ── Graficos de movimientos (ultimos 6 meses) ─────────────────────────

    private final Map<String, Long> entradasPorMes; // "2024-01" -> 5
    private final Map<String, Long> salidasPorMes; // "2024-01" -> 3

    // ── Actividad reciente ───────────────────────────────────────────────

    private final List<MovimientoDTO> ultimosMovimientos;

    // ── Distribucion de uniformes por colegio ─────────────────────────────

    private final Map<String, Long> uniformesPorColegio;

    // ── Nested DTOs ──────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class AlertaStockDTO {
        private final Long id;
        private final String nombre;
        private final int stockActual;
        private final int stockMinimo;
        private final String unidadMedida;
        private final String nivelRiesgo; // BAJO, MEDIO, ALTO, CRITICO
    }

    @Getter
    @Builder
    public static class MovimientoDTO {
        private final String tipo; // ENTRADA | SALIDA
        private final String descripcion;
        private final String fecha;
        private final int totalItems;
    }
}
