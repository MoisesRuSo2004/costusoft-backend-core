package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.LoginAudit;
import com.costusoft.inventory_system.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

    /**
     * Verifica si un usuario ya inicio sesion antes desde este device (hash).
     * Si NO existe → dispositivo nuevo → enviar notificacion.
     */
    boolean existsByUsuarioAndDeviceHashAndExitoso(
            Usuario usuario, String deviceHash, boolean exitoso);

    /** Los 10 ultimos logins exitosos del usuario para historial */
    @Query("SELECT la FROM LoginAudit la WHERE la.usuario = :usuario AND la.exitoso = true " +
           "ORDER BY la.fechaLogin DESC")
    java.util.List<LoginAudit> findTop10ByUsuarioExitoso(@Param("usuario") Usuario usuario,
            org.springframework.data.domain.Pageable pageable);

    /** Limpieza: elimina registros mas antiguos de N dias */
    @Modifying
    @Query("DELETE FROM LoginAudit la WHERE la.fechaLogin < :fecha")
    void deleteOlderThan(@Param("fecha") LocalDateTime fecha);
}
