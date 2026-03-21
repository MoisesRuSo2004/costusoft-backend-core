package com.costusoft.inventory_system.module.auth.controllers;

import com.costusoft.inventory_system.module.auth.dto.AuthResponseDTO;
import com.costusoft.inventory_system.module.auth.dto.LoginRequestDTO;
import com.costusoft.inventory_system.module.auth.dto.RefreshTokenRequestDTO;
import com.costusoft.inventory_system.module.auth.service.AuthService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller del modulo de autenticacion.
 *
 * Endpoints publicos (sin JWT):
 * POST /api/auth/login — obtener access + refresh token
 * POST /api/auth/refresh — renovar tokens con el refresh token
 *
 * Endpoints protegidos (requieren JWT):
 * GET /api/auth/me — datos del usuario autenticado actual
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion", description = "Login, refresh de tokens y perfil del usuario")
public class AuthController {

    private final AuthService authService;

    // ── POST /api/auth/login ─────────────────────────────────────────────

    @Operation(summary = "Iniciar sesion", description = "Autentica con username y password. Retorna access token (1h) y refresh token (7 dias).")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {

        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Login exitoso", response));
    }

    // ── POST /api/auth/refresh ───────────────────────────────────────────

    @Operation(summary = "Renovar tokens", description = "Emite un nuevo access token y refresh token a partir de un refresh token valido.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refresh(
            @Valid @RequestBody RefreshTokenRequestDTO request) {

        AuthResponseDTO response = authService.refresh(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Tokens renovados exitosamente", response));
    }

    // ── GET /api/auth/me ─────────────────────────────────────────────────

    @Operation(summary = "Datos del usuario autenticado", description = "Retorna la informacion basica del usuario dueno del token JWT activo.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponseDTO.UsuarioInfoDTO>> me() {
        AuthResponseDTO.UsuarioInfoDTO usuario = authService.me();
        return ResponseEntity.ok(
                ApiResponse.ok("Usuario autenticado", usuario));
    }

}