package com.costusoft.inventory_system.module.usuario.dto;

import com.costusoft.inventory_system.entity.Usuario;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTOs del modulo Usuario.
 *
 * CreateRequest — creacion: NO incluye password. Al crear un usuario se genera
 *                 un token de activacion y se envia un email para que el
 *                 propio usuario establezca su contrasena.
 * UpdateRequest — actualizacion: password opcional (si no viene, se mantiene el actual).
 * Response      — salida: NUNCA expone el password.
 */
public class UsuarioDTO {

    // ── Create Request ───────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El username solo puede contener letras, numeros, puntos, guiones y guion bajo")
        private String username;

        // password NO se incluye en la creacion.
        // El admin no asigna la contrasena — el sistema envia un email de activacion
        // para que el propio usuario la defina de forma segura.

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo invalido")
        @Size(max = 100)
        private String correo;

        @NotNull(message = "El rol es obligatorio")
        private Usuario.Rol rol;

        @Builder.Default
        private boolean activo = true;

        /**
         * Obligatorio cuando rol = INSTITUCION.
         * El ID del colegio al que pertenece el coordinador.
         * Ignorado para roles ADMIN, USER y BODEGA.
         */
        private Long colegioId;
    }

    // ── Update Request ───────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El username solo puede contener letras, numeros, puntos, guiones y guion bajo")
        private String username;

        // Password opcional en actualizacion — si viene null o vacio se mantiene el
        // actual
        @Size(min = 6, max = 100, message = "La contrasena debe tener entre 6 y 100 caracteres")
        private String password;

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo invalido")
        @Size(max = 100)
        private String correo;

        @NotNull(message = "El rol es obligatorio")
        private Usuario.Rol rol;

        private boolean activo;
    }

    // ── Change Password Request ──────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ChangePasswordRequest {

        @NotBlank(message = "La contrasena actual es obligatoria")
        private String passwordActual;

        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 6, max = 100, message = "La nueva contrasena debe tener entre 6 y 100 caracteres")
        private String passwordNueva;

        @NotBlank(message = "La confirmacion de contrasena es obligatoria")
        private String passwordConfirmacion;
    }

    // ── Response ─────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private final Long id;
        private final String username;
        private final String correo;
        private final String rol;
        private final boolean activo;
        /** true = el usuario ya activo su cuenta y puede iniciar sesion */
        private final boolean cuentaActivada;
        /**
         * ID del colegio asociado. Solo presente cuando rol = INSTITUCION.
         * null para los demas roles.
         */
        private final Long colegioId;
        /** Nombre del colegio asociado (solo para rol INSTITUCION). */
        private final String nombreColegio;
        private final String createdAt;
        private final String updatedAt;
        // password NUNCA se incluye en el response
    }
}