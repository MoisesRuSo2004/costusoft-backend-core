package com.costusoft.inventory_system.module.seguridad.service;

import com.costusoft.inventory_system.entity.LoginAudit;
import com.costusoft.inventory_system.entity.PasswordToken;
import com.costusoft.inventory_system.entity.PasswordToken.TipoToken;
import com.costusoft.inventory_system.entity.Usuario;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.module.seguridad.dto.SeguridadDTO;
import com.costusoft.inventory_system.module.seguridad.email.EmailService;
import com.costusoft.inventory_system.repo.LoginAuditRepository;
import com.costusoft.inventory_system.repo.PasswordTokenRepository;
import com.costusoft.inventory_system.repo.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Implementacion del servicio de seguridad de cuentas.
 *
 * Patrones de seguridad aplicados:
 * - No revelar si un correo existe o no en forgot-password (evita enumeracion)
 * - Tokens UUID v4 de un solo uso, expirados en BD
 * - Invalidar tokens previos antes de generar uno nuevo
 * - DeviceHash basado en SHA-256 del User-Agent (sin PII en indices)
 * - Envio de emails completamente async (no bloquea la respuesta HTTP)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SeguridadServiceImpl implements SeguridadService {

    private final PasswordTokenRepository passwordTokenRepository;
    private final LoginAuditRepository loginAuditRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // TTL en horas/minutos para cada tipo de token
    private static final int ACTIVACION_TTL_HORAS    = 24;
    private static final int RECUPERACION_TTL_MINUTOS = 15;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // ── Activacion de cuenta nueva ────────────────────────────────────────

    @Override
    public void generarYEnviarActivacion(Usuario usuario) {
        // Invalidar cualquier token de activacion anterior para este usuario
        passwordTokenRepository.deleteByUsuarioAndTipo(usuario, TipoToken.ACTIVACION);

        String tokenValor = UUID.randomUUID().toString();

        PasswordToken token = PasswordToken.builder()
                .token(tokenValor)
                .usuario(usuario)
                .tipo(TipoToken.ACTIVACION)
                .expiresAt(LocalDateTime.now().plusHours(ACTIVACION_TTL_HORAS))
                .usado(false)
                .build();

        passwordTokenRepository.save(token);
        log.info("Token de activacion generado para usuario '{}'", usuario.getUsername());

        // Envio async — no bloquea
        emailService.enviarActivacionCuenta(usuario, tokenValor, frontendUrl);
    }

    // ── Establecer password (activar cuenta) ─────────────────────────────

    @Override
    public void activarCuenta(SeguridadDTO.SetPasswordRequest request) {
        // 1. Validar que passwords coincidan
        if (!request.getPassword().equals(request.getPasswordConfirmacion())) {
            throw new BusinessException("La contrasena y su confirmacion no coinciden.");
        }

        // 2. Buscar token valido (no usado)
        PasswordToken token = passwordTokenRepository
                .findByTokenAndUsadoFalse(request.getToken())
                .orElseThrow(() -> new BusinessException(
                        "El enlace de activacion es invalido o ya fue utilizado."));

        // 3. Verificar tipo correcto
        if (token.getTipo() != TipoToken.ACTIVACION) {
            throw new BusinessException("El enlace de activacion es invalido.");
        }

        // 4. Verificar que no haya expirado
        if (token.isExpired()) {
            throw new BusinessException(
                    "El enlace de activacion ha expirado. Contacta al administrador para que reenvie la invitacion.");
        }

        // 5. Activar la cuenta y setear la contrasena
        Usuario usuario = token.getUsuario();
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setCuentaActivada(true);
        usuarioRepository.save(usuario);

        // 6. Invalidar el token
        token.setUsado(true);
        passwordTokenRepository.save(token);

        log.info("Cuenta activada exitosamente para usuario '{}'", usuario.getUsername());
    }

    // ── Forgot password ───────────────────────────────────────────────────

    @Override
    public void solicitarRecuperacionPassword(SeguridadDTO.ForgotPasswordRequest request) {
        // Buscar usuario por correo — si no existe, NO lanzar error (evita enumeracion)
        usuarioRepository.findByCorreo(request.getCorreo()).ifPresent(usuario -> {
            if (!usuario.isActivo()) {
                log.warn("Solicitud de recuperacion para cuenta inactiva: '{}'", usuario.getUsername());
                return; // No enviar email a cuentas desactivadas
            }

            // Invalidar tokens de recuperacion anteriores
            passwordTokenRepository.deleteByUsuarioAndTipo(usuario, TipoToken.RECUPERACION);

            String tokenValor = UUID.randomUUID().toString();

            PasswordToken token = PasswordToken.builder()
                    .token(tokenValor)
                    .usuario(usuario)
                    .tipo(TipoToken.RECUPERACION)
                    .expiresAt(LocalDateTime.now().plusMinutes(RECUPERACION_TTL_MINUTOS))
                    .usado(false)
                    .build();

            passwordTokenRepository.save(token);
            log.info("Token de recuperacion generado para usuario '{}'", usuario.getUsername());

            // Envio async
            emailService.enviarRecuperacionPassword(usuario, tokenValor, frontendUrl);
        });

        // Siempre retorna OK aunque el correo no exista — seguridad anti-enumeracion
        log.debug("Solicitud de recuperacion procesada para correo: {}", request.getCorreo());
    }

    // ── Reset password ────────────────────────────────────────────────────

    @Override
    public void resetearPassword(SeguridadDTO.ResetPasswordRequest request) {
        // 1. Validar que passwords coincidan
        if (!request.getPasswordNueva().equals(request.getPasswordConfirmacion())) {
            throw new BusinessException("La contrasena y su confirmacion no coinciden.");
        }

        // 2. Buscar token valido
        PasswordToken token = passwordTokenRepository
                .findByTokenAndUsadoFalse(request.getToken())
                .orElseThrow(() -> new BusinessException(
                        "El enlace de recuperacion es invalido o ya fue utilizado."));

        // 3. Verificar tipo correcto
        if (token.getTipo() != TipoToken.RECUPERACION) {
            throw new BusinessException("El enlace de recuperacion es invalido.");
        }

        // 4. Verificar expiracion
        if (token.isExpired()) {
            throw new BusinessException(
                    "El enlace de recuperacion ha expirado (15 min). Solicita uno nuevo.");
        }

        // 5. Cambiar la contrasena
        Usuario usuario = token.getUsuario();

        if (passwordEncoder.matches(request.getPasswordNueva(), usuario.getPassword())) {
            throw new BusinessException(
                    "La nueva contrasena no puede ser igual a la anterior.");
        }

        usuario.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        usuarioRepository.save(usuario);

        // 6. Invalidar token
        token.setUsado(true);
        passwordTokenRepository.save(token);

        log.info("Password reseteado exitosamente para usuario '{}'", usuario.getUsername());

        // 7. Notificar via email (async)
        String fechaHora = LocalDateTime.now().format(FORMATTER);
        emailService.enviarConfirmacionCambioPassword(usuario, fechaHora);
    }

    // ── Login Audit + deteccion de nuevo dispositivo ─────────────────────

    @Override
    public void procesarLoginAudit(Usuario usuario, String ipAddress,
                                   String userAgent, boolean exitoso) {
        String deviceHash = calcularDeviceHash(userAgent);

        // Registrar el login en el historial
        LoginAudit audit = LoginAudit.builder()
                .usuario(usuario)
                .ipAddress(ipAddress != null ? ipAddress : "desconocida")
                .userAgent(userAgent != null ? userAgent : "desconocido")
                .deviceHash(deviceHash)
                .fechaLogin(LocalDateTime.now())
                .exitoso(exitoso)
                .build();

        loginAuditRepository.save(audit);

        // Solo notificar si el login fue exitoso y el dispositivo es nuevo
        if (exitoso) {
            // Buscamos si habia logins exitosos con este hash ANTES del actual
            boolean esNuevoDispositivo = loginAuditRepository
                    .findTop10ByUsuarioExitoso(usuario, PageRequest.of(0, 10))
                    .stream()
                    .filter(a -> !a.getId().equals(audit.getId()))   // excluir el recien guardado
                    .noneMatch(a -> deviceHash.equals(a.getDeviceHash()));

            if (esNuevoDispositivo) {
                String fechaHora = LocalDateTime.now().format(FORMATTER);
                log.info("Nuevo dispositivo detectado para usuario '{}' — ip: {}",
                        usuario.getUsername(), ipAddress);
                emailService.enviarNotificacionNuevoDispositivo(
                        usuario, ipAddress, userAgent, fechaHora);
            }
        }
    }

    // ── Notificacion cambio de password ──────────────────────────────────

    @Override
    public void notificarCambioPassword(Usuario usuario) {
        String fechaHora = LocalDateTime.now().format(FORMATTER);
        emailService.enviarConfirmacionCambioPassword(usuario, fechaHora);
        log.info("Notificacion de cambio de password enviada a '{}'", usuario.getUsername());
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    /**
     * Calcula SHA-256 del User-Agent para usar como device fingerprint.
     * Hex-string de 64 caracteres.
     */
    private String calcularDeviceHash(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "unknown";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userAgent.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre esta disponible en Java — esto no debe ocurrir
            log.error("Error calculando device hash: {}", e.getMessage());
            return "unknown";
        }
    }
}
