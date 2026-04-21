package com.costusoft.inventory_system.module.seguridad.controllers;

import com.costusoft.inventory_system.module.seguridad.dto.SeguridadDTO;
import com.costusoft.inventory_system.module.seguridad.service.SeguridadService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints publicos del modulo de seguridad de cuentas.
 *
 * Todos los endpoints son publicos (sin JWT) porque se usan exactamente
 * cuando el usuario NO tiene sesion activa.
 *
 * POST /api/seguridad/set-password      — activar cuenta nueva + definir password
 * POST /api/seguridad/forgot-password   — solicitar link de recuperacion
 * POST /api/seguridad/reset-password    — resetear password con el token recibido
 */
@RestController
@RequestMapping("/api/seguridad")
@RequiredArgsConstructor
@Tag(name = "Seguridad de Cuentas",
     description = "Activacion de cuenta, recuperacion y reset de contrasena")
public class SeguridadController {

    private final SeguridadService seguridadService;

    // ── POST /api/seguridad/set-password ─────────────────────────────────

    @Operation(
        summary = "Activar cuenta nueva",
        description = """
            El usuario recibe un email con un link que contiene un token único.
            Con ese token establece su contraseña inicial y activa la cuenta.
            El token expira en 24h y es de un solo uso.
            """
    )
    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @Valid @RequestBody SeguridadDTO.SetPasswordRequest request) {

        seguridadService.activarCuenta(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Cuenta activada exitosamente. Ya puedes iniciar sesion."));
    }

    // ── POST /api/seguridad/forgot-password ──────────────────────────────

    @Operation(
        summary = "Solicitar recuperacion de contrasena",
        description = """
            Envia un email con un link de recuperacion al correo registrado.
            Si el correo no existe en el sistema, la respuesta es la misma
            (medida de seguridad anti-enumeracion de usuarios).
            El token expira en 15 minutos.
            """
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody SeguridadDTO.ForgotPasswordRequest request) {

        seguridadService.solicitarRecuperacionPassword(request);
        // Siempre retorna el mismo mensaje — no revelar si el correo existe o no
        return ResponseEntity.ok(
                ApiResponse.ok("Si el correo esta registrado, recibiras un enlace en los proximos minutos."));
    }

    // ── POST /api/seguridad/reset-password ───────────────────────────────

    @Operation(
        summary = "Resetear contrasena con token",
        description = """
            Valida el token recibido por email y reemplaza la contraseña.
            El token expira en 15 minutos y solo puede usarse una vez.
            Tras el reset exitoso se envia un email de confirmacion.
            """
    )
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody SeguridadDTO.ResetPasswordRequest request) {

        seguridadService.resetearPassword(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Contrasena restablecida exitosamente. Ya puedes iniciar sesion."));
    }
}
