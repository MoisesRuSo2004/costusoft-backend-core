package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.EstadoPedido;
import com.costusoft.inventory_system.entity.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    // ── Conteos para dashboard ───────────────────────────────────────────

    long countByEstado(EstadoPedido estado);

    long countByColegioId(Long colegioId);

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
     * Carga el pedido + detalles + uniforme + insumosRequeridos + insumo de cada insumo.
     * Usar para: iniciarProduccion() — necesita el árbol completo para agregar insumos.
     */
    @Query("""
           SELECT DISTINCT p FROM Pedido p
           LEFT JOIN FETCH p.detalles d
           LEFT JOIN FETCH d.uniforme u
           LEFT JOIN FETCH u.insumosRequeridos ir
           LEFT JOIN FETCH ir.insumo
           LEFT JOIN FETCH p.colegio
           WHERE p.id = :id
           """)
    Optional<Pedido> findByIdWithDetallesAndInsumos(@Param("id") Long id);

    /**
     * Carga el pedido + detalles + uniforme + insumosRequeridos + insumo + salida.
     * Usar para: entregar() y respuesta completa incluyendo salida.
     */
    @Query("""
           SELECT DISTINCT p FROM Pedido p
           LEFT JOIN FETCH p.detalles d
           LEFT JOIN FETCH d.uniforme u
           LEFT JOIN FETCH u.insumosRequeridos ir
           LEFT JOIN FETCH ir.insumo
           LEFT JOIN FETCH p.colegio
           LEFT JOIN FETCH p.salida
           WHERE p.id = :id
           """)
    Optional<Pedido> findByIdFull(@Param("id") Long id);
}
