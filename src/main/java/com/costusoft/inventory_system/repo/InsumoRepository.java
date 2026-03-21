package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    Optional<Insumo> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Insumo> findByNombreContainingIgnoreCase(String nombre);

    /** Insumos cuyo stock es <= su stockMinimo */
    @Query("SELECT i FROM Insumo i WHERE i.stock <= i.stockMinimo")
    List<Insumo> findInsumosConStockBajo();

    /** Insumos con stock en cero */
    List<Insumo> findByStock(Integer stock);

    /** Insumos por tipo */
    List<Insumo> findByTipoIgnoreCase(String tipo);
}
