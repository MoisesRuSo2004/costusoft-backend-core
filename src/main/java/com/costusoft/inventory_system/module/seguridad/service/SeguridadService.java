package com.costusoft.inventory_system.module.seguridad.service;

import com.costusoft.inventory_system.entity.Usuario;
import com.costusoft.inventory_system.module.seguridad.dto.SeguridadDTO;

/**
 * Contrato del servicio de seguridad de cuentas.
 *
 * Responsabilidades:
 * - Generar y validar tokens de activacion / recuperacion
 * - Orquestar el envio de emails de seguridad
 * - Registrar y analizar el historial de logins
 */
public interface SeguridadService {

    /**
     * Genera un token de activacion para el usuario y envia el email de bienvenida.
     * Llamado por UsuarioServiceImpl al crear un usuario nuevo.
     */
    void generarYEnviarActivacion(Usuario usuario);

    /**
     * Genera un token de recuperacion y envia el email.
     * Llamado cuando el usuario olvido su contrasena.
     * Si el correo no existe, no lanza error (evita enumeracion de usuarios).
     */
    void solicitarRecuperacionPassword(SeguridadDTO.ForgotPasswordRequest request);

    /**
     * Valida el token de activacion y establece la contrasena inicial.
     * Marca la cuenta como activada.
     */
    void activarCuenta(SeguridadDTO.SetPasswordRequest request);

    /**
     * Valida el token de recuperacion y reemplaza la contrasena.
     */
    void resetearPassword(SeguridadDTO.ResetPasswordRequest request);

    /**
     * Registra el login en el historial.
     * Si el dispositivo es nuevo, envia notificacion al usuario de forma asincrona.
     */
    void procesarLoginAudit(Usuario usuario, String ipAddress, String userAgent, boolean exitoso);

    /**
     * Envia el email de confirmacion despues de un cambio de contrasena exitoso.
     */
    void notificarCambioPassword(Usuario usuario);
}
