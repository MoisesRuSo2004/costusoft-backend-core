package com.costusoft.inventory_system.module.reporte.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTOs del módulo Reporte.
 *
 * Tipos de informe disponibles:
 *
 * GENERAL — todos los insumos con entradas y salidas en el periodo
 * ENTRADAS — solo insumos con movimientos de entrada
 * SALIDAS — solo insumos con movimientos de salida
 * STOCK_BAJO — insumos bajo el mínimo (incluye stock cero)
 * ROTACION — índice de rotación por insumo (rápidos vs "muertos")
 * CONSUMO_PROMEDIO — tasa de consumo diario/semanal/mensual con tendencia
 * PEDIDOS — estado de pedidos con semáforo 🟢🟡🔴
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReporteDTO {

    // ══════════════════════════════════════════════════════════════════════
    // REQUEST
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FiltroRequest {

        @NotBlank(message = "La fecha de inicio es obligatoria")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Formato de fecha inválido. Use yyyy-MM-dd")
        private String fechaInicio;

        @NotBlank(message = "La fecha de fin es obligatoria")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Formato de fecha inválido. Use yyyy-MM-dd")
        private String fechaFin;

        /**
         * Tipo de informe. Valores válidos:
         * GENERAL | ENTRADAS | SALIDAS | STOCK_BAJO | ROTACION | CONSUMO_PROMEDIO |
         * PEDIDOS
         */
        @NotBlank(message = "El tipo de informe es obligatorio")
        @Pattern(regexp = "(?i)^(GENERAL|ENTRADAS|SALIDAS|STOCK_BAJO|ROTACION|CONSUMO_PROMEDIO|PEDIDOS)$", message = "Tipo de informe inválido. Valores permitidos: GENERAL, ENTRADAS, SALIDAS, STOCK_BAJO, ROTACION, CONSUMO_PROMEDIO, PEDIDOS")
        private String tipoInforme;

        /** Filtro opcional por proveedor (ENTRADAS, GENERAL). */
        private Long proveedorId;

        /** Filtro opcional por colegio (SALIDAS, GENERAL, PEDIDOS). */
        private Long colegioId;

        /**
         * Filtro opcional de estado para PEDIDOS.
         * Valores: BORRADOR | CALCULADO | CONFIRMADO | EN_PRODUCCION |
         * LISTO_PARA_ENTREGA | ENTREGADO | CANCELADO
         * Si es null, incluye todos los estados.
         */
        private String estadoPedido;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ITEMS — Tipos de fila por informe
    // ══════════════════════════════════════════════════════════════════════

    // ── GENERAL / ENTRADAS / SALIDAS / STOCK_BAJO ────────────────────────

    @Getter
    @Builder
    public static class ItemResponse {

        private final Long insumoId;
        private final String nombreInsumo;
        private final String unidadMedida;
        private final String tipo;
        private final int entradas;
        private final int salidas;
        private final int stockActual;
        private final int stockMinimo;
        private final boolean stockBajo;
        /** true si stockActual == 0 — alerta crítica */
        private final boolean stockCero;
    }

    // ── ROTACION ─────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class RotacionItem {

        private final Long insumoId;
        private final String nombreInsumo;
        private final String unidadMedida;
        private final String tipo;
        private final int stockActual;
        private final int totalSalidas;

        /**
         * Unidades consumidas por mes en el periodo analizado.
         * Fórmula: totalSalidas / (diasPeriodo / 30)
         */
        private final BigDecimal indiceRotacion;

        /**
         * Días que dura el stock actual al ritmo de consumo observado.
         * null si no hubo consumo (insumo sin movimiento).
         */
        private final Integer diasCobertura;

        /**
         * Categoría de rotación:
         * "Alta rotación" (≥10 u/mes) | "Media rotación" (3–10) |
         * "Baja rotación" (<3) | "Sin movimiento"
         */
        private final String categoriaRotacion;

        /** true si no tuvo ninguna salida en el periodo Y tiene stock. */
        private final boolean stockMuerto;
    }

    // ── CONSUMO_PROMEDIO ─────────────────────────────────────────────────

    @Getter
    @Builder
    public static class ConsumoItem {

        private final Long insumoId;
        private final String nombreInsumo;
        private final String unidadMedida;
        private final String tipo;
        private final int stockActual;
        private final int totalConsumo;

        /** Promedio de unidades consumidas por día en el periodo. */
        private final BigDecimal consumoDiario;

        /** consumoDiario × 7 */
        private final BigDecimal consumoSemanal;

        /** consumoDiario × 30 */
        private final BigDecimal consumoMensual;

        /**
         * Comparación primera mitad vs segunda mitad del periodo:
         * "Creciente" | "Decreciente" | "Estable" | "Sin datos"
         */
        private final String tendencia;

        /**
         * Días de stock restante al ritmo actual de consumo.
         * null si consumoDiario == 0.
         */
        private final Integer diasCoberturaEstimados;
    }

    // ── PEDIDOS ──────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class PedidoItem {

        private final Long pedidoId;
        private final String numeroPedido;
        private final String colegio;
        private final String estado;
        private final String estadoDescripcion;

        /** Formato: yyyy-MM-dd HH:mm:ss */
        private final String fechaPedido;

        /** Formato: yyyy-MM-dd — null si no fue especificada. */
        private final String fechaEstimadaEntrega;

        /**
         * Días hasta la entrega (negativo = atrasado).
         * null si no tiene fechaEstimadaEntrega o el pedido es final.
         */
        private final Integer diasRestantes;

        /**
         * Semáforo de estado del pedido:
         * "VERDE" → más de 7 días hasta la entrega
         * "AMARILLO" → 0–7 días hasta la entrega
         * "ROJO" → atrasado (fechaEstimadaEntrega ya pasó)
         * "ENTREGADO" → pedido completado
         * "CANCELADO" → pedido cancelado
         * "SIN_FECHA" → no tiene fechaEstimadaEntrega asignada
         */
        private final String semaforo;

        /**
         * Descripción del semáforo para UI: "A tiempo", "Próximo", "Retrasado", etc.
         */
        private final String semaforoDescripcion;

        private final Integer totalPrendas;

        /** Factor de cumplimiento del último cálculo (0–100). null si no calculado. */
        private final Integer porcentajeCumplimiento;

        private final String creadoPor;
    }

    // ══════════════════════════════════════════════════════════════════════
    // RESUMEN
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Builder
    public static class ResumenResponse {

        private final String tipoInforme;
        private final String fechaInicio;
        private final String fechaFin;
        private final String generadoEn;

        // ── Campos para GENERAL / ENTRADAS / SALIDAS / STOCK_BAJO ────────
        private final Integer totalInsumos;
        private final Integer totalEntradas;
        private final Integer totalSalidas;
        private final Integer insumosConStockBajo;
        private final Integer insumosConStockCero;

        // ── Campos para ROTACION ─────────────────────────────────────────
        /** Insumos con indiceRotacion >= 10 u/mes. */
        private final Integer insumosAltaRotacion;
        /** Insumos con stock > 0 y cero salidas en el periodo. */
        private final Integer insumosStockMuerto;

        // ── Campos para CONSUMO_PROMEDIO ─────────────────────────────────
        /** Insumos con consumo creciente en la segunda mitad del periodo. */
        private final Integer insumosTendenciaCreciente;
        /** Insumos con consumo decreciente. */
        private final Integer insumosTendenciaDecreciente;

        // ── Campos para PEDIDOS ──────────────────────────────────────────
        private final Integer totalPedidos;
        private final Integer pedidosVerdes;
        private final Integer pedidosAmarillos;
        private final Integer pedidosRojos;
        private final Integer pedidosEntregados;
        private final Integer pedidosCancelados;
    }

    // ══════════════════════════════════════════════════════════════════════
    // RESPONSE COMPLETO
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Builder
    public static class Response {

        /**
         * Poblado para: GENERAL, ENTRADAS, SALIDAS, STOCK_BAJO.
         * null para otros tipos.
         */
        private final List<ItemResponse> items;

        /**
         * Poblado para: ROTACION.
         * null para otros tipos.
         */
        private final List<RotacionItem> rotacion;

        /**
         * Poblado para: CONSUMO_PROMEDIO.
         * null para otros tipos.
         */
        private final List<ConsumoItem> consumo;

        /**
         * Poblado para: PEDIDOS.
         * null para otros tipos.
         */
        private final List<PedidoItem> pedidos;

        private final ResumenResponse resumen;
    }
}
