package com.costusoft.inventory_system.repo;

// ─────────────────────────────────────────────────────────────
// ColegioRepository.java
// ─────────────────────────────────────────────────────────────
import com.costusoft.inventory_system.entity.Colegio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ColegioRepository extends JpaRepository<Colegio, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
