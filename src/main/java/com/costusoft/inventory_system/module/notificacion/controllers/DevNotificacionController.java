package com.costusoft.inventory_system.module.notificacion.controllers;

import com.costusoft.inventory_system.module.notificacion.service.NotificacionService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Envio de prueba — SOLO PERFIL DEV.
 * Permite verificar el push sin cambiar el estado de pedidos reales.
 */
@RestController
@RequestMapping("/api/dev/notificaciones")
@RequiredArgsConstructor
@Profile("dev")
@Tag(name = "[DEV] Notificaciones", description = "Depuracion de push — solo disponible en perfil dev")
public class DevNotificacionController {

    private final NotificacionService notificacionService;

    @Operation(summary = "[DEV] Enviar push de prueba a mis dispositivos")
    @PostMapping("/prueba")
    public ResponseEntity<ApiResponse<Integer>> prueba(Authentication auth) {
        // /api/dev/** es publica: aqui se exige el token del usuario para saber a quien avisar.
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Inicia sesion para enviar la prueba a tus dispositivos", null));
        }
        int entregados = notificacionService.enviarPrueba(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Push de prueba enviado", entregados));
    }

    @Operation(summary = "[DEV] Reenviar el aviso de un pedido ya listo",
            description = "Envia a los coordinadores del colegio el mismo aviso que se envia al marcar "
                    + "el pedido como listo, sin cambiar su estado.")
    @PostMapping("/pedido-listo/{pedidoId}")
    public ResponseEntity<ApiResponse<Integer>> pedidoListo(@PathVariable Long pedidoId) {
        int entregados = notificacionService.reenviarPedidoListo(pedidoId);
        return ResponseEntity.ok(ApiResponse.ok("Aviso de pedido listo reenviado", entregados));
    }
}
