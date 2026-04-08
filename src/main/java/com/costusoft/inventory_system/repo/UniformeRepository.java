package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.Uniforme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UniformeRepository extends JpaRepository<Uniforme, Long> {

    List<Uniforme> findByColegioId(Long colegioId);

    boolean existsByPrendaIgnoreCaseAndColegioId(String prenda, Long colegioId);

    /** Trae el uniforme con sus insumos requeridos en un solo query (evita N+1) */
    @Query("""
                SELECT DISTINCT u FROM Uniforme u
                LEFT JOIN FETCH u.insumosRequeridos ir
                LEFT JOIN FETCH ir.insumo
                WHERE u.colegio.id = :colegioId
            """)
    List<Uniforme> findByColegioIdWithInsumos(@Param("colegioId") Long colegioId);

    @Query("""
                SELECT DISTINCT u FROM Uniforme u
                LEFT JOIN FETCH u.insumosRequeridos ir
                LEFT JOIN FETCH ir.insumo
                WHERE u.id = :id
            """)
    java.util.Optional<Uniforme> findByIdWithInsumos(@Param("id") Long id);

    /**
     * Carga múltiples uniformes con sus insumos en un solo query — evita N+1
     * en el cálculo de pedidos multi-prenda.
     */
    @Query("""
                SELECT DISTINCT u FROM Uniforme u
                LEFT JOIN FETCH u.insumosRequeridos ir
                LEFT JOIN FETCH ir.insumo
                WHERE u.id IN :ids
            """)
    List<Uniforme> findByIdInWithInsumos(@Param("ids") List<Long> ids);
}
