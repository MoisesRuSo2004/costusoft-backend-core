package com.costusoft.inventory_system.module.seguridad.email;

import com.costusoft.inventory_system.entity.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implementacion DEV del servicio de email.
 *
 * En lugar de enviar correos reales, imprime en consola un bloque
 * bien visible con toda la informacion necesaria para hacer pruebas
 * en local sin necesitar SMTP ni API key de Resend.
 *
 * Solo activo con el perfil "dev" — en produccion se usa ResendEmailService.
 */
@Slf4j
@Service
@Profile("dev")
public class DevEmailService implements EmailService {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String SEP =
            "═══════════════════════════════════════════════════════════════";

    // ── Activacion de cuenta ─────────────────────────────────────────────

    @Async
    @Override
    public void enviarActivacionCuenta(Usuario usuario, String token, String frontendUrl) {
        String url = frontendUrl + "/set-password?token=" + token;

        log.info("""

                {}
                 📧  [DEV EMAIL] ACTIVACION DE CUENTA
                {}
                 Para  : {} <{}>
                 Asunto: ¡Bienvenido a Costusoft! Activa tu cuenta
                {}
                 ✅  URL DE ACTIVACION (expira en 24h):
                     {}
                {}
                 TOKEN DIRECTO (para Postman):
                     {}
                {}
                 BODY para POST /api/seguridad/set-password :
                 {{
                   "token": "{}",
                   "password": "TuPassword123!",
                   "passwordConfirmacion": "TuPassword123!"
                 }}
                {}
                """,
                SEP, SEP, usuario.getUsername(), usuario.getCorreo(),
                SEP, url, SEP, token, SEP, token, SEP);
    }

    // ── Recuperacion de contrasena ───────────────────────────────────────

    @Async
    @Override
    public void enviarRecuperacionPassword(Usuario usuario, String token, String frontendUrl) {
        String url = frontendUrl + "/reset-password?token=" + token;

        log.info("""

                {}
                 📧  [DEV EMAIL] RECUPERACION DE CONTRASENA
                {}
                 Para  : {} <{}>
                 Asunto: Recuperacion de contrasena — Costusoft
                {}
                 ✅  URL DE RESET (expira en 15 min):
                     {}
                {}
                 TOKEN DIRECTO (para Postman):
                     {}
                {}
                 BODY para POST /api/seguridad/reset-password :
                 {{
                   "token": "{}",
                   "passwordNueva": "NuevaPassword123!",
                   "passwordConfirmacion": "NuevaPassword123!"
                 }}
                {}
                """,
                SEP, SEP, usuario.getUsername(), usuario.getCorreo(),
                SEP, url, SEP, token, SEP, token, SEP);
    }

    // ── Nuevo dispositivo ────────────────────────────────────────────────

    @Async
    @Override
    public void enviarNotificacionNuevoDispositivo(
            Usuario usuario,
            String ipAddress,
            String userAgent,
            String fechaHora) {

        log.info("""

                {}
                 📧  [DEV EMAIL] NUEVO DISPOSITIVO DETECTADO
                {}
                 Para    : {} <{}>
                 Asunto  : Nuevo acceso detectado en tu cuenta — Costusoft
                {}
                 📅 Fecha/Hora : {}
                 🌐 IP         : {}
                 💻 User-Agent : {}
                {}
                 (En produccion se enviaria un email de alerta al usuario)
                {}
                """,
                SEP, SEP, usuario.getUsername(), usuario.getCorreo(),
                SEP, fechaHora, ipAddress, userAgent, SEP, SEP);
    }

    // ── Confirmacion cambio de password ──────────────────────────────────

    @Async
    @Override
    public void enviarConfirmacionCambioPassword(Usuario usuario, String fechaHora) {

        log.info("""

                {}
                 📧  [DEV EMAIL] CONTRASENA ACTUALIZADA
                {}
                 Para    : {} <{}>
                 Asunto  : Tu contrasena fue actualizada — Costusoft
                {}
                 ✅ La contrasena fue cambiada el: {}
                {}
                 (En produccion se enviaria un email de confirmacion al usuario)
                {}
                """,
                SEP, SEP, usuario.getUsername(), usuario.getCorreo(),
                SEP, fechaHora, SEP, SEP);
    }
}
