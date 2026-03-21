package com.costusoft.inventory_system.module.auth.service;

import com.costusoft.inventory_system.module.auth.dto.AuthResponseDTO;
import com.costusoft.inventory_system.module.auth.dto.LoginRequestDTO;
import com.costusoft.inventory_system.module.auth.dto.RefreshTokenRequestDTO;

/**
 * Contrato del servicio de autenticacion.
 *
 * La interfaz desacopla el controller de la implementacion,
 * facilitando tests unitarios con mocks.
 */
public interface AuthService {

    /**
     * Autentica al usuario con username y password.
     * Retorna un par access + refresh token si las credenciales son correctas.
     *
     * @throws org.springframework.security.authentication.BadCredentialsException si
     *                                                                             las
     *                                                                             credenciales
     *                                                                             son
     *                                                                             incorrectas
     * @throws org.springframework.security.authentication.DisabledException       si
     *                                                                             el
     *                                                                             usuario
     *                                                                             esta
     *                                                                             inactivo
     */
    AuthResponseDTO login(LoginRequestDTO request);

    /**
     * Emite un nuevo access token a partir de un refresh token valido.
     *
     * @throws com.inventario.api.exception.BusinessException si el refresh token es
     *                                                        invalido o expiro
     */
    AuthResponseDTO refresh(RefreshTokenRequestDTO request);

    /**
     * Retorna los datos del usuario autenticado actualmente (del SecurityContext).
     */
    AuthResponseDTO.UsuarioInfoDTO me();
}
