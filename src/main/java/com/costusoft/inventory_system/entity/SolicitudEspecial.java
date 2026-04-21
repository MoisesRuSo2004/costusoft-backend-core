package com.costusoft.inventory_system.entity;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Solicitud especial enviada por un coordinador de colegio (rol INSTITUCION).
 *
 * Permite gestionar peticiones que requieren atencion manual del equipo
 * Costusoft: ajustes de talla, pedidos urgentes, cambios de fecha, etc.
 *
 * Flujo: PENDIENTE → EN_REVISION → RESUELTA | RECHAZADA
 */
@Entity
@Table(name = "solicitudes_especiales",
       indexes = {
           @Index(name = "idx_solicitud_usuario",  columnList = "usuario_id"),
           @Index(name = "idx_solicitud_colegio",  columnList = "colegio_id"),
           @Index(name = "idx_solicitud_estado",   columnList = "estado")
       })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudEspecial extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario INSTITUCION que envio la solicitud */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_solicitud_usuario"))
    private Usuario usuario;

    /** Colegio al que pertenece la solicitud */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_solicitud_colegio"))
    private Colegio colegio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoSolicitud tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 200, message = "El asunto no puede superar 200 caracteres")
    @Column(name = "asunto", nullable = false, length = 200)
    private String asunto;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 1000, message = "La descripcion no puede superar 1000 caracteres")
    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;

    /** Respuesta del equipo Costusoft (null hasta que la gestion el admin) */
    @Size(max = 1000, message = "La respuesta no puede superar 1000 caracteres")
    @Column(name = "respuesta", length = 1000)
    private String respuesta;

    /** Fecha en que se registro la respuesta */
    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    // ── Tipos de solicitud ───────────────────────────────────────────────

    public enum TipoSolicitud {
        /** Ajuste en tallas de prendas de un pedido */
        AJUSTE_TALLA,
        /** Solicitud de produccion con prioridad alta */
        PEDIDO_URGENTE,
        /** Cambio en la fecha estimada de entrega */
        CAMBIO_FECHA_ENTREGA,
        /** Pregunta o consulta general sobre el pedido */
        CONSULTA_GENERAL,
        /** Solicitud de devolucion o correccion de un pedido entregado */
        DEVOLUCION
    }

    // ── Estados de solicitud ─────────────────────────────────────────────

    public enum EstadoSolicitud {
        /** Recien enviada, sin gestion */
        PENDIENTE,
        /** Equipo Costusoft la revisa */
        EN_REVISION,
        /** Solicitud atendida y respondida */
        RESUELTA,
        /** Solicitud no procedente */
        RECHAZADA
    }
}
