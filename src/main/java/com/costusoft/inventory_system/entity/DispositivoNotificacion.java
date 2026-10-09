package com.costusoft.inventory_system.entity;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Dispositivo de un usuario que recibe notificaciones push (Firebase Cloud Messaging).
 *
 * La app movil registra aqui su token FCM al iniciar sesion y lo elimina al cerrarla.
 * Un token pertenece a un solo dispositivo: si otro usuario inicia sesion en el mismo
 * telefono, el token se reasigna a ese usuario.
 */
@Entity
@Table(name = "dispositivos_notificacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_dispositivo_token", columnNames = "token"),
        indexes = @Index(name = "idx_dispositivo_usuario", columnList = "usuario_id"))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoNotificacion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_dispositivo_usuario"))
    private Usuario usuario;

    /** Token de registro que entrega Firebase al dispositivo. */
    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "plataforma", nullable = false, length = 20)
    private Plataforma plataforma;

    public enum Plataforma { ANDROID, IOS, WEB }
}
