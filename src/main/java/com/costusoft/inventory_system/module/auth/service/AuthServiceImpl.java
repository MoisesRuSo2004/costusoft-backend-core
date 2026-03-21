package com.costusoft.inventory_system.module.auth.service;

import com.costusoft.inventory_system.entity.Usuario;
import com.costusoft.inventory_system.repo.UsuarioRepository;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.auth.dto.AuthResponseDTO;
import com.costusoft.inventory_system.module.auth.dto.LoginRequestDTO;
import com.costusoft.inventory_system.module.auth.dto.RefreshTokenRequestDTO;
import com.costusoft.inventory_system.security.JwtTokenProvider;
import com.costusoft.inventory_system.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion del servicio de autenticacion.
 *
 * Flujo de login:
 * 1. AuthenticationManager valida username + password contra la BD
 * 2. Si es valido, genera access token + refresh token
 * 3. Retorna AuthResponseDTO con tokens e info del usuario
 *
 * Flujo de refresh:
 * 1. Valida que el refresh token no este expirado ni malformado
 * 2. Extrae el username del token
 * 3. Genera un nuevo par de tokens
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Login ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        // 1. Autenticar — lanza BadCredentialsException o DisabledException si falla
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername().trim(),
                        request.getPassword()));

        // 2. Setear en el SecurityContext para el contexto actual
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Generar tokens
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getUsername());

        log.info("Login exitoso para usuario: {}", userDetails.getUsername());

        // 4. Construir y retornar respuesta
        return buildAuthResponse(accessToken, refreshToken, userDetails);
    }

    // ── Refresh ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO refresh(RefreshTokenRequestDTO request) {
        String refreshToken = request.getRefreshToken();

        // 1. Validar el refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("El refresh token es invalido o ha expirado. Inicie sesion nuevamente.");
        }

        // 2. Extraer username del token
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // 3. Verificar que el usuario siga existiendo y activo
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "username", username));

        if (!usuario.isActivo()) {
            throw new BusinessException("La cuenta esta desactivada. Contacte al administrador.");
        }

        // 4. Generar nuevo par de tokens
        UserDetailsImpl userDetails = UserDetailsImpl.build(usuario);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        log.info("Tokens renovados para usuario: {}", username);

        return buildAuthResponse(newAccessToken, newRefreshToken, userDetails);
    }

    // ── Me ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO.UsuarioInfoDTO me() {
        // Extrae el usuario del SecurityContext (ya autenticado via JWT)
        UserDetailsImpl userDetails = getCurrentUser();

        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", "username", userDetails.getUsername()));

        return buildUsuarioInfo(usuario);
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private AuthResponseDTO buildAuthResponse(
            String accessToken,
            String refreshToken,
            UserDetailsImpl userDetails) {

        Usuario usuario = usuarioRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", "username", userDetails.getUsername()));

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs)
                .usuario(buildUsuarioInfo(usuario))
                .build();
    }

    private AuthResponseDTO.UsuarioInfoDTO buildUsuarioInfo(Usuario usuario) {
        return AuthResponseDTO.UsuarioInfoDTO.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol().name())
                .build();
    }

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("No hay sesion activa");
        }
        return (UserDetailsImpl) authentication.getPrincipal();
    }
}