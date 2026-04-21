package com.costusoft.inventory_system.module.institucion.dto;

import com.costusoft.inventory_system.entity.SolicitudEspecial.EstadoSolicitud;
import com.costusoft.inventory_system.entity.SolicitudEspecial.TipoSolicitud;
import com.costusoft.inventory_system.module.pedido.dto.PedidoDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTOs del portal institucional.
 *
 * Todos los requests de este modulo omiten el colegioId — se obtiene
 * automaticamente del token JWT del usuario INSTITUCION autenticado.
 * Esto garantiza que un coordinador nunca pueda crear/ver datos de otro colegio.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstitucionDTO {

    // ══════════════════════════════════════════════════════════════════════
    // PERFIL — Dashboard de bienvenida del coordinador
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Builder
    public static class PerfilResponse {

        private final Long colegioId;
        private final String nombreColegio;
        private final String direccionColegio;
        private final String username;
        private final String correo;

        /** Total de pedidos realizados por este colegio */
        private final int totalPedidos;
        /** Pedidos en curso (no finalizados ni cancelados) */
        private final int pedidosActivos;
        /** Cantidad de uniformes configurados para el colegio */
        private final int totalUniformes;
        /** Solicitudes especiales pendientes de respuesta */
        private final int solicitudesPendientes;
    }

    // ══════════════════════════════════════════════════════════════════════
    // PEDIDO — Crear un pedido desde el portal institucional
    // El colegioId se inyecta automaticamente desde el perfil del usuario
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PedidoRequest {

        /**
         * Fecha estimada de entrega del pedido.
         * Debe ser una fecha futura.
         */
        @Future(message = "La fecha estimada de entrega debe ser futura")
        private LocalDate fechaEstimadaEntrega;

        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        private String observaciones;

        /**
         * Prendas solicitadas.
         * Cada detalle incluye uniformeId, cantidad y talla.
         * Los uniformes deben pertenecer al colegio del coordinador.
         */
        @NotEmpty(message = "El pedido debe tener al menos una prenda")
        @Valid
        private List<PedidoDTO.DetalleRequest> detalles;
    }

    // ══════════════════════════════════════════════════════════════════════
    // SOLICITUD ESPECIAL — Peticion del coordinador al equipo Costusoft
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SolicitudRequest {

        @NotNull(message = "El tipo de solicitud es obligatorio")
        private TipoSolicitud tipo;

        @NotBlank(message = "El asunto es obligatorio")
        @Size(max = 200, message = "El asunto no puede superar 200 caracteres")
        private String asunto;

        @NotBlank(message = "La descripcion es obligatoria")
        @Size(max = 1000, message = "La descripcion no puede superar 1000 caracteres")
        private String descripcion;
    }

    @Getter
    @Builder
    public static class SolicitudResponse {

        private final Long id;
        /** Nombre del tipo de solicitud */
        private final String tipo;
        /** Estado actual: PENDIENTE, EN_REVISION, RESUELTA, RECHAZADA */
        private final String estado;
        private final String asunto;
        private final String descripcion;
        /** Respuesta del equipo Costusoft (null si no ha sido respondida) */
        private final String respuesta;
        /** Fecha en que se registro la respuesta (null si pendiente) */
        private final String fechaRespuesta;
        private final String createdAt;
    }

    // ══════════════════════════════════════════════════════════════════════
    // FILTRO DE SOLICITUDES (query param opcional)
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SolicitudFiltroRequest {

        /** Filtrar por estado. Si es null retorna todos los estados. */
        private EstadoSolicitud estado;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CATALOGO — Prendas disponibles para el colegio
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Builder
    public static class CatalogoItem {

        private final Long uniformeId;
        private final String nombre;
        private final String tipo;
        private final String genero;
        /** Tallas configuradas para esta prenda */
        private final List<String> tallas;
        /** Insumos requeridos para fabricarla */
        private final List<InsumoInfo> insumos;
    }

    @Getter
    @Builder
    public static class InsumoInfo {
        private final Long insumoId;
        private final String nombre;
        private final String unidadMedida;
    }

    // ══════════════════════════════════════════════════════════════════════
    // PEDIDO POR GRADO — Plantilla para crear pedidos a partir de un grado
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PedidoPorGradoRequest {

        @NotBlank(message = "El grado es obligatorio")
        @Size(max = 50)
        private String grado;

        @NotNull(message = "La cantidad de estudiantes es obligatoria")
        @Min(value = 1, message = "Debe haber al menos 1 estudiante")
        private Integer cantidadEstudiantes;

        /**
         * Tipo de uniforme a solicitar (ej. "Diario", "Educacion Fisica"). Opcional: si no viene se incluyen todas las prendas.
         */
        @Size(max = 50)
        private String tipoUniforme;

        /** Fecha estimada propuesta para la entrega. Opcional. */
        @Future(message = "La fecha estimada de entrega debe ser futura")
        private LocalDate fechaEstimadaEntrega;

        @Size(max = 500)
        private String observaciones;
    }

    @Getter
    @Builder
    public static class PedidoPorGradoItem {
        private final Long uniformeId;
        private final String nombre;
        private final String tipo;
        private final String genero;
        private final Integer cantidadSugerida;
        private final List<String> tallasDisponibles;
    }

    @Getter
    @Builder
    public static class PedidoPorGradoResponse {
        private final Long colegioId;
        private final String colegioNombre;
        private final String grado;
        private final Integer cantidadEstudiantes;
        private final List<PedidoPorGradoItem> items;
        private final String fechaEstimadaEntrega;
        private final String observaciones;
    }

    // ══════════════════════════════════════════════════════════════════════
    // SEGUIMIENTO — Resumen de estado y detalles consumidos por frontend
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Builder
    public static class SeguimientoResponse {
        private final Long pedidoId;
        private final String numeroPedido;
        private final String estado;
        private final String estadoDescripcion;
        private final String fechaEstimadaEntrega;
        private final Long salidaId;
        private final List<com.costusoft.inventory_system.module.pedido.dto.PedidoDTO.ResumenInsumo> resumenInsumos;
    }
}
