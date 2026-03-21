package com.costusoft.inventory_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Detalle de una entrada: qué insumo entró y en qué cantidad.
 *
 * No extiende AuditableEntity porque su ciclo de vida está
 * completamente subordinado a Entrada (cascade ALL).
 */
@Entity
@Table(name = "detalle_entradas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La entrada padre es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrada_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_entrada"))
    private Entrada entrada;

    @NotNull(message = "El insumo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_entrada_insumo"))
    private Insumo insumo;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /**
     * Nombre snapshot: guarda el nombre del insumo al momento de la entrada.
     * Útil para reportes históricos si el nombre del insumo cambia.
     */
    @Column(name = "nombre_insumo_snapshot", length = 120)
    private String nombreInsumoSnapshot;
}