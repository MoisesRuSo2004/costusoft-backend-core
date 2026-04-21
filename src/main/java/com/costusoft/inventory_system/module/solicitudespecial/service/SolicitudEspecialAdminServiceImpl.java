package com.costusoft.inventory_system.module.solicitudespecial.service;

import com.costusoft.inventory_system.entity.SolicitudEspecial;
import com.costusoft.inventory_system.entity.SolicitudEspecial.EstadoSolicitud;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.solicitudespecial.dto.SolicitudEspecialAdminDTO;
import com.costusoft.inventory_system.repo.SolicitudEspecialRepository;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Set;

/**
 * Implementación del panel administrativo de Solicitudes Especiales.
 *
 * Principios aplicados:
 * - Las transiciones de estado siguen el flujo: PENDIENTE → EN_REVISION → RESUELTA | RECHAZADA
 * - Solo RESUELTA y RECHAZADA registran fechaRespuesta y respuesta
 * - Se audita quién realizó el cambio con log estructurado
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SolicitudEspecialAdminServiceImpl implements SolicitudEspecialAdminService {

    private final SolicitudEspecialRepository solicitudRepository;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Estados que cierran el ciclo de vida de la solicitud */
    private static final Set<EstadoSolicitud> ESTADOS_FINALES =
            EnumSet.of(EstadoSolicitud.RESUELTA, EstadoSolicitud.RECHAZADA);

    // ── Listar ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageDTO<SolicitudEspecialAdminDTO.AdminResponse> listar(
            EstadoSolicitud estado, Pageable pageable) {

        Page<SolicitudEspecial> page = (estado != null)
                ? solicitudRepository.findByEstadoOrderByCreatedAtDesc(estado, pageable)
                : solicitudRepository.findAllByOrderByCreatedAtDesc(pageable);

        return PageDTO.from(page, this::toAdminResponse);
    }

    // ── Obtener ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SolicitudEspecialAdminDTO.AdminResponse obtener(Long id) {
        return toAdminResponse(buscarPorId(id));
    }

    // ── Gestionar ──────────────────────────────────────────────────────────

    @Override
    public SolicitudEspecialAdminDTO.AdminResponse gestionar(
            Long id, SolicitudEspecialAdminDTO.GestionRequest request, String adminUsername) {

        SolicitudEspecial solicitud = buscarPorId(id);

        // Validar que RESUELTA/RECHAZADA incluyan respuesta
        if (ESTADOS_FINALES.contains(request.getEstado())) {
            String resp = request.getRespuesta();
            if (resp == null || resp.isBlank()) {
                throw new BusinessException(
                        "Debes proporcionar una respuesta al " +
                        (request.getEstado() == EstadoSolicitud.RESUELTA
                                ? "resolver" : "rechazar") +
                        " la solicitud.");
            }
        }

        EstadoSolicitud estadoAnterior = solicitud.getEstado();

        // Aplicar cambios
        solicitud.setEstado(request.getEstado());

        if (request.getRespuesta() != null && !request.getRespuesta().isBlank()) {
            solicitud.setRespuesta(request.getRespuesta().trim());
        }

        // Fecha de respuesta: solo para estados finales
        if (ESTADOS_FINALES.contains(request.getEstado())) {
            solicitud.setFechaRespuesta(LocalDateTime.now());
        } else {
            // Si vuelve a PENDIENTE o EN_REVISION, limpiar fechaRespuesta
            solicitud.setFechaRespuesta(null);
        }

        SolicitudEspecial actualizada = solicitudRepository.save(solicitud);

        log.info("Solicitud #{} — estado: {} → {} | admin: '{}' | colegio: '{}'",
                id, estadoAnterior, request.getEstado(),
                adminUsername, solicitud.getColegio().getNombre());

        return toAdminResponse(actualizada);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private SolicitudEspecial buscarPorId(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SolicitudEspecial", id));
    }

    /** Convierte entidad → AdminResponse para el panel del admin. */
    private SolicitudEspecialAdminDTO.AdminResponse toAdminResponse(SolicitudEspecial s) {
        return SolicitudEspecialAdminDTO.AdminResponse.builder()
                .id(s.getId())
                .tipo(s.getTipo().name())
                .estado(s.getEstado().name())
                .asunto(s.getAsunto())
                .descripcion(s.getDescripcion())
                .respuesta(s.getRespuesta())
                .fechaRespuesta(s.getFechaRespuesta() != null
                        ? s.getFechaRespuesta().format(FMT) : null)
                // Datos del remitente (institución)
                .colegioNombre(s.getColegio() != null
                        ? s.getColegio().getNombre() : "Sin colegio")
                .username(s.getUsuario() != null
                        ? s.getUsuario().getUsername() : "Desconocido")
                // Auditoría
                .createdAt(s.getCreatedAt() != null
                        ? s.getCreatedAt().format(FMT) : null)
                .updatedAt(s.getUpdatedAt() != null
                        ? s.getUpdatedAt().format(FMT) : null)
                .build();
    }
}
