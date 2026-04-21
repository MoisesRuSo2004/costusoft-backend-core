package com.costusoft.inventory_system.module.seguridad.email;

import com.costusoft.inventory_system.entity.Usuario;

/**
 * Contrato del servicio de correo transaccional.
 *
 * Todos los metodos son fire-and-forget (asincrono con @Async).
 * Si el envio falla, se loguea el error pero NO se propaga al caller.
 */
public interface EmailService {

    /**
     * Envia el correo de bienvenida con el link para establecer la contrasena inicial.
     * Link expira en 24 horas.
     */
    void enviarActivacionCuenta(Usuario usuario, String token, String frontendUrl);

    /**
     * Envia el link de recuperacion de contrasena.
     * Link expira en 15 minutos y es de un solo uso.
     */
    void enviarRecuperacionPassword(Usuario usuario, String token, String frontendUrl);

    /**
     * Notifica al usuario que se detecto un inicio de sesion desde un
     * dispositivo / IP que no habia usado antes.
     */
    void enviarNotificacionNuevoDispositivo(
            Usuario usuario,
            String ipAddress,
            String userAgent,
            String fechaHora);

    /**
     * Confirma al usuario que su contrasena fue cambiada exitosamente.
     * Si el no lo hizo, debe contactar al administrador de inmediato.
     */
    void enviarConfirmacionCambioPassword(Usuario usuario, String fechaHora);
}
