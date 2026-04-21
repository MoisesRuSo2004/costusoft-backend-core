package com.costusoft.inventory_system.repo;

import com.costusoft.inventory_system.entity.Colegio;
import com.costusoft.inventory_system.entity.SolicitudEspecial;
import com.costusoft.inventory_system.entity.SolicitudEspecial.EstadoSolicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudEspecialRepository extends JpaRepository<SolicitudEspecial, Long> {

    /** Solicitudes de un colegio especifico — paginado */
    Page<SolicitudEspecial> findByColegioOrderByCreatedAtDesc(Colegio colegio, Pageable pageable);

    /** Solicitudes de un colegio filtradas por estado */
    Page<SolicitudEspecial> findByColegioAndEstadoOrderByCreatedAtDesc(
            Colegio colegio, EstadoSolicitud estado, Pageable pageable);

    /** Contar solicitudes pendientes de un colegio */
    long countByColegioAndEstado(Colegio colegio, EstadoSolicitud estado);

    /** Todas las solicitudes filtradas por estado — para el panel del ADMIN */
    Page<SolicitudEspecial> findByEstadoOrderByCreatedAtDesc(
            EstadoSolicitud estado, Pageable pageable);

    /** TODAS las solicitudes (sin filtro de estado) — para el panel del ADMIN */
    Page<SolicitudEspecial> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
