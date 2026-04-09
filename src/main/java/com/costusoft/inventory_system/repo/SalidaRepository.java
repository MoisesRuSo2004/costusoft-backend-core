package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.EstadoMovimiento;
import com.costusoft.inventory_system.entity.Salida;
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
public interface SalidaRepository extends JpaRepository<Salida, Long> {

    /** Paginado sin fetch (Spring resuelve count automáticamente) */
    Page<Salida> findAllByOrderByFechaDesc(Pageable pageable);

    /** Paginado filtrado por estado — bandeja de BODEGA */
    Page<Salida> findByEstadoOrderByFechaDesc(EstadoMovimiento estado, Pageable pageable);

    List<Salida> findByFechaBetween(LocalDate inicio, LocalDate fin);

    /** Para reportes — evita N+1 al acceder a detalles e insumos */
    @Query("""
                SELECT DISTINCT s FROM Salida s
                LEFT JOIN FETCH s.detalles d
                LEFT JOIN FETCH d.insumo
                LEFT JOIN FETCH s.colegio
                WHERE s.fecha BETWEEN :inicio AND :fin
            """)
    List<Salida> findByFechaBetweenWithDetalles(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    List<Salida> findByColegioIdAndFechaBetween(Long colegioId, LocalDate inicio, LocalDate fin);

    /** Para reportes filtrados por colegio — evita N+1 */
    @Query("""
                SELECT DISTINCT s FROM Salida s
                LEFT JOIN FETCH s.detalles d
                LEFT JOIN FETCH d.insumo
                LEFT JOIN FETCH s.colegio
                WHERE s.colegio.id = :colegioId AND s.fecha BETWEEN :inicio AND :fin
            """)
    List<Salida> findByColegioIdAndFechaBetweenWithDetalles(
            @Param("colegioId") Long colegioId,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);

    @Query("""
                SELECT s FROM Salida s
                LEFT JOIN FETCH s.detalles d
                LEFT JOIN FETCH d.insumo
                LEFT JOIN FETCH s.colegio
                WHERE s.id = :id
            """)
    Optional<Salida> findByIdWithDetalles(@Param("id") Long id);

    @Query("""
                SELECT DISTINCT s FROM Salida s
                LEFT JOIN FETCH s.detalles d
                LEFT JOIN FETCH d.insumo
                LEFT JOIN FETCH s.colegio
                ORDER BY s.fecha DESC
            """)
    List<Salida> findAllWithDetalles();

    long countByFechaBetween(LocalDate inicio, LocalDate fin);

    /** Para contadores del dashboard — cuántas salidas hay por estado */
    long countByEstado(EstadoMovimiento estado);
}
