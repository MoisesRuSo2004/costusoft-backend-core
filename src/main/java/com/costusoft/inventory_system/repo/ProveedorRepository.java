package com.costusoft.inventory_system.repo;

// ─────────────────────────────────────────────────────────────
// ProveedorRepository.java
// ─────────────────────────────────────────────────────────────
import com.costusoft.inventory_system.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNit(String nit);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    boolean existsByNitAndIdNot(String nit, Long id);
}
