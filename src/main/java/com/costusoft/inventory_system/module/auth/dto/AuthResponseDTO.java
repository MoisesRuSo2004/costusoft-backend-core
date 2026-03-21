package com.costusoft.inventory_system.module.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO de respuesta tras un login exitoso.
 *
 * Retorna:
 * - accessToken : JWT de corta duracion (1h prod / 24h dev)
 * - refreshToken : JWT de larga duracion para renovar el access (7 dias)
 * - tokenType : siempre "Bearer"
 * - expiresIn : milisegundos hasta que expira el accessToken
 * - usuario : datos basicos del usuario autenticado (sin contrasena)
 */
@Getter
@Builder
public class AuthResponseDTO {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresIn;
    private final UsuarioInfoDTO usuario;

    // ── Info del usuario dentro de la respuesta ─────────────────────────

    @Getter
    @Builder
    public static class UsuarioInfoDTO {
        private final Long id;
        private final String username;
        private final String correo;
        private final String rol;
    }
}
