package com.costusoft.inventory_system.repo;

// ─────────────────────────────────────────────────────────────
// ColegioRepository.java
// ─────────────────────────────────────────────────────────────
import com.costusoft.inventory_system.entity.Colegio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColegioRepository extends JpaRepository<Colegio, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /** Busca un colegio por nombre exacto (case-insensitive). */
    Optional<Colegio> findByNombreIgnoreCase(String nombre);
}
