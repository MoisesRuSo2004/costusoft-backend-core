package com.costusoft.inventory_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de cada intento de login (exitoso o fallido).
 *
 * Propósito principal: detectar accesos desde dispositivos o IPs nunca vistos
 * y notificar al usuario por email para que tome accion si no fue el.
 *
 * El {@code deviceHash} es un SHA-256 del User-Agent — sirve para
 * comparar rapido si el "navegador/dispositivo" ya inicio sesion antes.
 */
@Entity
@Table(
    name = "login_audits",
    indexes = {
        @Index(name = "idx_la_usuario",     columnList = "usuario_id"),
        @Index(name = "idx_la_device",      columnList = "usuario_id, device_hash"),
        @Index(name = "idx_la_fecha_login", columnList = "fecha_login")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Direccion IP del cliente (IPv4 o IPv6, max 45 chars) */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** User-Agent completo del cliente */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * SHA-256 (hex) del User-Agent.
     * Se usa para detectar dispositivos nuevos sin exponer el User-Agent completo
     * en indices de la BD.
     */
    @Column(name = "device_hash", length = 64, nullable = false)
    private String deviceHash;

    @Column(name = "fecha_login", nullable = false)
    private LocalDateTime fechaLogin;

    /** true = autenticacion correcta, false = fallo de credenciales */
    @Column(nullable = false)
    private boolean exitoso;
}
