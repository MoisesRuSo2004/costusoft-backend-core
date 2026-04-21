package com.costusoft.inventory_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Token de seguridad para activacion de cuenta y recuperacion de contrasena.
 *
 * Ciclo de vida:
 *   ACTIVACION  — generado al crear usuario, expira en 24h, un solo uso
 *   RECUPERACION — generado al pedir reset, expira en 15 min, un solo uso
 *
 * Un token se invalida marcando {@code usado = true} inmediatamente despues de consumirse.
 * Nunca se reutiliza.
 */
@Entity
@Table(
    name = "password_tokens",
    indexes = {
        @Index(name = "idx_pt_token",   columnList = "token"),
        @Index(name = "idx_pt_usuario", columnList = "usuario_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID aleatorio de 36 chars — unico en toda la tabla */
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoToken tipo;

    /** Fecha/hora exacta de expiracion */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** true cuando el token ya fue consumido — nunca reutilizar */
    @Column(nullable = false)
    @Builder.Default
    private boolean usado = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Enum ─────────────────────────────────────────────────────────────

    public enum TipoToken {
        /** Para activar una cuenta recien creada y definir la contrasena inicial */
        ACTIVACION,
        /** Para recuperar el acceso cuando el usuario olvido su contrasena */
        RECUPERACION
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Retorna true si el token ya paso su fecha de expiracion */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /** Un token es valido si NO fue usado Y NO ha expirado */
    public boolean isValido() {
        return !usado && !isExpired();
    }
}
