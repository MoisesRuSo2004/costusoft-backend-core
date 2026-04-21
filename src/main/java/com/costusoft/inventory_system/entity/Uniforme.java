package com.costusoft.inventory_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;

/**
 * Entidad que representa una prenda de uniforme asociada a un colegio.
 *
 * Un uniforme puede requerir múltiples insumos con cantidades específicas
 * (relación a través de la tabla UniformeInsumo).
 */
@Entity
@Table(name = "uniformes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Uniforme extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la prenda es obligatorio")
    @Size(max = 100, message = "La prenda no puede superar 100 caracteres")
    @Column(name = "prenda", nullable = false, length = 100)
    private String prenda;

    /**
     * Tipo de uniforme: "Diario" | "Educacion Fisica".
     * Libre texto para no limitar futuras categorías.
     */
    @Size(max = 50, message = "El tipo no puede superar 50 caracteres")
    @Column(name = "tipo", length = 50)
    private String tipo;

    /**
     * Género al que aplica: "Hombre" | "Mujer" | "Unisex".
     * null = aplica a todos.
     */
    @Size(max = 20, message = "El género no puede superar 20 caracteres")
    @Column(name = "genero", length = 20)
    private String genero;

    /**
     * Colegio dueño de este uniforme.
     * FK: colegio_id — lado dueño de la relación.
     */
    @NotNull(message = "El colegio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colegio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_uniforme_colegio"))
    private Colegio colegio;

    /**
     * Insumos requeridos para fabricar este uniforme.
     * La tabla de unión es uniforme_insumos.
     */
    @OneToMany(mappedBy = "uniforme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<UniformeInsumo> insumosRequeridos = new ArrayList<>();

    // ── Helpers de relación ─────────────────────────────────────────────

    public void agregarInsumo(UniformeInsumo uniformeInsumo) {
        insumosRequeridos.add(uniformeInsumo);
        uniformeInsumo.setUniforme(this);
    }

    public void removerInsumo(UniformeInsumo uniformeInsumo) {
        insumosRequeridos.remove(uniformeInsumo);
        uniformeInsumo.setUniforme(null);
    }
}