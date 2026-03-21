package com.costusoft.inventory_system.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para renovar el access token usando el refresh token.
 *
 * El cliente envia el refreshToken cuando el accessToken expira.
 * Si el refreshToken es valido, se emite un nuevo par de tokens.
 */
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenRequestDTO {

    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
