package com.costusoft.inventory_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;

/**
 * Entidad que representa una entrada de insumos al inventario.
 *
 * Ciclo de vida con rol BODEGA:
 *   PENDIENTE  → USER/ADMIN crea la solicitud. Stock intacto.
 *   CONFIRMADA → BODEGA/ADMIN verifica físicamente y aprueba. Stock incrementado.
 *   RECHAZADA  → BODEGA/ADMIN rechaza con motivo. Stock sin cambios.
 */
@Entity
@Table(name = "entradas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrada extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La fecha de entrada es obligatoria")
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /**
     * Proveedor que entregó los insumos.
     * Es opcional: puede haber entradas de ajuste sin proveedor.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", foreignKey = @ForeignKey(name = "fk_entrada_proveedor"))
    private Proveedor proveedor;

    /**
     * Detalles de la entrada.
     * cascade ALL + orphanRemoval garantiza integridad total.
     */
    @NotEmpty(message = "La entrada debe tener al menos un detalle")
    @OneToMany(mappedBy = "entrada", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleEntrada> detalles = new ArrayList<>();

    // ── Campos de flujo de aprobación ───────────────────────────────────

    /** Estado actual del movimiento. Inicia siempre en PENDIENTE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoMovimiento estado = EstadoMovimiento.PENDIENTE;

    /** Username del BODEGA/ADMIN que confirmó o rechazó la solicitud. */
    @Column(name = "confirmada_por", length = 50)
    private String confirmadaPor;

    /** Motivo del rechazo. Solo presente cuando estado = RECHAZADA. */
    @Size(max = 500, message = "El motivo de rechazo no puede superar 500 caracteres")
    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    /** Fecha y hora exacta en que se confirmó o rechazó. Para trazabilidad. */
    @Column(name = "confirmada_at")
    private LocalDateTime confirmadaAt;

    // ── Helpers de estado ───────────────────────────────────────────────

    public boolean isPendiente()  { return EstadoMovimiento.PENDIENTE  == this.estado; }
    public boolean isConfirmada() { return EstadoMovimiento.CONFIRMADA == this.estado; }
    public boolean isRechazada()  { return EstadoMovimiento.RECHAZADA  == this.estado; }

    // ── Helpers de relación ─────────────────────────────────────────────

    public void agregarDetalle(DetalleEntrada detalle) {
        detalles.add(detalle);
        detalle.setEntrada(this);
    }

    public void limpiarDetalles() {
        detalles.forEach(d -> d.setEntrada(null));
        detalles.clear();
    }
}