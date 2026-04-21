package com.costusoft.inventory_system.module.solicitudespecial.dto;

import com.costusoft.inventory_system.entity.SolicitudEspecial.EstadoSolicitud;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTOs para la gestión administrativa de Solicitudes Especiales.
 *
 * Este módulo permite a un ADMIN ver y responder todas las solicitudes
 * enviadas por los coordinadores de colegio (rol INSTITUCION).
 *
 * Endpoints:
 *   GET  /api/solicitudes-especiales            → listar (filtrable, paginado)
 *   GET  /api/solicitudes-especiales/{id}       → detalle
 *   PUT  /api/solicitudes-especiales/{id}/gestionar → cambiar estado + respuesta
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolicitudEspecialAdminDTO {

    // ══════════════════════════════════════════════════════════════════════
    // RESPONSE — Vista del admin con datos enriquecidos del colegio/usuario
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Builder
    public static class AdminResponse {

        private final Long id;

        /** Tipo de solicitud: AJUSTE_TALLA, PEDIDO_URGENTE, etc. */
        private final String tipo;

        /** Estado actual: PENDIENTE, EN_REVISION, RESUELTA, RECHAZADA */
        private final String estado;

        private final String asunto;
        private final String descripcion;

        /** Respuesta del equipo Costusoft (null si aún no fue gestionada) */
        private final String respuesta;

        /** Fecha de registro de la respuesta (null si pendiente) */
        private final String fechaRespuesta;

        // ── Datos del remitente ──────────────────────────────────────────

        /** Nombre del colegio que originó la solicitud */
        private final String colegioNombre;

        /** Username del coordinador INSTITUCION que la creó */
        private final String username;

        // ── Auditoría ────────────────────────────────────────────────────

        private final String createdAt;
        private final String updatedAt;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REQUEST — Gestión (cambiar estado + respuesta)
    // ══════════════════════════════════════════════════════════════════════

    @Getter
    @Setter
    @NoArgsConstructor
    public static class GestionRequest {

        @NotNull(message = "El nuevo estado es obligatorio")
        private EstadoSolicitud estado;

        @Size(max = 1000, message = "La respuesta no puede superar 1000 caracteres")
        private String respuesta;
    }
}
