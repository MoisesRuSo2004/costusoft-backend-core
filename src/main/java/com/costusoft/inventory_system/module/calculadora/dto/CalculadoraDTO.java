package com.costusoft.inventory_system.module.calculadora.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTOs del módulo Calculadora de Disponibilidad.
 *
 * Dos modos de uso:
 *
 * 1. Verificación simple   — POST /verificar
 *    ¿Puedo fabricar N unidades de una prenda X?
 *    Request: uniformeId + cantidad
 *
 * 2. Cálculo de pedido     — POST /pedido
 *    ¿Puedo completar un pedido que incluye MÚLTIPLES prendas?
 *    Ejemplo: 50 camisas + 50 pantalones del Colegio San Juan.
 *    Los insumos compartidos (ej. tela) se agregan para ver el consumo real.
 *    Request: colegioId + cantidad  →  todas las prendas del colegio × cantidad
 *          o: lista explícita de {uniformeId, cantidad}
 */
public class CalculadoraDTO {

    // ══════════════════════════════════════════════════════════════════════
    //  MODO 1 — Verificación simple (una prenda)
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {

        @NotNull(message = "El ID del uniforme es obligatorio")
        private Long uniformeId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 10000, message = "La cantidad no puede superar 10.000 unidades")
        private Integer cantidad;
    }

    @Getter
    @Builder
    public static class Response {

        private final Long uniformeId;
        private final String nombrePrenda;
        private final String talla;
        private final String tipo;
        private final String genero;
        private final int cantidadSolicitada;

        /**
         * Máximo de unidades que se pueden fabricar con el stock actual.
         * Calculado como el mínimo de (stock / cantidadBase) por insumo.
         */
        private final int cantidadMaximaFabricable;

        /** true solo si TODOS los insumos tienen stock suficiente. */
        private final boolean disponible;

        private final List<DetalleInsumo> detalles;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MODO 2 — Cálculo de pedido (múltiples prendas)
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PedidoRequest {

        /**
         * Atajo: carga todas las prendas del colegio con la misma cantidad.
         * Se ignora si se proporciona la lista {@code prendas}.
         */
        private Long colegioId;

        /**
         * Cantidad de uniformes a fabricar para cada prenda.
         * Requerido si se usa {@code colegioId}.
         */
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 10000, message = "La cantidad no puede superar 10.000 unidades")
        private Integer cantidad;

        /**
         * Lista explícita de prendas con su cantidad individual.
         * Si se proporciona, tiene precedencia sobre colegioId + cantidad.
         */
        @Valid
        private List<PrendaRequest> prendas;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrendaRequest {

        @NotNull(message = "El ID del uniforme es obligatorio")
        private Long uniformeId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 10000, message = "La cantidad no puede superar 10.000 unidades")
        private Integer cantidad;
    }

    @Getter
    @Builder
    public static class PedidoResponse {

        /** true solo si el pedido completo puede ser atendido con el stock actual. */
        private final boolean disponibleCompleto;

        /**
         * Factor de cumplimiento entre 0.0 y 1.0.
         * 1.0 = pedido completamente atendible.
         * 0.7 = solo puedes completar el 70% del pedido.
         */
        private final BigDecimal factorCumplimiento;

        /**
         * Porcentaje del pedido que puede completarse (0-100).
         * Útil para mostrar en UI sin cálculos adicionales.
         */
        private final int porcentajeCumplimiento;

        /**
         * Nombre del insumo que limita la producción (el cuello de botella).
         * null si el pedido es completamente atendible.
         */
        private final String insumoLimitante;

        /** Resultado detallado por cada prenda del pedido. */
        private final List<ResultadoPrenda> prendas;

        /**
         * Vista consolidada de todos los insumos involucrados.
         * Si "tela" se usa en camisa Y pantalón, aparece UNA sola vez
         * con el total necesario sumado. Aquí se ve el problema real.
         */
        private final List<ResumenInsumo> resumenInsumos;
    }

    @Getter
    @Builder
    public static class ResultadoPrenda {

        private final Long uniformeId;
        private final String prenda;
        private final String talla;
        private final String tipo;
        private final String genero;
        private final int cantidadSolicitada;

        /**
         * Máximo fabricable para ESTA prenda, considerando el factor de cumplimiento
         * global del pedido (que incluye el impacto de insumos compartidos).
         */
        private final int cantidadMaxima;

        /**
         * ¿Esta prenda, por sí sola, tiene stock suficiente?
         * Puede ser true mientras disponibleCompleto es false
         * si otros insumos compartidos con otras prendas agotan el stock.
         */
        private final boolean disponibleIndividual;

        /** Detalle insumo por insumo requerido por esta prenda. */
        private final List<DetalleInsumo> insumos;
    }

    @Getter
    @Builder
    public static class ResumenInsumo {

        private final Long insumoId;
        private final String nombreInsumo;
        private final String unidadMedida;

        /** Stock actual en bodega. */
        private final BigDecimal stockActual;

        /**
         * Total necesario sumando los requerimientos de TODAS las prendas.
         * Si camisa necesita 2m y pantalón 1.5m → totalNecesario = 3.5m.
         */
        private final BigDecimal totalNecesario;

        /**
         * Cuánto falta para completar el pedido.
         * 0 si stockActual >= totalNecesario.
         */
        private final BigDecimal faltante;

        /** true si stockActual >= totalNecesario. */
        private final boolean suficiente;

        /** "Disponible" | "Insuficiente" | "Sin stock" */
        private final String estado;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Detalle por insumo (compartido entre modo 1 y modo 2)
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Builder
    public static class DetalleInsumo {

        private final Long insumoId;
        private final String nombreInsumo;
        private final String unidadMedida;

        /** cantidadBase × cantidad solicitada para ESTA prenda. */
        private final BigDecimal cantidadNecesaria;

        /** Stock actual en bodega (a nivel individual, sin considerar otras prendas). */
        private final BigDecimal stockActual;

        /**
         * max(stockActual - cantidadNecesaria, 0).
         * Sobrante si solo se fabricara esta prenda.
         */
        private final BigDecimal stockRestante;

        /** true si stockActual >= cantidadNecesaria (evaluación individual). */
        private final boolean suficiente;

        /** "Disponible" | "Insuficiente" | "Sin stock" */
        private final String estado;
    }
}
