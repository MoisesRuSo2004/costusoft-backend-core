package com.costusoft.inventory_system.module.notificacion.dto;

import com.costusoft.inventory_system.entity.DispositivoNotificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTOs del modulo de notificaciones push.
 */
public class NotificacionDTO {

    private NotificacionDTO() {
    }

    /** Registro (o actualizacion) del token FCM del dispositivo del usuario autenticado. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class DispositivoRequest {

        @NotBlank(message = "El token es obligatorio")
        @Size(max = 512, message = "El token no puede superar 512 caracteres")
        private String token;

        @NotNull(message = "La plataforma es obligatoria (ANDROID, IOS o WEB)")
        private DispositivoNotificacion.Plataforma plataforma;
    }

    /** Baja del token al cerrar sesion. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class EliminarDispositivoRequest {

        @NotBlank(message = "El token es obligatorio")
        private String token;
    }
}
