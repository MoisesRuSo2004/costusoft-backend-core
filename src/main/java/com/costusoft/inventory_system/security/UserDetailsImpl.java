package com.costusoft.inventory_system.security;

import com.costusoft.inventory_system.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Implementación de UserDetails que envuelve la entidad Usuario.
 *
 * Spring Security trabaja con esta clase internamente.
 * Se construye desde UserDetailsServiceImpl al autenticar.
 */
@Getter
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String correo;
    private final boolean activo;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserDetailsImpl(
            Long id,
            String username,
            String password,
            String correo,
            boolean activo,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.correo = correo;
        this.activo = activo;
        this.authorities = authorities;
    }

    /**
     * Factory method — construye UserDetailsImpl desde la entidad Usuario.
     * El rol se convierte a ROLE_ADMIN o ROLE_USER (convención Spring Security).
     */
    public static UserDetailsImpl build(Usuario usuario) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));

        return new UserDetailsImpl(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.getCorreo(),
                usuario.isActivo(),
                authorities);
    }

    // ── UserDetails contract ────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /** La cuenta no expira en esta versión */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** La cuenta no se bloquea por intentos fallidos (por ahora) */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** Las credenciales no expiran */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Usa el flag activo de la entidad Usuario */
    @Override
    public boolean isEnabled() {
        return activo;
    }
}
