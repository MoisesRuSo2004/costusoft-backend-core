package com.costusoft.inventory_system.entity;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entidad que representa un usuario del sistema.
 *
 * Roles soportados: ADMIN, USER
 * La contraseña siempre se almacena hasheada con BCrypt.
 */
@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_usuario_correo", columnNames = "correo")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Column(name = "password", nullable = false)
    private String password;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    @Column(name = "correo", nullable = false, length = 100)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    // ── Enum de roles ───────────────────────────────────────────────────
    public enum Rol {
        /** Acceso total al sistema: usuarios, proveedores, stock, reportes. */
        ADMIN,
        /** Secretaria/operador: crea solicitudes de entradas y salidas. */
        USER,
        /** Operador de almacén: confirma o rechaza solicitudes PENDIENTES. No crea solicitudes. */
        BODEGA
    }
}
