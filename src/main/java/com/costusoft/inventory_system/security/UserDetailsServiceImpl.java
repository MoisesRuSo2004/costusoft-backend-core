package com.costusoft.inventory_system.security;

import com.costusoft.inventory_system.entity.Usuario;
import com.costusoft.inventory_system.repo.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de UserDetailsService que carga el usuario desde PostgreSQL.
 *
 * Spring Security llama a loadUserByUsername durante la autenticación
 * para obtener el UserDetails con el que comparar credenciales.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Intento de login con usuario inexistente: {}", username);
                    // Mensaje genérico — no revelar si el usuario existe o no
                    return new UsernameNotFoundException("Credenciales inválidas");
                });

        // Verificar activacion ANTES que activo — mensaje mas especifico para el usuario
        if (!usuario.isCuentaActivada()) {
            log.warn("Intento de login con cuenta no activada: {}", username);
            throw new DisabledException(
                    "Tu cuenta aún no ha sido activada. Revisa tu correo electrónico y sigue el enlace de activación.");
        }

        if (!usuario.isActivo()) {
            log.warn("Intento de login con usuario inactivo: {}", username);
            throw new DisabledException("La cuenta está desactivada. Contacte al administrador.");
        }

        return UserDetailsImpl.build(usuario);
    }
}
