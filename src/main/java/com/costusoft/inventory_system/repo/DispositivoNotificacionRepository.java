package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.DispositivoNotificacion;
import com.costusoft.inventory_system.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DispositivoNotificacionRepository extends JpaRepository<DispositivoNotificacion, Long> {

    Optional<DispositivoNotificacion> findByToken(String token);

    /** Tokens de los usuarios activos de un colegio con el rol indicado (los coordinadores). */
    @Query("""
            select d.token from DispositivoNotificacion d
            where d.usuario.colegio.id = :colegioId
              and d.usuario.rol = :rol
              and d.usuario.activo = true
            """)
    List<String> tokensDeColegio(@Param("colegioId") Long colegioId, @Param("rol") Usuario.Rol rol);

    /** Tokens de todos los usuarios activos con el rol indicado (p. ej. Bodega). */
    @Query("""
            select d.token from DispositivoNotificacion d
            where d.usuario.rol = :rol
              and d.usuario.activo = true
            """)
    List<String> tokensDeRol(@Param("rol") Usuario.Rol rol);

    /** Tokens del usuario (para el aviso de prueba). */
    @Query("select d.token from DispositivoNotificacion d where d.usuario.username = :username")
    List<String> tokensDeUsuario(@Param("username") String username);

    /** Limpia tokens que Firebase reporto como vencidos o invalidos. */
    @Transactional
    @Modifying
    @Query("delete from DispositivoNotificacion d where d.token in :tokens")
    int eliminarTokens(@Param("tokens") Collection<String> tokens);
}
