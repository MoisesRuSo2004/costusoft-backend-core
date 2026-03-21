package com.costusoft.inventory_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;

/**
 * Entidad que representa un colegio cliente.
 *
 * Un colegio tiene asociados uno o más uniformes.
 * La relación con Uniforme es bidireccional: Uniforme es el lado dueño (FK
 * colegio_id).
 */
@Entity
@Table(name = "colegios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_colegio_nombre", columnNames = "nombre")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Colegio extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del colegio es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Size(max = 250, message = "La dirección no puede superar 250 caracteres")
    @Column(name = "direccion", length = 250)
    private String direccion;

    /**
     * Uniformes asociados a este colegio.
     * cascade ALL + orphanRemoval: si se elimina el colegio,
     * sus uniformes se eliminan automáticamente.
     */
    @OneToMany(mappedBy = "colegio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Uniforme> uniformes = new ArrayList<>();

    // ── Helpers de relación ─────────────────────────────────────────────

    public void agregarUniforme(Uniforme uniforme) {
        uniformes.add(uniforme);
        uniforme.setColegio(this);
    }

    public void removerUniforme(Uniforme uniforme) {
        uniformes.remove(uniforme);
        uniforme.setColegio(null);
    }
}