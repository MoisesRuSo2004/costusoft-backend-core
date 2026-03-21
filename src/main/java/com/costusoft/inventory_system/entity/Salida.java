package com.costusoft.inventory_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;

/**
 * Entidad que representa una salida de insumos del inventario.
 *
 * Cada salida descuenta stock de los insumos involucrados.
 * Si el stock es insuficiente se lanza StockInsuficienteException
 * antes de persistir cualquier cambio.
 */
@Entity
@Table(name = "salidas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salida extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La fecha de salida es obligatoria")
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /**
     * Colegio al que se destina la salida (opcional).
     * Permite trazabilidad de qué colegio consumió qué insumos.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colegio_id", foreignKey = @ForeignKey(name = "fk_salida_colegio"))
    private Colegio colegio;

    @NotEmpty(message = "La salida debe tener al menos un detalle")
    @OneToMany(mappedBy = "salida", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleSalida> detalles = new ArrayList<>();

    // ── Helpers de relación ─────────────────────────────────────────────

    public void agregarDetalle(DetalleSalida detalle) {
        detalles.add(detalle);
        detalle.setSalida(this);
    }

    public void limpiarDetalles() {
        detalles.forEach(d -> d.setSalida(null));
        detalles.clear();
    }
}
