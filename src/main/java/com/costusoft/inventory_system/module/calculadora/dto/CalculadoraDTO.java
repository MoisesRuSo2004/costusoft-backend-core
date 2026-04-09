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
 * 1. Verificación simple — POST /verificar
 * ¿Puedo fabricar N unidades de una prenda X en talla Y?
 * Request: uniformeId + cantidad + talla
 *
 * 2. Cálculo de pedido — POST /pedido
 * ¿Puedo completar un pedido con MÚLTIPLES prendas y tallas?
 * Ejemplo: 50 suéteres talla M + 30 pantalones talla 06-08.
 * Los insumos compartidos se consolidan para mostrar el consumo real.
 *
 * IMPORTANTE: La talla es SIEMPRE obligatoria porque los insumos varían
 * por talla (UniformeInsumo tiene talla). Sin talla, no se puede calcular.
 */
public class CalculadoraDTO {

    // ══════════════════════════════════════════════════════════════════════
    // MODO 1 — Verificación simple (una prenda)
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

        /**
         * Talla a fabricar. OBLIGATORIA para que la calculadora use los insumos
         * correctos de esa talla en UniformeInsumo.
         * Ejemplos: "S", "M", "L", "XL", "06-08", "10-12", "14-16".
         * Obtén las tallas disponibles en: GET /api/uniformes/{id}/tallas
         */
        @NotBlank(message = "La talla es obligatoria para calcular los insumos correctos")
        @Size(max = 10, message = "La talla no puede superar 10 caracteres")
        private String talla;
    }

    @Getter
    @Builder
    public static class Response {

        private final Long uniformeId;
        private final String nombrePrenda;
        private final String talla;
        private final String tipo;

        /**
         * Género de la prenda. Puede ser null para prendas de Educación Física
         * que son unisex (no tienen género definido).
         */
        private final String genero;
        private final int cantidadSolicitada;

        /**
         * Máximo de unidades fabricables con el stock actual.
         * Calculado como min(stock_i / cantidadBase_i) para todos los insumos de la
         * talla.
         */
        private final int cantidadMaximaFabricable;

        /**
         * true solo si TODOS los insumos tienen stock suficiente para la cantidad
         * solicitada.
         */
        private final boolean disponible;

        private final List<DetalleInsumo> detalles;
    }

    // ══════════════════════════════════════════════════════════════════════
    // MODO 2 — Cálculo de pedido (múltiples prendas)
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PedidoRequest {

        /**
         * Atajo: carga todas las prendas del colegio con la misma cantidad y talla.
         * Se ignora si se proporciona la lista {@code prendas}.
         * Requiere también {@code cantidad} y {@code talla}.
         */
        private Long colegioId;

        /**
         * Cantidad de uniformes a fabricar por prenda.
         * Requerido cuando se usa {@code colegioId}.
         */
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 10000, message = "La cantidad no puede superar 10.000 unidades")
        private Integer cantidad;

        /**
         * Talla global cuando se usa el modo {@code colegioId + cantidad}.
         * Se ignora si se proporciona la lista {@code prendas}
         * (cada prenda lleva su propia talla).
         */
        @Size(max = 10, message = "La talla no puede superar 10 caracteres")
        private String talla;

        /**
         * Lista explícita de prendas con cantidad y talla individual.
         * Si se proporciona, tiene precedencia sobre colegioId + cantidad.
         * CADA PRENDA DEBE INCLUIR SU TALLA.
         */
        @Valid
        private List<PrendaRequest> prendas;
    }

    /**
     * Una prenda dentro del request de cálculo de pedido.
     *
     * La talla es OBLIGATORIA porque los insumos requeridos están definidos
     * por talla en UniformeInsumo: un Suéter M tiene insumos distintos a un Suéter
     * XL.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrendaRequest {

        @NotNull(message = "El ID del uniforme es obligatorio")
        private Long uniformeId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 10000, message = "La cantidad no puede superar 10.000 unidades")
        private Integer cantidad;

        /**
         * Talla solicitada para esta prenda.
         * Filtra los insumos en UniformeInsumo por este valor.
         * Ejemplos: "S", "M", "L", "XL", "06-08", "10-12", "14-16"
         */
        @NotBlank(message = "La talla es obligatoria para calcular los insumos correctos")
        @Size(max = 10, message = "La talla no puede superar 10 caracteres")
        private String talla;
    }

    @Getter
    @Builder
    public static class PedidoResponse {

        /** true solo si el pedido COMPLETO puede fabricarse con el stock actual. */
        private final boolean disponibleCompleto;

        /**
         * Factor de cumplimiento entre 0.0000 y 1.0000.
         * 1.0 = pedido completamente atendible.
         * 0.7 = solo puedes fabricar el 70% de cada prenda.
         */
        private final BigDecimal factorCumplimiento;

        /**
         * Porcentaje 0–100. Listo para mostrar en UI.
         */
        private final int porcentajeCumplimiento;

        /**
         * Nombre del insumo cuello de botella que limita la producción.
         * null si disponibleCompleto = true.
         */
        private final String insumoLimitante;

        /** Resultado detallado por cada (prenda, talla) del pedido. */
        private final List<ResultadoPrenda> prendas;

        /**
         * Insumos consolidados de todo el pedido.
         * Si "Tela lacoste blanco" la usan Suéter-M (1m) y Suéter-L (1m),
         * aparece UNA vez con totalNecesario = 2m.
         * Aquí se ve el impacto real del pedido sobre el inventario.
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

        /**
         * Género. Puede ser null para prendas de Educación Física (unisex).
         */
        private final String genero;
        private final int cantidadSolicitada;

        /**
         * Máximo fabricable para ESTA prenda con el factor global del pedido.
         * = floor(cantidadSolicitada × factorCumplimiento)
         */
        private final int cantidadMaxima;

        /**
         * ¿Esta prenda sola tiene stock suficiente?
         * Puede ser true mientras disponibleCompleto es false si otros insumos
         * compartidos con otras prendas agotan el stock global.
         */
        private final boolean disponibleIndividual;

        /** Detalle insumo a insumo de esta prenda en esta talla. */
        private final List<DetalleInsumo> insumos;
    }

    @Getter
    @Builder
    public static class ResumenInsumo {

        private final Long insumoId;
        private final String nombreInsumo;
        private final String unidadMedida;

        /** Stock actual en bodega al momento del cálculo. */
        private final BigDecimal stockActual;

        /**
         * Total necesario sumando todos los requerimientos del pedido completo.
         * Ejemplo: Suéter-M (1m) + Pantalón-06-08 (1.2m) = 2.2m de Tela azul.
         */
        private final BigDecimal totalNecesario;

        /** max(totalNecesario - stockActual, 0). Cuánto falta. */
        private final BigDecimal faltante;

        /** true si stockActual >= totalNecesario. */
        private final boolean suficiente;

        /** "Disponible" | "Insuficiente" | "Sin stock" */
        private final String estado;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Detalle por insumo (compartido entre modo 1 y modo 2)
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Builder
    public static class DetalleInsumo {

        private final Long insumoId;
        private final String nombreInsumo;
        private final String unidadMedida;

        /** cantidadBase × cantidad solicitada para esta prenda/talla. */
        private final BigDecimal cantidadNecesaria;

        /** Stock actual en bodega (evaluación individual, sin otras prendas). */
        private final BigDecimal stockActual;

        /**
         * max(stockActual - cantidadNecesaria, 0).
         * Sobrante si SOLO se fabricara esta prenda.
         */
        private final BigDecimal stockRestante;

        /** true si stockActual >= cantidadNecesaria. */
        private final boolean suficiente;

        /** "Disponible" | "Insuficiente" | "Sin stock" */
        private final String estado;
    }
}