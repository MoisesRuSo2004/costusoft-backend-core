package com.costusoft.inventory_system.entity;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Línea de detalle de un pedido: una prenda específica con su cantidad.
 *
 * Tras ejecutar la calculadora se persisten los campos de resultado
 * (cantidadMaximaFabricable, disponibleIndividual) para que el frontend
 * pueda mostrar el estado sin recalcular.
 */
@Entity
@Table(name = "detalle_pedidos")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedido extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_pedido"))
    private Pedido pedido;

    @NotNull(message = "El uniforme es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uniforme_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_uniforme"))
    private Uniforme uniforme;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Max(value = 10000, message = "La cantidad no puede superar 10.000 unidades")
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /**
     * Talla solicitada para esta prenda en el pedido.
     * El usuario la especifica al agregar el uniforme al pedido.
     * Si no se indica, se toma la talla del uniforme como referencia.
     * Ejemplos: "4", "6", "8", "10", "S", "M", "L", "XL", "Única"
     */
    @Size(max = 10, message = "La talla no puede superar 10 caracteres")
    @Column(name = "talla", length = 10)
    private String talla;

    /**
     * Cantidad máxima fabricable para ESTA prenda considerando el factor global
     * del pedido (insumos compartidos incluidos). Poblado por calcular().
     */
    @Column(name = "cantidad_maxima_fabricable")
    private Integer cantidadMaximaFabricable;

    /**
     * ¿Esta prenda, evaluada sola, tiene stock suficiente?
     * Puede diferir del resultado global si insumos compartidos la afectan.
     */
    @Column(name = "disponible_individual")
    private Boolean disponibleIndividual;

    /** Snapshot del nombre de la prenda al momento de agregar al pedido. */
    @Column(name = "nombre_uniforme_snapshot", length = 150)
    private String nombreUniformeSnapshot;
}
