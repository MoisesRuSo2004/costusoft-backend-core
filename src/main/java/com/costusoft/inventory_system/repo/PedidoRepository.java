package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.EstadoPedido;
import com.costusoft.inventory_system.entity.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // ── Paginado general ─────────────────────────────────────────────────

    Page<Pedido> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ── Filtros ──────────────────────────────────────────────────────────

    Page<Pedido> findByEstadoOrderByCreatedAtDesc(EstadoPedido estado, Pageable pageable);

    Page<Pedido> findByColegioIdOrderByCreatedAtDesc(Long colegioId, Pageable pageable);

    Page<Pedido> findByEstadoAndColegioIdOrderByCreatedAtDesc(
            EstadoPedido estado, Long colegioId, Pageable pageable);

    // ── Reportes ─────────────────────────────────────────────────────────

    /**
     * Pedidos creados en un rango de fechas con colegio y detalles prefetcheados.
     * Evita N+1 al acceder a p.getColegio() y p.getDetalles() en el servicio de reportes.
     */
    @Query("""
           SELECT DISTINCT p FROM Pedido p
           LEFT JOIN FETCH p.colegio
           LEFT JOIN FETCH p.detalles
           WHERE p.createdAt >= :inicio AND p.createdAt <= :fin
           ORDER BY p.createdAt DESC
           """)
    List<Pedido> findByCreatedAtBetweenWithColegioAndDetalles(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // ── Conteos para dashboard ───────────────────────────────────────────

    long countByEstado(EstadoPedido estado);

    long countByColegioId(Long colegioId);

    /**
     * Cuenta pedidos de un colegio que NO estén en los estados indicados.
     * Usado en el dashboard institucional para calcular "pedidos activos"
     * sin cargar todas las entidades (evita N+1).
     *
     * Ejemplo: countByColegioIdAndEstadoNotIn(id, List.of(ENTREGADO, CANCELADO))
     */
    long countByColegioIdAndEstadoNotIn(Long colegioId, java.util.List<EstadoPedido> estados);

    // ── Carga con detalles (evita N+1 en operaciones individuales) ───────

    /**
     * Carga el pedido + sus detalles + el uniforme de cada detalle.
     * Usar para: calcular(), confirmar(), cancelar(), obtenerPorId() lista.
     */
    @Query("""
           SELECT DISTINCT p FROM Pedido p
           LEFT JOIN FETCH p.detalles d
           LEFT JOIN FETCH d.uniforme
           LEFT JOIN FETCH p.colegio
           WHERE p.id = :id
           """)
    Optional<Pedido> findByIdWithDetalles(@Param("id") Long id);

    /**
     * Carga el pedido + detalles + uniforme + colegio.
     * insumosRequeridos se carga via SUBSELECT por @Fetch(FetchMode.SUBSELECT) en Uniforme.
     * Usar para: iniciarProduccion() — dentro de @Transactional, la sesión está activa.
     */
    @Query("""
           SELECT DISTINCT p FROM Pedido p
           LEFT JOIN FETCH p.detalles d
           LEFT JOIN FETCH d.uniforme u
           LEFT JOIN FETCH p.colegio
           WHERE p.id = :id
           """)
    Optional<Pedido> findByIdWithDetallesAndInsumos(@Param("id") Long id);

    /**
     * Carga el pedido + detalles + uniforme + colegio + salida.
     * insumosRequeridos se carga via SUBSELECT por @Fetch(FetchMode.SUBSELECT) en Uniforme.
     * Usar para: entregar() y respuesta completa incluyendo salida.
     */
    @Query("""
           SELECT DISTINCT p FROM Pedido p
           LEFT JOIN FETCH p.detalles d
           LEFT JOIN FETCH d.uniforme u
           LEFT JOIN FETCH p.colegio
           LEFT JOIN FETCH p.salida
           WHERE p.id = :id
           """)
    Optional<Pedido> findByIdFull(@Param("id") Long id);
}
