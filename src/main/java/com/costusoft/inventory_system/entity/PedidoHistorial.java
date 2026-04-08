package com.costusoft.inventory_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de cada transición de estado de un pedido.
 *
 * Permite auditar completamente quién hizo qué y cuándo:
 * creación, cálculo, aprobación, inicio de producción, entrega, cancelación.
 *
 * No extiende AuditableEntity porque su propio fechaAccion es la marca de tiempo
 * y no requiere createdAt/updatedAt separados.
 */
@Entity
@Table(name = "pedido_historial", indexes = {
        @Index(name = "idx_historial_pedido", columnList = "pedido_id"),
        @Index(name = "idx_historial_fecha",  columnList = "fecha_accion")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, foreignKey = @ForeignKey(name = "fk_historial_pedido"))
    private Pedido pedido;

    /** Estado anterior — null si es la creación inicial. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoPedido estadoNuevo;

    /** Descripción corta de la acción realizada. Ej: "Pedido creado", "Aprobado por admin". */
    @NotBlank
    @Size(max = 200)
    @Column(name = "accion", nullable = false, length = 200)
    private String accion;

    /** Observación adicional — motivo de rechazo, notas del aprobador, etc. */
    @Size(max = 500)
    @Column(name = "observacion", length = 500)
    private String observacion;

    /** Username de quien ejecutó la acción. */
    @NotBlank
    @Column(name = "realizado_por", nullable = false, length = 50)
    private String realizadoPor;

    @Column(name = "fecha_accion", nullable = false)
    private LocalDateTime fechaAccion;
}
