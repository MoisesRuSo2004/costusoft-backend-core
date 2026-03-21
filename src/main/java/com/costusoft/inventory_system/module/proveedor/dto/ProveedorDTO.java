package com.costusoft.inventory_system.module.proveedor.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTOs del modulo Proveedor.
 *
 * Request — entrada: crear / actualizar proveedor.
 * Response — salida: datos que el frontend recibe.
 */
public class ProveedorDTO {

    // ── Request ─────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "El nombre del proveedor es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        private String nombre;

        @NotBlank(message = "El NIT es obligatorio")
        @Size(max = 20, message = "El NIT no puede superar 20 caracteres")
        private String nit;

        @Size(max = 20, message = "El telefono no puede superar 20 caracteres")
        private String telefono;

        @Size(max = 250, message = "La direccion no puede superar 250 caracteres")
        private String direccion;

        @Email(message = "Formato de correo invalido")
        @Size(max = 100)
        private String correo;
    }

    // ── Response ─────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private final Long id;
        private final String nombre;
        private final String nit;
        private final String telefono;
        private final String direccion;
        private final String correo;
        private final String createdAt;
        private final String updatedAt;
    }
}