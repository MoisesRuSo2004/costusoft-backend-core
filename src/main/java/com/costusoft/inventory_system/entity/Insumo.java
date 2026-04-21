package com.costusoft.inventory_system.entity;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entidad que representa un insumo (materia prima) del inventario.
 *
 * El campo stockMinimo define el umbral para alertas de stock bajo.
 * El campo riesgo es calculado/asignado por el módulo de predicción ML.
 */
@Entity
@Table(name = "insumos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_insumo_nombre", columnNames = "nombre")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Insumo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del insumo es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(name = "stock", nullable = false)
    private Integer stock;

    @NotBlank(message = "La unidad de medida es obligatoria")
    @Size(max = 30, message = "La unidad de medida no puede superar 30 caracteres")
    @Column(name = "unidad_medida", nullable = false, length = 30)
    private String unidadMedida;

    @Size(max = 50, message = "El tipo no puede superar 50 caracteres")
    @Column(name = "tipo", length = 50)
    private String tipo;

    @Builder.Default
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 10;

    @Enumerated(EnumType.STRING)
    @Column(name = "riesgo", length = 20)
    private NivelRiesgo riesgo;

    // ── Enum nivel de riesgo (asignado por PrediccionService) ───────────
    public enum NivelRiesgo {
        BAJO,
        MEDIO,
        ALTO,
        CRITICO
    }

    // ── Helpers de negocio ──────────────────────────────────────────────

    public boolean tieneStockBajo() {
        return this.stock != null && this.stock <= this.stockMinimo;
    }

    public void incrementarStock(int cantidad) {
        if (cantidad <= 0)
            throw new IllegalArgumentException("La cantidad a incrementar debe ser positiva");
        this.stock += cantidad;
    }

    public void decrementarStock(int cantidad) {
        if (cantidad <= 0)
            throw new IllegalArgumentException("La cantidad a decrementar debe ser positiva");
        if (cantidad > this.stock)
            throw new IllegalStateException("Stock insuficiente para el insumo: " + this.nombre);
        this.stock -= cantidad;
    }
}