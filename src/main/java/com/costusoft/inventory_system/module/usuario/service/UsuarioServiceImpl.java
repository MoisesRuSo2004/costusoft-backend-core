package com.costusoft.inventory_system.module.usuario.service;

import com.costusoft.inventory_system.entity.Colegio;
import com.costusoft.inventory_system.entity.Usuario;
import com.costusoft.inventory_system.repo.ColegioRepository;
import com.costusoft.inventory_system.repo.UsuarioRepository;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.seguridad.service.SeguridadService;
import com.costusoft.inventory_system.module.usuario.dto.UsuarioDTO;
import com.costusoft.inventory_system.module.usuario.mapper.UsuarioMapper;
import com.costusoft.inventory_system.security.UserDetailsImpl;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Implementacion del servicio de usuarios.
 *
 * Reglas de seguridad:
 * - El password NUNCA viaja en texto plano fuera de este service
 * - El admin no puede eliminarse a si mismo
 * - cambiarPassword valida el password actual antes de modificar
 * - El username solo acepta caracteres alfanumericos + ._-
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ColegioRepository colegioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final SeguridadService seguridadService;

    // ── Crear ────────────────────────────────────────────────────────────

    @Override
    public UsuarioDTO.Response crear(UsuarioDTO.CreateRequest request) {
        validarUsernameUnico(request.getUsername(), null);
        validarCorreoUnico(request.getCorreo(), null);

        // Validar regla de negocio: INSTITUCION requiere un colegio asociado
        if (request.getRol() == Usuario.Rol.INSTITUCION) {
            if (request.getColegioId() == null) {
                throw new BusinessException(
                        "Los usuarios con rol INSTITUCION deben tener un colegio asignado.");
            }
        }

        Usuario usuario = usuarioMapper.toEntity(request);

        // Resolver colegio si aplica
        if (request.getRol() == Usuario.Rol.INSTITUCION && request.getColegioId() != null) {
            Colegio colegio = colegioRepository.findById(request.getColegioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Colegio", request.getColegioId()));
            usuario.setColegio(colegio);
        }

        // Contrasena inutilizable — el usuario la definira via el link de activacion.
        // Un hash BCrypt de un UUID aleatorio nunca puede ser adivinado ni revertido.
        usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setCuentaActivada(false);

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario creado — id: {} | username: '{}' | rol: {} | pendiente activacion",
                guardado.getId(), guardado.getUsername(), guardado.getRol());

        // Genera token de activacion + envia email de bienvenida (async)
        seguridadService.generarYEnviarActivacion(guardado);

        return usuarioMapper.toResponse(guardado);
    }

    // ── Listar ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageDTO<UsuarioDTO.Response> listar(Pageable pageable) {
        return PageDTO.from(usuarioRepository.findAll(pageable), usuarioMapper::toResponse);
    }

    // ── Obtener por ID ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO.Response obtenerPorId(Long id) {
        return usuarioMapper.toResponse(findOrThrow(id));
    }

    @Override
    public UsuarioDTO.Response obtenerPorUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "username", username));
        return usuarioMapper.toResponse(usuario);
    }

    // ── Actualizar ───────────────────────────────────────────────────────

    @Override
    public UsuarioDTO.Response actualizar(Long id, UsuarioDTO.UpdateRequest request) {
        Usuario usuario = findOrThrow(id);

        validarUsernameUnico(request.getUsername(), id);
        validarCorreoUnico(request.getCorreo(), id);

        // Actualizar campos basicos
        usuario.setUsername(request.getUsername());
        usuario.setCorreo(request.getCorreo());
        usuario.setRol(request.getRol());
        usuario.setActivo(request.isActivo());

        // Password: solo actualizar si viene con valor
        if (StringUtils.hasText(request.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
            log.info("Password actualizado para usuario id: {}", id);
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        log.info("Usuario actualizado — id: {} | username: '{}'", id, actualizado.getUsername());

        return usuarioMapper.toResponse(actualizado);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────

    @Override
    public void eliminar(Long id) {
        Usuario usuario = findOrThrow(id);

        // Proteccion: el admin no puede eliminarse a si mismo
        UserDetailsImpl currentUser = getCurrentUser();
        if (currentUser.getId().equals(id)) {
            throw new BusinessException("No puedes eliminar tu propia cuenta.");
        }

        usuarioRepository.delete(usuario);
        log.info("Usuario eliminado — id: {} | username: '{}'", id, usuario.getUsername());
    }

    // ── Cambiar password ─────────────────────────────────────────────────

    @Override
    public void cambiarPassword(UsuarioDTO.ChangePasswordRequest request) {
        // Validar que nueva password y confirmacion coinciden
        if (!request.getPasswordNueva().equals(request.getPasswordConfirmacion())) {
            throw new BusinessException("La nueva contrasena y su confirmacion no coinciden.");
        }

        // Obtener usuario autenticado
        UserDetailsImpl currentUser = getCurrentUser();
        Usuario usuario = usuarioRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", "username", currentUser.getUsername()));

        // Validar password actual
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            throw new BadCredentialsException("La contrasena actual es incorrecta.");
        }

        // Validar que la nueva no sea igual a la actual
        if (passwordEncoder.matches(request.getPasswordNueva(), usuario.getPassword())) {
            throw new BusinessException("La nueva contrasena debe ser diferente a la actual.");
        }

        usuario.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        usuarioRepository.save(usuario);

        log.info("Password cambiado exitosamente para usuario: '{}'", usuario.getUsername());

        // Notificar al usuario por email que su contrasena fue cambiada (async)
        seguridadService.notificarCambioPassword(usuario);
    }

    // ── Toggle activo ────────────────────────────────────────────────────

    @Override
    public UsuarioDTO.Response toggleActivo(Long id) {
        Usuario usuario = findOrThrow(id);

        // No desactivar la propia cuenta
        UserDetailsImpl currentUser = getCurrentUser();
        if (currentUser.getId().equals(id)) {
            throw new BusinessException("No puedes desactivar tu propia cuenta.");
        }

        usuario.setActivo(!usuario.isActivo());
        Usuario actualizado = usuarioRepository.save(usuario);

        log.info("Usuario '{}' — activo: {}", actualizado.getUsername(), actualizado.isActivo());
        return usuarioMapper.toResponse(actualizado);
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private Usuario findOrThrow(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    private void validarUsernameUnico(String username, Long idExcluido) {
        boolean existe = (idExcluido == null)
                ? usuarioRepository.existsByUsername(username)
                : usuarioRepository.existsByUsernameAndIdNot(username, idExcluido);

        if (existe) {
            throw new BusinessException(
                    "El username '" + username + "' ya esta en uso.");
        }
    }

    private void validarCorreoUnico(String correo, Long idExcluido) {
        boolean existe = (idExcluido == null)
                ? usuarioRepository.existsByCorreo(correo)
                : usuarioRepository.existsByCorreoAndIdNot(correo, idExcluido);

        if (existe) {
            throw new BusinessException(
                    "El correo '" + correo + "' ya esta registrado.");
        }
    }

    private UserDetailsImpl getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsImpl) auth.getPrincipal();
    }
}
