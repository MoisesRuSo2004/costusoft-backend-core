package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.Uniforme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniformeRepository extends JpaRepository<Uniforme, Long> {

    List<Uniforme> findByColegioId(Long colegioId);

    /**
     * Valida unicidad de (prenda, tipo, genero, colegio).
     * Permite mismo nombre de prenda si difiere en tipo o género.
     */
    boolean existsByPrendaIgnoreCaseAndTipoIgnoreCaseAndGeneroIgnoreCaseAndColegioId(
            String prenda, String tipo, String genero, Long colegioId);

    // ── Queries con JOIN FETCH (evitan N+1) ──────────────────────────────

    /** Prendas de un colegio con todos sus insumos/tallas cargados */
    @Query("""
                SELECT DISTINCT u FROM Uniforme u
                LEFT JOIN FETCH u.insumosRequeridos ir
                LEFT JOIN FETCH ir.insumo
                WHERE u.colegio.id = :colegioId
                ORDER BY u.tipo, u.genero, u.prenda
            """)
    List<Uniforme> findByColegioIdWithInsumos(@Param("colegioId") Long colegioId);

    /** Una prenda con todos sus insumos/tallas */
    @Query("""
                SELECT DISTINCT u FROM Uniforme u
                LEFT JOIN FETCH u.insumosRequeridos ir
                LEFT JOIN FETCH ir.insumo
                WHERE u.id = :id
            """)
    Optional<Uniforme> findByIdWithInsumos(@Param("id") Long id);

    /** Batch de prendas con insumos — evita N+1 en calculadora multi-prenda */
    @Query("""
                SELECT DISTINCT u FROM Uniforme u
                LEFT JOIN FETCH u.insumosRequeridos ir
                LEFT JOIN FETCH ir.insumo
                WHERE u.id IN :ids
            """)
    List<Uniforme> findByIdInWithInsumos(@Param("ids") List<Long> ids);

    // ── Tallas disponibles para una prenda ───────────────────────────────

    /**
     * Retorna las tallas configuradas para una prenda (distintas y ordenadas).
     * Útil para poblar el dropdown de talla en el frontend.
     *
     * Ejemplo: ["06-08", "10-12", "14-16"] o ["S", "M", "L", "XL"]
     */
    @Query("""
                SELECT DISTINCT ir.talla FROM Uniforme u
                JOIN u.insumosRequeridos ir
                WHERE u.id = :uniformeId
                ORDER BY ir.talla
            """)
    List<String> findDistinctTallasByUniformeId(@Param("uniformeId") Long uniformeId);
}
