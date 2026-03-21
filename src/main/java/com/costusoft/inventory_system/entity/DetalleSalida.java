package com.costusoft.inventory_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Detalle de una salida: qué insumo salió y en qué cantidad.
 *
 * No extiende AuditableEntity porque su ciclo de vida está
 * completamente subordinado a Salida (cascade ALL).
 */
@Entity
@Table(name = "detalle_salidas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleSalida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La salida padre es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salida_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_salida"))
    private Salida salida;

    @NotNull(message = "El insumo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_salida_insumo"))
    private Insumo insumo;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /**
     * Nombre snapshot: guarda el nombre del insumo al momento de la salida.
     * Útil para reportes históricos si el nombre del insumo cambia.
     */
    @Column(name = "nombre_insumo_snapshot", length = 120)
    private String nombreInsumoSnapshot;
}
