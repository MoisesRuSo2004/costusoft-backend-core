package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.Entrada;
import com.costusoft.inventory_system.entity.EstadoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntradaRepository extends JpaRepository<Entrada, Long> {

    /** Fetch con detalles e insumos — evita N+1 en listados completos */
    @Query("""
                SELECT DISTINCT e FROM Entrada e
                LEFT JOIN FETCH e.detalles d
                LEFT JOIN FETCH d.insumo
                LEFT JOIN FETCH e.proveedor
                ORDER BY e.fecha DESC
            """)
    List<Entrada> findAllWithDetalles();

    /** Paginado sin fetch (Spring resuelve count automáticamente) */
    Page<Entrada> findAllByOrderByFechaDesc(Pageable pageable);

    /** Paginado filtrado por estado — bandeja de BODEGA */
    Page<Entrada> findByEstadoOrderByFechaDesc(EstadoMovimiento estado, Pageable pageable);

    /** Para el módulo de predicción y reportes */
    List<Entrada> findByFechaBetween(LocalDate inicio, LocalDate fin);

    /** Para reportes filtrados por proveedor */
    List<Entrada> findByProveedorIdAndFechaBetween(Long proveedorId, LocalDate inicio, LocalDate fin);

    /** Fetch individual con detalles */
    @Query("""
                SELECT e FROM Entrada e
                LEFT JOIN FETCH e.detalles d
                LEFT JOIN FETCH d.insumo
                LEFT JOIN FETCH e.proveedor
                WHERE e.id = :id
            """)
    Optional<Entrada> findByIdWithDetalles(@Param("id") Long id);

    long countByFechaBetween(LocalDate inicio, LocalDate fin);

    /** Para contadores del dashboard — cuántas entradas hay por estado */
    long countByEstado(EstadoMovimiento estado);
}
