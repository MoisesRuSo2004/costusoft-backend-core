package com.costusoft.inventory_system.config;

import com.costusoft.inventory_system.entity.Colegio;
import com.costusoft.inventory_system.entity.Usuario;
import com.costusoft.inventory_system.repo.ColegioRepository;
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
 * Crea los usuarios del sistema si no existen al arrancar la aplicación.
 * NUNCA ejecutar en producción — usa el perfil "dev" exclusivamente.
 *
 * Credenciales dev:
 *   admin       / admin123       → ADMIN      (acceso total)
 *   usuario     / usuario123     → USER       (secretaria, crea solicitudes)
 *   bodega      / bodega123      → BODEGA     (operador almacén, confirma/rechaza)
 *   coordinador / coordinador123 → INSTITUCION (portal institucional – Colegio Demo)
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ColegioRepository colegioRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Nombre del colegio demo para el coordinador de prueba ────────────
    private static final String COLEGIO_DEMO = "Colegio Demo Dev";

    @Override
    public void run(String... args) {
        // Usuarios del equipo Costusoft (sin colegio)
        crearUsuario("admin",   "admin123",       "admin@costusoft.dev",       Usuario.Rol.ADMIN,       null);
        crearUsuario("usuario", "usuario123",     "usuario@costusoft.dev",     Usuario.Rol.USER,        null);
        crearUsuario("bodega",  "bodega123",      "bodega@costusoft.dev",      Usuario.Rol.BODEGA,      null);

        // Usuario institucional: necesita un colegio asignado
        Colegio colegioDemo = obtenerOCrearColegioDemo();
        crearUsuario("coordinador", "coordinador123", "coordinador@demo.edu.co",
                     Usuario.Rol.INSTITUCION, colegioDemo);
    }

    // ── Colegio demo ─────────────────────────────────────────────────────

    private Colegio obtenerOCrearColegioDemo() {
        return colegioRepository.findByNombreIgnoreCase(COLEGIO_DEMO)
                .orElseGet(() -> {
                    Colegio colegio = new Colegio();
                    colegio.setNombre(COLEGIO_DEMO);
                    colegio.setDireccion("Calle 123 # 45-67, Bogotá, Colombia");
                    Colegio guardado = colegioRepository.save(colegio);
                    log.info("DataInitializer — colegio '{}' creado (id: {}).",
                            COLEGIO_DEMO, guardado.getId());
                    return guardado;
                });
    }

    // ── Creación de usuario ──────────────────────────────────────────────

    /**
     * Crea un usuario si su username no existe todavía.
     *
     * @param colegio null para roles internos (ADMIN, USER, BODEGA);
     *                requerido para INSTITUCION.
     */
    private void crearUsuario(String username, String password, String correo,
                              Usuario.Rol rol, Colegio colegio) {

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
        // CRÍTICO: cuentaActivada=true → permite login directo en dev.
        // En producción los usuarios activan su cuenta vía email.
        usuario.setCuentaActivada(true);
        usuario.setColegio(colegio); // null para roles sin portal institucional

        usuarioRepository.save(usuario);
        log.info("DataInitializer — usuario '{}' creado con rol {}{}.",
                username, rol,
                colegio != null ? " | colegio: " + colegio.getNombre() : "");
    }
}
