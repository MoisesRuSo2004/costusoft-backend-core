package com.costusoft.inventory_system.repo;

// ─────────────────────────────────────────────────────────────
// UsuarioRepository.java
// ─────────────────────────────────────────────────────────────

import com.costusoft.inventory_system.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByCorreo(String correo);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByCorreoAndIdNot(String correo, Long id);
}