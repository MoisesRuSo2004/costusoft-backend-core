package com.costusoft.inventory_system.config;

import com.costusoft.inventory_system.entity.Usuario;
import com.costusoft.inventory_system.repo.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador de datos para el perfil de desarrollo.
 *
 * Crea los tres usuarios del sistema si no existen al arrancar la aplicación.
 * NUNCA ejecutar en producción — usa el perfil "dev" exclusivamente.
 *
 * Credenciales dev:
 *   admin   / admin123   → ADMIN  (acceso total)
 *   usuario / usuario123 → USER   (secretaria, crea solicitudes)
 *   bodega  / bodega123  → BODEGA (operador almacén, confirma/rechaza)
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        crearUsuarioSiNoExiste("admin",   "admin123",   "admin@costusoft.dev",   Usuario.Rol.ADMIN);
        crearUsuarioSiNoExiste("usuario", "usuario123", "usuario@costusoft.dev", Usuario.Rol.USER);
        crearUsuarioSiNoExiste("bodega",  "bodega123",  "bodega@costusoft.dev",  Usuario.Rol.BODEGA);
    }

    private void crearUsuarioSiNoExiste(String username, String password, String correo, Usuario.Rol rol) {
        if (usuarioRepository.existsByUsername(username)) {
            log.debug("DataInitializer — usuario '{}' ya existe, omitiendo.", username);
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        usuario.setActivo(true);

        usuarioRepository.save(usuario);
        log.info("DataInitializer — usuario '{}' creado con rol {}.", username, rol);
    }
}
