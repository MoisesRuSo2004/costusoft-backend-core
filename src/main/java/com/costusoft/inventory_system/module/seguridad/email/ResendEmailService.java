package com.costusoft.inventory_system.module.seguridad.email;

import com.costusoft.inventory_system.entity.Usuario;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implementacion del servicio de email usando la API de Resend.
 *
 * Todos los metodos son @Async — no bloquean el hilo HTTP del llamador.
 * Los errores se loguean pero no se propagan (fire-and-forget seguro).
 *
 * Documentacion Resend Java SDK: https://resend.com/docs/send-with-java
 */
@Slf4j
@Service
@Profile("prod")   // Solo activo en produccion — en dev usa DevEmailService
public class ResendEmailService implements EmailService {

    private final Resend resendClient;

    @Value("${app.email.from}")
    private String emailFrom;

    public ResendEmailService(@Value("${app.resend.api-key}") String apiKey) {
        this.resendClient = new Resend(apiKey);
    }

    // ── Activacion de cuenta ─────────────────────────────────────────────

    @Async
    @Override
    public void enviarActivacionCuenta(Usuario usuario, String token, String frontendUrl) {
        String url  = frontendUrl + "/activar-cuenta?token=" + token;
        String html = EmailTemplates.activacionCuenta(usuario.getUsername(), url);
        String text = "Bienvenido a Costusoft, " + usuario.getUsername() + ".\n\n"
                    + "Activa tu cuenta en el siguiente enlace (expira en 24 h):\n" + url
                    + "\n\nSi no esperabas este correo, ignóralo.\n\n— Costusoft";

        enviar(usuario.getCorreo(),
            "Activa tu cuenta en Costusoft",
            html, text, "activacion");
    }

    // ── Recuperacion de contrasena ───────────────────────────────────────

    @Async
    @Override
    public void enviarRecuperacionPassword(Usuario usuario, String token, String frontendUrl) {
        String url  = frontendUrl + "/reset-password?token=" + token;
        String html = EmailTemplates.recuperacionPassword(usuario.getUsername(), url);
        String text = "Hola " + usuario.getUsername() + ",\n\n"
                    + "Restablece tu contraseña en el siguiente enlace (expira en 15 min):\n" + url
                    + "\n\nSi no lo solicitaste, ignora este correo.\n\n— Costusoft";

        enviar(usuario.getCorreo(),
            "Restablece tu contraseña — Costusoft",
            html, text, "recuperacion");
    }

    // ── Notificacion nuevo dispositivo ───────────────────────────────────

    @Async
    @Override
    public void enviarNotificacionNuevoDispositivo(
            Usuario usuario,
            String ipAddress,
            String userAgent,
            String fechaHora) {

        String uaResumido = resumirUserAgent(userAgent);
        String html = EmailTemplates.nuevoDispositivo(
                usuario.getUsername(), ipAddress, uaResumido, fechaHora);
        String text = "Hola " + usuario.getUsername() + ",\n\n"
                    + "Detectamos un nuevo acceso a tu cuenta.\n"
                    + "Fecha: " + fechaHora + "\nIP: " + ipAddress
                    + "\n\nSi no fuiste tú, cambia tu contraseña de inmediato.\n\n— Costusoft";

        enviar(usuario.getCorreo(),
            "Nuevo acceso detectado — Costusoft",
            html, text, "nuevo-dispositivo");
    }

    // ── Confirmacion cambio de contrasena ────────────────────────────────

    @Async
    @Override
    public void enviarConfirmacionCambioPassword(Usuario usuario, String fechaHora) {
        String html = EmailTemplates.cambioPasswordConfirmado(usuario.getUsername(), fechaHora);
        String text = "Hola " + usuario.getUsername() + ",\n\n"
                    + "Tu contraseña fue actualizada el " + fechaHora + ".\n"
                    + "Si no realizaste este cambio, contacta al administrador.\n\n— Costusoft";

        enviar(usuario.getCorreo(),
            "Tu contraseña fue actualizada — Costusoft",
            html, text, "cambio-password");
    }

    // ── Metodo interno de envio ──────────────────────────────────────────

    private void enviar(String destinatario, String asunto,
                        String htmlContent, String textContent, String tipo) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(emailFrom)
                    .to(destinatario)
                    .subject(asunto)
                    .html(htmlContent)
                    .text(textContent)
                    .build();

            CreateEmailResponse response = resendClient.emails().send(params);
            log.info("Email [{}] enviado a '{}' — id: {}", tipo, destinatario, response.getId());

        } catch (ResendException e) {
            // Error de Resend API (invalid key, rate limit, etc.)
            log.error("Error al enviar email [{}] a '{}': {}", tipo, destinatario, e.getMessage());
        } catch (Exception e) {
            // Cualquier otro error inesperado — nunca debe afectar el flujo principal
            log.error("Error inesperado al enviar email [{}] a '{}': {}",
                    tipo, destinatario, e.getMessage(), e);
        }
    }

    /** Resume el User-Agent a algo legible para el email de notificacion */
    private String resumirUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Desconocido";
        // Limitar a 120 chars para el template — el UA completo puede ser muy largo
        return userAgent.length() > 120 ? userAgent.substring(0, 117) + "..." : userAgent;
    }
}
