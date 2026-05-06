package com.costusoft.inventory_system.module.seguridad.email;

/**
 * Templates HTML para los correos transaccionales de Costusoft.
 *
 * Características:
 * - Diseño profesional (estilo SaaS moderno)
 * - Dark mode real via @media (prefers-color-scheme: dark) + clases CSS
 * - Preheader text para previsualizacion en bandeja de entrada
 * - Estructura table-based compatible con Outlook, Gmail, Apple Mail
 * - Inline styles como fallback light; clases CSS sobreescriben en dark
 */
public final class EmailTemplates {

    private EmailTemplates() {}

    // ── Paleta de colores ──────────────────────────────────────────────────
    private static final String ACCENT  = "#4f46e5"; // indigo-600
    private static final String ACCENT2 = "#4338ca"; // indigo-700

    // ── Base HTML ──────────────────────────────────────────────────────────

    private static String base(String titulo, String preheader, String cuerpo) {
        return """
            <!DOCTYPE html>
            <html lang="es" xmlns="http://www.w3.org/1999/xhtml">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
              <meta name="color-scheme" content="light dark"/>
              <meta name="supported-color-schemes" content="light dark"/>
              <title>%s</title>
              <style>
                :root { color-scheme: light dark; }
                body  { margin:0; padding:0; }

                @media (prefers-color-scheme: dark) {
                  .em-outer    { background-color: #080d18 !important; }
                  .em-card     { background-color: #111827 !important; }
                  .em-header   { background: linear-gradient(135deg,#0a0f1e 0%%,#111827 100%%) !important; }
                  .em-footer   { background-color: #0c1120 !important;
                                 border-top-color: #1f2937 !important; }
                  .em-h2       { color: #f1f5f9 !important; }
                  .em-accent   { color: #818cf8 !important; }
                  .em-lead     { color: #94a3b8 !important; }
                  .em-body     { color: #cbd5e1 !important; }
                  .em-strong   { color: #e2e8f0 !important; }
                  .em-footer-t { color: #4b5563 !important; }
                  .em-footer-l { color: #818cf8 !important; }
                  .em-divider  { border-top-color: #1f2937 !important; }
                  /* Caja alerta (amarillo) */
                  .em-alert    { background-color: #130e02 !important;
                                 border-left-color: #d97706 !important; }
                  .em-alert-t  { color: #fcd34d !important; }
                  /* Caja info (indigo) */
                  .em-info     { background-color: #0c0f28 !important;
                                 border-left-color: #6366f1 !important; }
                  .em-info-t   { color: #a5b4fc !important; }
                  /* Tabla de datos */
                  .em-row-h    { color: #818cf8 !important; }
                  .em-row-v    { color: #cbd5e1 !important; }
                }
              </style>
            </head>
            <body class="em-outer"
                  style="margin:0;padding:0;background-color:#f1f5f9;
                         font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',
                         Roboto,Helvetica,Arial,sans-serif;">

              <!-- Preheader (invisible, mejora preview en bandeja y spam score) -->
              <div style="display:none;max-height:0;overflow:hidden;mso-hide:all;
                          font-size:1px;line-height:1px;color:#f1f5f9;">
                %s &nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;
                &nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;
              </div>

              <table width="100%%" cellpadding="0" cellspacing="0" role="presentation">
                <tr>
                  <td align="center" style="padding:40px 16px;">
                    <table width="100%%" cellpadding="0" cellspacing="0" role="presentation"
                           style="max-width:560px;width:100%%;">

                      <!-- ══ HEADER ══ -->
                      <tr>
                        <td class="em-header"
                            style="background:linear-gradient(135deg,#1e293b 0%%,#0f172a 100%%);
                                   border-radius:16px 16px 0 0;
                                   padding:36px 40px 32px;
                                   text-align:center;">

                          <!-- Logo pill -->
                          <table cellpadding="0" cellspacing="0" role="presentation"
                                 style="margin:0 auto 16px;">
                            <tr>
                              <td style="background:rgba(255,255,255,0.08);
                                         border:1px solid rgba(255,255,255,0.12);
                                         border-radius:12px;padding:10px 22px;">
                                <span style="font-size:17px;font-weight:800;color:#ffffff;
                                             letter-spacing:3px;text-transform:uppercase;
                                             font-family:-apple-system,BlinkMacSystemFont,
                                             'Segoe UI',sans-serif;">
                                  ✦&nbsp;COSTUSOFT
                                </span>
                              </td>
                            </tr>
                          </table>

                          <p style="margin:0;color:#475569;font-size:11px;
                                    letter-spacing:2.5px;text-transform:uppercase;
                                    font-weight:500;">
                            Sistema de Inventario
                          </p>
                        </td>
                      </tr>

                      <!-- ══ BODY ══ -->
                      <tr>
                        <td class="em-card"
                            style="background:#ffffff;padding:40px 40px 32px;">
                          %s
                        </td>
                      </tr>

                      <!-- ══ FOOTER ══ -->
                      <tr>
                        <td class="em-footer"
                            style="background:#f8fafc;
                                   border-top:1px solid #e2e8f0;
                                   border-radius:0 0 16px 16px;
                                   padding:24px 40px;
                                   text-align:center;">
                          <p class="em-footer-t"
                             style="margin:0 0 10px;color:#94a3b8;
                                    font-size:12px;line-height:1.7;">
                            Mensaje automático de
                            <strong style="color:#64748b;">Costusoft Inventario</strong>.<br/>
                            Por favor no respondas a este correo.
                          </p>
                          <a class="em-footer-l" href="https://app.costusoft.com"
                             style="color:#6366f1;font-size:11px;
                                    text-decoration:none;font-weight:600;">
                            app.costusoft.com
                          </a>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(titulo, preheader, cuerpo);
    }

    // ── Componentes ────────────────────────────────────────────────────────

    /** Botón CTA principal */
    private static String boton(String url, String texto) {
        return """
            <table cellpadding="0" cellspacing="0" role="presentation"
                   style="margin:32px auto 28px;">
              <tr>
                <td style="border-radius:10px;
                           background:linear-gradient(135deg,%s 0%%,%s 100%%);
                           mso-padding-alt:0;">
                  <a href="%s"
                     style="display:inline-block;padding:14px 36px;
                            color:#ffffff;text-decoration:none;
                            font-size:15px;font-weight:700;letter-spacing:.3px;
                            border-radius:10px;
                            font-family:-apple-system,BlinkMacSystemFont,
                            'Segoe UI',sans-serif;">
                    %s
                  </a>
                </td>
              </tr>
            </table>
            """.formatted(ACCENT, ACCENT2, url, texto);
    }

    /** Caja de aviso temporal (amarilla) */
    private static String cajaAlerta(String html) {
        return """
            <table cellpadding="0" cellspacing="0" role="presentation"
                   style="width:100%%;margin:20px 0;">
              <tr>
                <td class="em-alert"
                    style="background:#fffbeb;border-left:3px solid #f59e0b;
                           border-radius:0 8px 8px 0;padding:13px 18px;">
                  <span class="em-alert-t"
                        style="color:#78350f;font-size:13px;line-height:1.6;">
                    %s
                  </span>
                </td>
              </tr>
            </table>
            """.formatted(html);
    }

    /** Caja informativa (indigo) */
    private static String cajaInfo(String html) {
        return """
            <table cellpadding="0" cellspacing="0" role="presentation"
                   style="width:100%%;margin:20px 0;">
              <tr>
                <td class="em-info"
                    style="background:#eef2ff;border-left:3px solid #6366f1;
                           border-radius:0 8px 8px 0;padding:13px 18px;">
                  <span class="em-info-t"
                        style="color:#3730a3;font-size:13px;line-height:1.7;">
                    %s
                  </span>
                </td>
              </tr>
            </table>
            """.formatted(html);
    }

    /** Línea divisora */
    private static String divider() {
        return """
            <table cellpadding="0" cellspacing="0" role="presentation"
                   style="width:100%%;margin:24px 0 20px;">
              <tr>
                <td class="em-divider"
                    style="border-top:1px solid #e2e8f0;
                           font-size:0;line-height:0;">&nbsp;</td>
              </tr>
            </table>
            """;
    }

    // ── Templates públicos ─────────────────────────────────────────────────

    /**
     * Correo de bienvenida / activación de cuenta nueva.
     */
    public static String activacionCuenta(String username, String activarUrl) {
        String cuerpo = """
            <h2 class="em-h2"
                style="margin:0 0 8px;color:#0f172a;font-size:22px;
                       font-weight:700;line-height:1.3;">
              Bienvenido, <span class="em-accent" style="color:%s;">%s</span> 👋
            </h2>
            <p class="em-lead"
               style="margin:0 0 24px;color:#64748b;font-size:14px;line-height:1.6;">
              Tu cuenta ha sido creada por el administrador de Costusoft.
              Sigue el paso a continuación para empezar.
            </p>

            <p class="em-body"
               style="margin:0 0 4px;color:#334155;font-size:15px;line-height:1.7;">
              Haz clic en el botón para establecer tu contraseña y activar tu cuenta:
            </p>

            %s

            %s

            %s

            <p class="em-body"
               style="margin:20px 0 0;color:#64748b;font-size:13px;line-height:1.6;">
              Si no esperabas este correo, puedes ignorarlo con total seguridad.
              Nadie podrá acceder a tu cuenta sin completar la activación.
            </p>
            """.formatted(
                ACCENT, username,
                boton(activarUrl, "Activar mi cuenta →"),
                cajaAlerta("⏰&nbsp;<strong>Este enlace expira en 24 horas</strong> y es de un solo uso."
                    + " Si expira, contacta al administrador para que reenvíe la invitación."),
                divider());

        return base(
            "Activa tu cuenta — Costusoft",
            "Bienvenido a Costusoft. Activa tu cuenta para comenzar.",
            cuerpo);
    }

    /**
     * Correo de recuperación de contraseña.
     */
    public static String recuperacionPassword(String username, String resetUrl) {
        String cuerpo = """
            <h2 class="em-h2"
                style="margin:0 0 8px;color:#0f172a;font-size:22px;
                       font-weight:700;line-height:1.3;">
              Restablecer contraseña 🔑
            </h2>
            <p class="em-lead"
               style="margin:0 0 24px;color:#64748b;font-size:14px;line-height:1.6;">
              Hola <strong class="em-strong" style="color:#1e293b;">%s</strong>,
              recibimos una solicitud para restablecer la contraseña de tu cuenta.
            </p>

            <p class="em-body"
               style="margin:0 0 4px;color:#334155;font-size:15px;line-height:1.7;">
              Si fuiste tú, haz clic en el botón para crear una nueva contraseña:
            </p>

            %s

            %s

            %s

            <p class="em-body"
               style="margin:20px 0 0;color:#64748b;font-size:13px;line-height:1.6;">
              Si <strong>no solicitaste</strong> este cambio, ignora este correo
              con seguridad. Tu contraseña actual no será modificada.
            </p>
            """.formatted(
                username,
                boton(resetUrl, "Restablecer contraseña →"),
                cajaAlerta("⏰&nbsp;<strong>Este enlace expira en 15 minutos</strong> y solo puede usarse una vez."
                    + " Si expiró, solicita uno nuevo desde la pantalla de login."),
                divider());

        return base(
            "Restablece tu contraseña — Costusoft",
            "Solicitud para restablecer tu contraseña en Costusoft.",
            cuerpo);
    }

    /**
     * Notificación de acceso desde dispositivo o IP no reconocido.
     */
    public static String nuevoDispositivo(String username, String ip,
            String userAgent, String fechaHora) {

        String tablaInfo = """
            <table cellpadding="0" cellspacing="0" role="presentation" style="width:100%%;">
              <tr>
                <td class="em-row-h" style="color:#4f46e5;font-size:13px;padding:4px 0;
                    font-weight:600;white-space:nowrap;width:130px;vertical-align:top;">
                  📅&nbsp;Fecha/Hora
                </td>
                <td class="em-row-v" style="color:#1e293b;font-size:13px;padding:4px 0;">%s</td>
              </tr>
              <tr>
                <td class="em-row-h" style="color:#4f46e5;font-size:13px;padding:4px 0;
                    font-weight:600;white-space:nowrap;vertical-align:top;">
                  🌐&nbsp;IP
                </td>
                <td class="em-row-v" style="color:#1e293b;font-size:13px;padding:4px 0;">%s</td>
              </tr>
              <tr>
                <td class="em-row-h" style="color:#4f46e5;font-size:13px;padding:4px 0;
                    font-weight:600;white-space:nowrap;vertical-align:top;">
                  💻&nbsp;Dispositivo
                </td>
                <td class="em-row-v" style="color:#1e293b;font-size:13px;padding:4px 0;
                    word-break:break-all;">%s</td>
              </tr>
            </table>
            """.formatted(fechaHora, ip, userAgent);

        String cuerpo = """
            <h2 class="em-h2"
                style="margin:0 0 8px;color:#0f172a;font-size:22px;
                       font-weight:700;line-height:1.3;">
              Nuevo acceso detectado 🔍
            </h2>
            <p class="em-lead"
               style="margin:0 0 24px;color:#64748b;font-size:14px;line-height:1.6;">
              Hola <strong class="em-strong" style="color:#1e293b;">%s</strong>,
              detectamos un inicio de sesión desde un dispositivo o ubicación
              que no reconocemos.
            </p>

            %s

            <p class="em-body"
               style="margin:20px 0 6px;color:#334155;font-size:15px;line-height:1.7;">
              <strong>¿Fuiste tú?</strong>&nbsp; No necesitas hacer nada. Tu sesión está activa.
            </p>
            <p class="em-body"
               style="margin:0;color:#334155;font-size:15px;line-height:1.7;">
              <strong>¿No fuiste tú?</strong>&nbsp; Cambia tu contraseña de inmediato
              y contacta al administrador del sistema.
            </p>
            """.formatted(username, cajaInfo(tablaInfo));

        return base(
            "Nuevo acceso a tu cuenta — Costusoft",
            "Detectamos un nuevo inicio de sesión en tu cuenta de Costusoft.",
            cuerpo);
    }

    /**
     * Confirmación de cambio de contraseña exitoso.
     */
    public static String cambioPasswordConfirmado(String username, String fechaHora) {
        String cuerpo = """
            <h2 class="em-h2"
                style="margin:0 0 8px;color:#0f172a;font-size:22px;
                       font-weight:700;line-height:1.3;">
              Contraseña actualizada ✅
            </h2>
            <p class="em-lead"
               style="margin:0 0 24px;color:#64748b;font-size:14px;line-height:1.6;">
              Hola <strong class="em-strong" style="color:#1e293b;">%s</strong>,
              tu contraseña fue cambiada exitosamente.
            </p>

            %s

            %s

            <p class="em-body"
               style="margin:20px 0 0;color:#64748b;font-size:13px;line-height:1.6;">
              Si <strong>no realizaste este cambio</strong>, contacta al administrador
              del sistema de inmediato para proteger tu cuenta.
            </p>
            """.formatted(
                username,
                cajaInfo("📅&nbsp;<strong>Fecha y hora del cambio:</strong> " + fechaHora),
                divider());

        return base(
            "Contraseña actualizada — Costusoft",
            "Tu contraseña de Costusoft fue actualizada exitosamente.",
            cuerpo);
    }
}
