package com.costusoft.inventory_system.entity;

import java.math.BigDecimal;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Tabla de unión entre Uniforme e Insumo.
 *
 * Registra cuánto de cada insumo (y en qué unidad) requiere
 * fabricar una unidad de un uniforme determinado.
 *
 * PK compuesta: (uniforme_id, insumo_id)
 */
@Entity
@Table(name = "uniforme_insumos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_uniforme_insumo", columnNames = { "uniforme_id", "insumo_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniformeInsumo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El uniforme es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uniforme_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ui_uniforme"))
    private Uniforme uniforme;

    @NotNull(message = "El insumo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ui_insumo"))
    private Insumo insumo;

    @NotNull(message = "La cantidad base es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad base debe ser mayor a cero")
    @Column(name = "cantidad_base", nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidadBase;

    @NotBlank(message = "La unidad de medida es obligatoria")
    @Size(max = 30)
    @Column(name = "unidad_medida", nullable = false, length = 30)
    private String unidadMedida;
}
