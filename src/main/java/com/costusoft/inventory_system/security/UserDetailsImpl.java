package com.costusoft.inventory_system.security;

import com.costusoft.inventory_system.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Implementacion de UserDetails que envuelve la entidad Usuario.
 *
 * Spring Security trabaja con esta clase internamente.
 * Se construye desde UserDetailsServiceImpl al autenticar.
 *
 * colegioId: solo presente para usuarios con rol INSTITUCION.
 * Permite que los controllers/services del portal institucional
 * obtengan el colegio del usuario autenticado directamente del
 * SecurityContext sin tocar la BD.
 */
@Getter
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String correo;
    private final boolean activo;

    /**
     * ID del colegio al que pertenece el usuario.
     * Siempre disponible para rol INSTITUCION.
     * null para ADMIN, USER y BODEGA.
     */
    private final Long colegioId;

    private final Collection<? extends GrantedAuthority> authorities;

    private UserDetailsImpl(
            Long id,
            String username,
            String password,
            String correo,
            boolean activo,
            Long colegioId,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.correo = correo;
        this.activo = activo;
        this.colegioId = colegioId;
        this.authorities = authorities;
    }

    /**
     * Factory method — construye UserDetailsImpl desde la entidad Usuario.
     * El rol se convierte a ROLE_ADMIN, ROLE_USER, ROLE_BODEGA o ROLE_INSTITUCION
     * (convencion Spring Security).
     */
    public static UserDetailsImpl build(Usuario usuario) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));

        Long colegioId = (usuario.getColegio() != null)
                ? usuario.getColegio().getId()
                : null;

        return new UserDetailsImpl(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.getCorreo(),
                usuario.isActivo(),
                colegioId,
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

    /** La cuenta no expira en esta version */
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
