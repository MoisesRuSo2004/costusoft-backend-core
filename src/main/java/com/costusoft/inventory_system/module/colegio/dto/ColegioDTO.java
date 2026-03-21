package com.costusoft.inventory_system.module.colegio.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * DTOs del modulo Colegio.
 *
 * Un colegio puede crearse con o sin uniformes en el mismo request.
 * El campo uniformes en Request es opcional — si viene, se registran
 * todos en la misma transaccion.
 */
public class ColegioDTO {

    // ── Request ──────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "El nombre del colegio es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        private String nombre;

        @Size(max = 250, message = "La direccion no puede superar 250 caracteres")
        private String direccion;
    }

    // ── Response ─────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private final Long id;
        private final String nombre;
        private final String direccion;
        private final int totalUniformes;
        private final String createdAt;
        private final String updatedAt;
    }

    // ── Response con uniformes incluidos ─────────────────────────────────

    @Getter
    @Builder
    public static class ResponseConUniformes {

        private final Long id;
        private final String nombre;
        private final String direccion;
        private final List<UniformeResumen> uniformes;
        private final String createdAt;
    }

    // ── Resumen de uniforme dentro del colegio ────────────────────────────

    @Getter
    @Builder
    public static class UniformeResumen {
        private final Long id;
        private final String prenda;
        private final String talla;
        private final String genero;
    }
}