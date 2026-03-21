package com.costusoft.inventory_system.entity;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Entidad que representa un proveedor de insumos.
 *
 * El NIT es único por proveedor y es el identificador fiscal principal.
 */
@Entity
@Table(name = "proveedores", uniqueConstraints = {
        @UniqueConstraint(name = "uk_proveedor_nombre", columnNames = "nombre"),
        @UniqueConstraint(name = "uk_proveedor_nit", columnNames = "nit")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @NotBlank(message = "El NIT es obligatorio")
    @Size(max = 20, message = "El NIT no puede superar 20 caracteres")
    @Column(name = "nit", nullable = false, length = 20)
    private String nit;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Size(max = 250, message = "La dirección no puede superar 250 caracteres")
    @Column(name = "direccion", length = 250)
    private String direccion;

    @Email(message = "Formato de correo inválido")
    @Size(max = 100)
    @Column(name = "correo", length = 100)
    private String correo;
}
