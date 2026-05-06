package com.costusoft.inventory_system.module.usuario.service;

import com.costusoft.inventory_system.module.usuario.dto.UsuarioDTO;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UsuarioService {

    /** Crea un nuevo usuario — solo ADMIN */
    UsuarioDTO.Response crear(UsuarioDTO.CreateRequest request);

    /** Lista todos los usuarios paginados — solo ADMIN */
    PageDTO<UsuarioDTO.Response> listar(Pageable pageable);

    /** Obtiene un usuario por ID — solo ADMIN */
    UsuarioDTO.Response obtenerPorId(Long id);

    /** Obtiene un usuario por username — disponible para todos los usuarios autenticados */
    UsuarioDTO.Response obtenerPorUsername(String username);

    /** Actualiza un usuario existente — solo ADMIN */
    UsuarioDTO.Response actualizar(Long id, UsuarioDTO.UpdateRequest request);

    /** Elimina un usuario — solo ADMIN, no puede eliminarse a si mismo */
    void eliminar(Long id);

    /**
     * Cambia la contrasena del usuario autenticado actualmente.
     * Valida la contrasena actual antes de cambiar.
     * Disponible para ADMIN y USER (sobre su propia cuenta).
     */
    void cambiarPassword(UsuarioDTO.ChangePasswordRequest request);

    /** Activa o desactiva un usuario — solo ADMIN */
    UsuarioDTO.Response toggleActivo(Long id);

    /**
     * Sube o reemplaza la foto de perfil del usuario autenticado.
     * Almacena la imagen en Cloudinary y persiste la URL en la entidad.
     */
    UsuarioDTO.Response subirFoto(MultipartFile archivo);

    /**
     * Elimina la foto de perfil del usuario autenticado de Cloudinary y la BD.
     */
    UsuarioDTO.Response eliminarFoto();
}
