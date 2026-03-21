package com.costusoft.inventory_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;

/**
 * Entidad que representa una entrada de insumos al inventario.
 *
 * Cada entrada tiene uno o más detalles que especifican qué insumos
 * y en qué cantidades ingresaron. El stock de los insumos se actualiza
 * al guardar/modificar/eliminar una entrada.
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