package com.costusoft.inventory_system.module.seguridad.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTOs del modulo de seguridad.
 */
public class SeguridadDTO {

    // ── Establecer contrasena (activacion de cuenta nueva) ───────────────

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SetPasswordRequest {

        @NotBlank(message = "El token es obligatorio")
        private String token;

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 100, message = "La contrasena debe tener entre 8 y 100 caracteres")
        private String password;

        @NotBlank(message = "La confirmacion de contrasena es obligatoria")
        private String passwordConfirmacion;
    }

    // ── Solicitar recuperacion de contrasena ─────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ForgotPasswordRequest {

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo invalido")
        private String correo;
    }

    // ── Ejecutar reset de contrasena ─────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ResetPasswordRequest {

        @NotBlank(message = "El token es obligatorio")
        private String token;

        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 8, max = 100, message = "La contrasena debe tener entre 8 y 100 caracteres")
        private String passwordNueva;

        @NotBlank(message = "La confirmacion de contrasena es obligatoria")
        private String passwordConfirmacion;
    }
}
