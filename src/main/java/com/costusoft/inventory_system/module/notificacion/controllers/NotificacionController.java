package com.costusoft.inventory_system.module.notificacion.controllers;

import com.costusoft.inventory_system.module.notificacion.dto.NotificacionDTO;
import com.costusoft.inventory_system.module.notificacion.service.NotificacionService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Registro de dispositivos para notificaciones push de la app movil.
 * Disponible para cualquier usuario autenticado: cada uno solo gestiona sus dispositivos.
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Dispositivos que reciben avisos push (Firebase Cloud Messaging)")
public class NotificacionController {

    private final NotificacionService notificacionService;

    @Operation(summary = "Registrar dispositivo",
            description = "Guarda el token FCM del dispositivo del usuario autenticado. "
                    + "Si el token ya existia (otro usuario en el mismo telefono), se reasigna.")
    @PostMapping("/dispositivos")
    public ResponseEntity<ApiResponse<Void>> registrar(
            @Valid @RequestBody NotificacionDTO.DispositivoRequest request,
            Authentication auth) {
        notificacionService.registrarDispositivo(request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Dispositivo registrado", null));
    }

    @Operation(summary = "Eliminar dispositivo",
            description = "Deja de enviar avisos a este dispositivo (al cerrar sesion en la app).")
    @DeleteMapping("/dispositivos")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @Valid @RequestBody NotificacionDTO.EliminarDispositivoRequest request,
            Authentication auth) {
        notificacionService.eliminarDispositivo(request.getToken(), auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Dispositivo eliminado", null));
    }

    @Operation(summary = "Enviar aviso de prueba",
            description = "Envia un aviso solo a los dispositivos del usuario autenticado, para "
                    + "comprobar que le llegan. Devuelve a cuantos dispositivos se entrego.")
    @PostMapping("/prueba")
    public ResponseEntity<ApiResponse<Integer>> prueba(Authentication auth) {
        int entregados = notificacionService.enviarPrueba(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Aviso de prueba enviado", entregados));
    }
}
