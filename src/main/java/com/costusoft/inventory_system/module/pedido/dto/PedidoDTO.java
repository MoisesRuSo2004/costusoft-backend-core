package com.costusoft.inventory_system.module.pedido.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTOs del módulo Pedido.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * FLUJO DE CREACIÓN DESDE FRONTEND
 * ──────────────────────────────────────────────────────────────────────────
 * 1. GET  /api/colegios             → listar colegios (selección o buscador)
 * 2. POST /api/colegios             → crear colegio si no existe
 *    ─ O ─ usar nuevoColegio en el body del pedido (creación inline)
 * 3. GET  /api/uniformes/colegio/{colegioId} → listar prendas del colegio
 * 4. Por cada prenda seleccionada:  pedir cantidad + talla → agregar a lista
 * 5. POST /api/pedidos              → crear pedido
 * 6. POST /api/pedidos/{id}/calcular → verificar stock (opcional antes de confirmar)
 * 7. POST /api/pedidos/{id}/confirmar → confirmar
 * ──────────────────────────────────────────────────────────────────────────
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PedidoDTO {

    // ══════════════════════════════════════════════════════════════════════
    //  REQUEST — Crear / Actualizar
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {

        /**
         * ID de un colegio existente.
         * Mutuamente excluyente con {@code nuevoColegio}.
         * Si ambos vienen, se usa {@code colegioId} y se ignora {@code nuevoColegio}.
         */
        private Long colegioId;

        /**
         * Datos para crear un nuevo colegio al vuelo.
         * Solo se usa si {@code colegioId} es null.
         * Permite el flujo "no existe el colegio → créalo en el mismo request".
         */
        @Valid
        private NuevoColegioRequest nuevoColegio;

        /**
         * Fecha estimada de entrega al colegio.
         * Debe ser una fecha futura.
         */
        @Future(message = "La fecha estimada de entrega debe ser futura")
        private LocalDate fechaEstimadaEntrega;

        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        private String observaciones;

        @NotEmpty(message = "El pedido debe tener al menos una prenda")
        @Valid
        private List<DetalleRequest> detalles;
    }

    /**
     * Datos mínimos para crear un colegio inline desde el flujo del pedido.
     * Si ya existe un colegio con el mismo nombre, el servicio lanza error.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class NuevoColegioRequest {

        @NotBlank(message = "El nombre del colegio es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        private String nombre;

        @Size(max = 250, message = "La dirección no puede superar 250 caracteres")
        private String direccion;
    }

    /**
     * Una prenda dentro del pedido.
     *
     * Flujo UI:
     *   - Seleccionar uniforme del listado (GET /api/uniformes/colegio/{id})
     *   - Ingresar cantidad (número de unidades requeridas)
     *   - Ingresar talla (ej. "4", "M", "XL", "Única")
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class DetalleRequest {

        @NotNull(message = "El uniforme es obligatorio")
        private Long uniformeId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1,     message = "La cantidad debe ser al menos 1")
        @Max(value = 10000, message = "La cantidad no puede superar 10.000 unidades")
        private Integer cantidad;

        /**
         * Talla solicitada para esta prenda.
         * Ejemplos: "4", "6", "8", "10", "12", "S", "M", "L", "XL", "Única".
         * Si no se especifica, se usa la talla configurada en el uniforme.
         */
        @Size(max = 10, message = "La talla no puede superar 10 caracteres")
        private String talla;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CancelarRequest {

        @NotBlank(message = "El motivo de cancelación es obligatorio")
        @Size(max = 500)
        private String motivo;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  RESPONSE — Pedido completo
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Builder
    public static class Response {

        private final Long id;
        private final String numeroPedido;
        private final String estado;

        /** Descripción legible del estado para mostrar en UI. */
        private final String estadoDescripcion;

        /**
         * Fecha de creación del pedido (tomada de AuditableEntity.createdAt).
         * Formato: "yyyy-MM-dd HH:mm:ss"
         */
        private final String fechaCreacion;

        /** Formato: "yyyy-MM-dd" */
        private final String fechaEstimadaEntrega;

        private final String observaciones;

        // ── Resultado de la calculadora (null hasta ejecutar calcular()) ──

        private final Boolean disponibleCompleto;

        /** Factor 0.0000–1.0000. null si no se ha calculado. */
        private final BigDecimal factorCumplimiento;

        /** 0–100. null si no se ha calculado. */
        private final Integer porcentajeCumplimiento;

        /** Insumo cuello de botella. null si disponibleCompleto = true. */
        private final String insumoLimitante;

        // ── Relaciones ───────────────────────────────────────────────────

        private final ColegioInfo colegio;
        private final String creadoPor;

        /** ID de la Salida generada al iniciar producción. null hasta entonces. */
        private final Long salidaId;

        private final List<DetalleResponse> detalles;

        /**
         * Insumos consolidados del pedido completo (insumos compartidos sumados).
         * Solo se incluye en obtenerPorId() cuando estado != BORRADOR.
         * null en listados para no sobrecargar la respuesta.
         */
        private final List<ResumenInsumo> resumenInsumos;

        private final String updatedAt;
    }

    // ── Detalle de una prenda dentro del pedido ──────────────────────────

    @Getter
    @Builder
    public static class DetalleResponse {

        private final Long id;
        private final Long uniformeId;
        private final String nombreUniforme;
        private final String tipo;

        /**
         * Talla solicitada. Puede diferir de uniforme.talla si el usuario
         * especificó una talla diferente al agregar al pedido.
         */
        private final String talla;

        private final String genero;
        private final Integer cantidad;

        /** null hasta que se ejecute calcular(). */
        private final Integer cantidadMaximaFabricable;

        /** null hasta que se ejecute calcular(). */
        private final Boolean disponibleIndividual;
    }

    // ── Resumen consolidado de insumos (post-cálculo) ────────────────────

    @Getter
    @Builder
    public static class ResumenInsumo {

        private final Long insumoId;
        private final String nombre;
        private final String unidadMedida;

        /** Stock actual en bodega al momento del cálculo. */
        private final BigDecimal stockActual;

        /**
         * Total necesario sumando TODAS las prendas del pedido.
         * Si Camisa usa 2m de Tela y Pantalón 1.5m → totalNecesario = 3.5m.
         */
        private final BigDecimal totalNecesario;

        /** max(totalNecesario - stockActual, 0). */
        private final BigDecimal faltante;

        private final Boolean suficiente;

        /** "Disponible" | "Insuficiente" | "Sin stock" */
        private final String estado;

        /** true si el stock está por debajo del mínimo configurado. */
        private final Boolean alertaStockMinimo;
    }

    // ── Info compacta del colegio embebida en el pedido ──────────────────

    @Getter
    @Builder
    public static class ColegioInfo {

        private final Long id;
        private final String nombre;
        private final String direccion;
    }

    // ── Historial de auditoría ───────────────────────────────────────────

    @Getter
    @Builder
    public static class HistorialResponse {

        private final Long id;
        private final String estadoAnterior;
        private final String estadoNuevo;
        private final String accion;
        private final String observacion;
        private final String realizadoPor;
        private final String fechaAccion;
    }
}
