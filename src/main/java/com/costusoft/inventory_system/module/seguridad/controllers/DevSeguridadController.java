package com.costusoft.inventory_system.module.seguridad.controllers;

import com.costusoft.inventory_system.entity.LoginAudit;
import com.costusoft.inventory_system.entity.PasswordToken;
import com.costusoft.inventory_system.repo.LoginAuditRepository;
import com.costusoft.inventory_system.repo.PasswordTokenRepository;
import com.costusoft.inventory_system.repo.UsuarioRepository;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Endpoints de depuracion para el modulo de seguridad — SOLO PERFIL DEV.
 *
 * Permite obtener tokens y audits directamente desde la API sin necesitar
 * acceso a la BD ni esperar emails.
 *
 * ⚠️ Este controller NO existe en produccion (@Profile("dev")).
 */
@RestController
@RequestMapping("/api/dev/seguridad")
@RequiredArgsConstructor
@Profile("dev")
@Tag(name = "[DEV] Seguridad Debug",
     description = "Endpoints de depuracion — solo disponibles en perfil dev")
public class DevSeguridadController {

    private final PasswordTokenRepository passwordTokenRepository;
    private final LoginAuditRepository loginAuditRepository;
    private final UsuarioRepository usuarioRepository;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── GET /api/dev/seguridad/tokens ────────────────────────────────────

    @Operation(
        summary = "[DEV] Listar tokens activos",
        description = "Retorna todos los tokens de activacion y recuperacion pendientes (no usados, no expirados). Util para probar flujos sin email real."
    )
    @GetMapping("/tokens")
    public ResponseEntity<ApiResponse<List<TokenDevDTO>>> listarTokensActivos() {

        List<TokenDevDTO> tokens = passwordTokenRepository.findAll()
                .stream()
                .filter(PasswordToken::isValido)
                .map(t -> TokenDevDTO.builder()
                        .id(t.getId())
                        .username(t.getUsuario().getUsername())
                        .correo(t.getUsuario().getCorreo())
                        .tipo(t.getTipo().name())
                        .token(t.getToken())
                        .expiresAt(t.getExpiresAt().format(FMT))
                        .bodyPostman(buildBody(t))
                        .build())
                .toList();

        return ResponseEntity.ok(
                ApiResponse.ok("Tokens activos (" + tokens.size() + ")", tokens));
    }

    // ── GET /api/dev/seguridad/tokens/{username} ─────────────────────────

    @Operation(summary = "[DEV] Tokens activos de un usuario especifico")
    @GetMapping("/tokens/{username}")
    public ResponseEntity<ApiResponse<List<TokenDevDTO>>> tokensPorUsuario(
            @PathVariable String username) {

        List<TokenDevDTO> tokens = usuarioRepository.findByUsername(username)
                .map(usuario -> passwordTokenRepository.findAll()
                        .stream()
                        .filter(t -> t.getUsuario().getId().equals(usuario.getId()))
                        .filter(PasswordToken::isValido)
                        .map(t -> TokenDevDTO.builder()
                                .id(t.getId())
                                .username(t.getUsuario().getUsername())
                                .correo(t.getUsuario().getCorreo())
                                .tipo(t.getTipo().name())
                                .token(t.getToken())
                                .expiresAt(t.getExpiresAt().format(FMT))
                                .bodyPostman(buildBody(t))
                                .build())
                        .toList())
                .orElse(List.of());

        return ResponseEntity.ok(
                ApiResponse.ok("Tokens de '" + username + "' (" + tokens.size() + ")", tokens));
    }

    // ── GET /api/dev/seguridad/logins ────────────────────────────────────

    @Operation(summary = "[DEV] Ultimos 20 registros de login audit")
    @GetMapping("/logins")
    public ResponseEntity<ApiResponse<List<LoginAuditDevDTO>>> ultimosLogins() {

        List<LoginAuditDevDTO> audits = loginAuditRepository
                .findAll(PageRequest.of(0, 20,
                        org.springframework.data.domain.Sort.by("fechaLogin").descending()))
                .stream()
                .map(a -> LoginAuditDevDTO.builder()
                        .id(a.getId())
                        .username(a.getUsuario().getUsername())
                        .ipAddress(a.getIpAddress())
                        .userAgent(a.getUserAgent() != null && a.getUserAgent().length() > 80
                                ? a.getUserAgent().substring(0, 77) + "..."
                                : a.getUserAgent())
                        .deviceHash(a.getDeviceHash().substring(0, 12) + "...")
                        .fechaLogin(a.getFechaLogin().format(FMT))
                        .exitoso(a.isExitoso())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("Ultimos logins", audits));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String buildBody(PasswordToken t) {
        return switch (t.getTipo()) {
            case ACTIVACION -> """
                    POST /api/seguridad/set-password
                    { "token": "%s", "password": "TuPassword123!", "passwordConfirmacion": "TuPassword123!" }
                    """.formatted(t.getToken()).strip();
            case RECUPERACION -> """
                    POST /api/seguridad/reset-password
                    { "token": "%s", "passwordNueva": "NuevaPwd123!", "passwordConfirmacion": "NuevaPwd123!" }
                    """.formatted(t.getToken()).strip();
        };
    }

    // ── DTOs internos ─────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class TokenDevDTO {
        private final Long id;
        private final String username;
        private final String correo;
        private final String tipo;
        private final String token;
        private final String expiresAt;
        /** Body listo para copiar y pegar en Postman */
        private final String bodyPostman;
    }

    @Getter
    @Builder
    public static class LoginAuditDevDTO {
        private final Long id;
        private final String username;
        private final String ipAddress;
        private final String userAgent;
        private final String deviceHash;
        private final String fechaLogin;
        private final boolean exitoso;
    }
}
