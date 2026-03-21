package com.costusoft.inventory_system.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para la peticion de login.
 *
 * Las validaciones rechazan la request antes de llegar al service,
 * evitando consultas innecesarias a la base de datos.
 */
@Getter
@Setter
@NoArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
    private String username;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 3, max = 100, message = "La contrasena debe tener entre 3 y 100 caracteres")
    private String password;
}
