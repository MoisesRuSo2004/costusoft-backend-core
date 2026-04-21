package com.costusoft.inventory_system.module.solicitudespecial.service;

import com.costusoft.inventory_system.entity.SolicitudEspecial.EstadoSolicitud;
import com.costusoft.inventory_system.module.solicitudespecial.dto.SolicitudEspecialAdminDTO;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import org.springframework.data.domain.Pageable;

/**
 * Contrato de negocio del panel administrativo de Solicitudes Especiales.
 *
 * Solo accesible para el rol ADMIN.
 * Permite visualizar y gestionar todas las solicitudes enviadas por instituciones.
 */
public interface SolicitudEspecialAdminService {

    /**
     * Lista todas las solicitudes, con filtro opcional por estado.
     *
     * @param estado null → retorna todos los estados
     * @param pageable paginación y ordenación
     */
    PageDTO<SolicitudEspecialAdminDTO.AdminResponse> listar(
            EstadoSolicitud estado, Pageable pageable);

    /**
     * Obtiene el detalle completo de una solicitud por su ID.
     *
     * @throws com.costusoft.inventory_system.exception.ResourceNotFoundException
     *         si no existe una solicitud con ese id
     */
    SolicitudEspecialAdminDTO.AdminResponse obtener(Long id);

    /**
     * Gestiona una solicitud: cambia su estado y registra la respuesta del equipo.
     *
     * Reglas de negocio:
     * - Si el estado es RESUELTA o RECHAZADA, se registra la fechaRespuesta automáticamente.
     * - Si el estado vuelve a PENDIENTE o EN_REVISION, se limpia la fechaRespuesta.
     * - La respuesta es obligatoria para RESUELTA y RECHAZADA (se valida en el controller).
     *
     * @throws com.costusoft.inventory_system.exception.ResourceNotFoundException
     *         si no existe la solicitud
     * @throws com.costusoft.inventory_system.exception.BusinessException
     *         si la transición de estado no es válida
     */
    SolicitudEspecialAdminDTO.AdminResponse gestionar(
            Long id, SolicitudEspecialAdminDTO.GestionRequest request, String adminUsername);
}
