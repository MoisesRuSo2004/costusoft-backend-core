package com.costusoft.inventory_system.module.seguridad.email;

/**
 * Templates HTML para los correos transaccionales de Costusoft.
 *
 * Diseno: limpio, profesional, responsive basico.
 * Colores corporativos: primario #1a1a2e, acento #e94560.
 */
public final class EmailTemplates {

  private EmailTemplates() {
  }

  // ── Paleta y layout comun ─────────────────────────────────────────────

  private static String base(String titulo, String cuerpo) {
    return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
          <title>%s</title>
        </head>
        <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:40px 0;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0"
                     style="background:#ffffff;border-radius:12px;overflow:hidden;
                            box-shadow:0 4px 20px rgba(0,0,0,.08);max-width:600px;width:100%%;">

                <!-- HEADER -->
                <tr>
                  <td style="background:linear-gradient(135deg,#1a1a2e 0%%,#16213e 100%%);
                             padding:36px 40px;text-align:center;">
                    <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:700;
                               letter-spacing:1px;">
                      🧵 COSTUSOFT
                    </h1>
                    <p style="margin:6px 0 0;color:#a0aec0;font-size:13px;letter-spacing:2px;">
                      SISTEMA DE INVENTARIO
                    </p>
                  </td>
                </tr>

                <!-- BODY -->
                <tr>
                  <td style="padding:40px 40px 32px;">
                    %s
                  </td>
                </tr>

                <!-- FOOTER -->
                <tr>
                  <td style="background:#f8fafc;padding:20px 40px;
                             border-top:1px solid #e2e8f0;text-align:center;">
                    <p style="margin:0;color:#94a3b8;font-size:12px;line-height:1.6;">
                      Este correo fue generado automaticamente por Costusoft Inventario.<br/>
                      Por favor no respondas a este mensaje.
                    </p>
                  </td>
                </tr>

              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(titulo, cuerpo);
  }

  /** Boton primario reutilizable */
  private static String boton(String url, String texto) {
    return """
        <div style="text-align:center;margin:32px 0;">
          <a href="%s"
             style="background:linear-gradient(135deg,#e94560 0%%,#c0392b 100%%);
                    color:#ffffff;text-decoration:none;padding:14px 36px;
                    border-radius:8px;font-size:15px;font-weight:600;
                    display:inline-block;letter-spacing:.5px;">
            %s
          </a>
        </div>
        """.formatted(url, texto);
  }

  /** Caja de alerta (aviso de seguridad) */
  private static String cajaAlerta(String contenido) {
    return """
        <div style="background:#fff8e1;border-left:4px solid #f59e0b;
                    border-radius:6px;padding:14px 18px;margin:24px 0;">
          %s
        </div>
        """.formatted(contenido);
  }

  /** Caja informativa */
  private static String cajaInfo(String contenido) {
    return """
        <div style="background:#eff6ff;border-left:4px solid #3b82f6;
                    border-radius:6px;padding:14px 18px;margin:24px 0;">
          %s
        </div>
        """.formatted(contenido);
  }

  // ── Templates publicos ────────────────────────────────────────────────

  /**
   * Correo de bienvenida / activacion de cuenta.
   * 
   * @param username   nombre de usuario
   * @param activarUrl URL completa con el token
   */
  public static String activacionCuenta(String username, String activarUrl) {
    String cuerpo = """
        <h2 style="color:#1a1a2e;font-size:22px;margin:0 0 8px;">
          ¡Bienvenido a Costusoft, <span style="color:#e94560;">%s</span>! 🎉
        </h2>
        <p style="color:#64748b;font-size:14px;margin:0 0 20px;">
          Tu cuenta ha sido creada exitosamente por el administrador del sistema.
        </p>
        <p style="color:#334155;font-size:15px;line-height:1.7;margin:0 0 8px;">
          Para comenzar a usar el sistema, necesitas <strong>establecer tu contraseña</strong>.
          Haz clic en el botón de abajo:
        </p>
        %s
        %s
        <p style="color:#475569;font-size:14px;line-height:1.6;margin:20px 0 0;">
          Si no esperabas este correo o no solicitaste una cuenta, puedes ignorarlo
          con total seguridad.
        </p>
        """.formatted(
        username,
        boton(activarUrl, "Activar mi cuenta →"),
        cajaAlerta("""
            <p style="margin:0;color:#92400e;font-size:13px;">
              ⏰ <strong>Este enlace expira en 24 horas</strong> y es de un solo uso.<br/>
              Si expira, contacta al administrador para que reenvíe la invitación.
            </p>
            """));
    return base("Activa tu cuenta — Costusoft", cuerpo);
  }

  /**
   * Correo de recuperacion de contrasena.
   * 
   * @param username nombre de usuario
   * @param resetUrl URL completa con el token de reset
   */
  public static String recuperacionPassword(String username, String resetUrl) {
    String cuerpo = """
        <h2 style="color:#1a1a2e;font-size:22px;margin:0 0 8px;">
          Recuperacion de contrasena 🔑
        </h2>
        <p style="color:#64748b;font-size:14px;margin:0 0 20px;">
          Hola <strong>%s</strong>, recibimos una solicitud para restablecer
          la contrasena de tu cuenta.
        </p>
        <p style="color:#334155;font-size:15px;line-height:1.7;margin:0 0 8px;">
          Si fuiste tu, haz clic en el boton para crear una nueva contrasena:
        </p>
        %s
        %s
        <p style="color:#475569;font-size:14px;line-height:1.6;margin:20px 0 0;">
          Si no solicitaste restablecer tu contrasena, ignora este correo.
          Tu contrasena actual <strong>no cambiara</strong>.
        </p>
        """.formatted(
        username,
        boton(resetUrl, "Restablecer contrasena →"),
        cajaAlerta("""
            <p style="margin:0;color:#92400e;font-size:13px;">
              ⏰ <strong>Este enlace expira en 15 minutos</strong> y solo puede usarse una vez.<br/>
              Si expiró, solicita un nuevo enlace desde la pantalla de login.
            </p>
            """));
    return base("Recupera tu contrasena — Costusoft", cuerpo);
  }

  /**
   * Notificacion de acceso desde dispositivo/IP no reconocido.
   */
  public static String nuevoDispositivo(String username, String ip,
      String userAgent, String fechaHora) {
    String cuerpo = """
        <h2 style="color:#1a1a2e;font-size:22px;margin:0 0 8px;">
          Nuevo acceso detectado 🔍
        </h2>
        <p style="color:#64748b;font-size:14px;margin:0 0 20px;">
          Hola <strong>%s</strong>, detectamos un inicio de sesion en tu cuenta
          desde un dispositivo o ubicacion que no habiamos visto antes.
        </p>
        %s
        <p style="color:#334155;font-size:15px;line-height:1.7;margin:20px 0 8px;">
          <strong>¿Fuiste tu?</strong> No necesitas hacer nada. Tu sesion esta activa.
        </p>
        <p style="color:#334155;font-size:15px;line-height:1.7;margin:0;">
          <strong>¿No fuiste tu?</strong> Cambia tu contrasena de inmediato y contacta
          al administrador del sistema.
        </p>
        """.formatted(
        username,
        cajaInfo("""
            <table cellpadding="0" cellspacing="0" style="width:100%%;">
              <tr>
                <td style="color:#1e40af;font-size:13px;padding:3px 0;
                           font-weight:600;white-space:nowrap;width:120px;">📅 Fecha/Hora</td>
                <td style="color:#1e3a5f;font-size:13px;padding:3px 0;">%s</td>
              </tr>
              <tr>
                <td style="color:#1e40af;font-size:13px;padding:3px 0;
                           font-weight:600;white-space:nowrap;">🌐 Dirección IP</td>
                <td style="color:#1e3a5f;font-size:13px;padding:3px 0;">%s</td>
              </tr>
              <tr>
                <td style="color:#1e40af;font-size:13px;padding:3px 0;
                           font-weight:600;white-space:nowrap;">💻 Dispositivo</td>
                <td style="color:#1e3a5f;font-size:13px;padding:3px 0;
                           word-break:break-all;">%s</td>
              </tr>
            </table>
            """.formatted(fechaHora, ip, userAgent)));
    return base("Nuevo acceso a tu cuenta — Costusoft", cuerpo);
  }

  /**
   * Confirmacion de cambio de contrasena exitoso.
   */
  public static String cambioPasswordConfirmado(String username, String fechaHora) {
    String cuerpo = """
        <h2 style="color:#1a1a2e;font-size:22px;margin:0 0 8px;">
          Contrasena actualizada ✅
        </h2>
        <p style="color:#64748b;font-size:14px;margin:0 0 20px;">
          Hola <strong>%s</strong>, te confirmamos que la contrasena de tu cuenta
          fue cambiada exitosamente.
        </p>
        %s
        <p style="color:#334155;font-size:15px;line-height:1.7;margin:20px 0 0;">
          Si <strong>no realizaste este cambio</strong>, contacta al administrador
          del sistema de inmediato para proteger tu cuenta.
        </p>
        """.formatted(
        username,
        cajaInfo("""
            <p style="margin:0;color:#1e40af;font-size:13px;">
              📅 <strong>Fecha y hora del cambio:</strong> %s
            </p>
            """.formatted(fechaHora)));
    return base("Contrasena actualizada — Costusoft", cuerpo);
  }
}
