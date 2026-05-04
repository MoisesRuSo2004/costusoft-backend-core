package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    Optional<Insumo> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /** Búsqueda por nombre — solo insumos activos (para autocomplete en entradas/salidas) */
    List<Insumo> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    /** Insumos activos cuyo stock es <= su stockMinimo */
    @Query("SELECT i FROM Insumo i WHERE i.stock <= i.stockMinimo AND i.activo = true")
    List<Insumo> findInsumosConStockBajo();

    /** Insumos con stock en cero */
    List<Insumo> findByStock(Integer stock);

    /** Insumos por tipo */
    List<Insumo> findByTipoIgnoreCase(String tipo);

    /** ¿El insumo tiene detalles en alguna entrada? */
    @Query("SELECT COUNT(de) > 0 FROM DetalleEntrada de WHERE de.insumo.id = :insumoId")
    boolean tieneDetallesEntrada(@Param("insumoId") Long insumoId);

    /** ¿El insumo tiene detalles en alguna salida? */
    @Query("SELECT COUNT(ds) > 0 FROM DetalleSalida ds WHERE ds.insumo.id = :insumoId")
    boolean tieneDetallesSalida(@Param("insumoId") Long insumoId);
}
