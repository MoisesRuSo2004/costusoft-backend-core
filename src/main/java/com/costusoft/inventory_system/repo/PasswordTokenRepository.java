package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.PasswordToken;
import com.costusoft.inventory_system.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordTokenRepository extends JpaRepository<PasswordToken, Long> {

    /** Busca un token activo (no usado) por su valor */
    Optional<PasswordToken> findByTokenAndUsadoFalse(String token);

    /** Busca cualquier token por valor (para auditoría) */
    Optional<PasswordToken> findByToken(String token);

    /** Elimina todos los tokens previos del mismo tipo para un usuario
     *  antes de generar uno nuevo — evita tokens huerfanos */
    @Modifying
    @Query("DELETE FROM PasswordToken pt WHERE pt.usuario = :usuario AND pt.tipo = :tipo")
    void deleteByUsuarioAndTipo(
            @Param("usuario") Usuario usuario,
            @Param("tipo") PasswordToken.TipoToken tipo);

    /** Limpieza periodica de tokens expirados */
    @Modifying
    @Query("DELETE FROM PasswordToken pt WHERE pt.expiresAt < :ahora")
    void deleteExpiredTokens(@Param("ahora") LocalDateTime ahora);
}
