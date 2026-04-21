package com.costusoft.inventory_system.module.solicitudespecial.controllers;

import com.costusoft.inventory_system.entity.SolicitudEspecial.EstadoSolicitud;
import com.costusoft.inventory_system.module.solicitudespecial.dto.SolicitudEspecialAdminDTO;
import com.costusoft.inventory_system.module.solicitudespecial.service.SolicitudEspecialAdminService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Panel administrativo de Solicitudes Especiales.
 *
 * Permite al equipo Costusoft (ADMIN) revisar, gestionar y responder
 * las solicitudes enviadas por los coordinadores de colegio (INSTITUCION).
 *
 * Base URL: /api/solicitudes-especiales
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │ Endpoint                          │ Acción                   │
 * ├──────────────────────────────────────────────────────────────┤
 * │ GET  /                            │ Listar (paginado+filtro) │
 * │ GET  /{id}                        │ Detalle de una solicitud │
 * │ PUT  /{id}/gestionar              │ Cambiar estado+respuesta │
 * └──────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/solicitudes-especiales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Solicitudes Especiales (Admin)",
     description = "Panel para que el equipo Costusoft gestione las solicitudes institucionales.")
public class SolicitudEspecialController {

    private final SolicitudEspecialAdminService solicitudService;

    // ══════════════════════════════════════════════════════════════════════
    // LISTAR
    // ══════════════════════════════════════════════════════════════════════

    @Operation(
        summary = "Listar solicitudes institucionales",
        description = "Retorna todas las solicitudes especiales con paginación. "
                    + "Se puede filtrar por estado (PENDIENTE, EN_REVISION, RESUELTA, RECHAZADA). "
                    + "Ordenadas por fecha de creación descendente.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<SolicitudEspecialAdminDTO.AdminResponse>>> listar(
            @Parameter(description = "Filtrar por estado. Si se omite, retorna todos.")
            @RequestParam(required = false) EstadoSolicitud estado,
            @Parameter(description = "Página (0-indexado)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ejemplo: createdAt,desc") @RequestParam(defaultValue = "createdAt,desc") String sort) {

        // Parsear el sort param: "campo,dirección"
        Pageable pageable = buildPageable(page, size, sort);

        PageDTO<SolicitudEspecialAdminDTO.AdminResponse> resultado =
                solicitudService.listar(estado, pageable);

        String msg = estado != null
                ? "Solicitudes con estado " + estado + " (" + resultado.getTotalElements() + ")"
                : "Todas las solicitudes (" + resultado.getTotalElements() + ")";

        return ResponseEntity.ok(ApiResponse.ok(msg, resultado));
    }

    // ══════════════════════════════════════════════════════════════════════
    // DETALLE
    // ══════════════════════════════════════════════════════════════════════

    @Operation(
        summary = "Detalle de una solicitud",
        description = "Retorna el detalle completo de una solicitud especial por su ID.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SolicitudEspecialAdminDTO.AdminResponse>> obtener(
            @Parameter(description = "ID de la solicitud") @PathVariable Long id) {

        SolicitudEspecialAdminDTO.AdminResponse solicitud = solicitudService.obtener(id);
        return ResponseEntity.ok(ApiResponse.ok("Solicitud especial", solicitud));
    }

    // ══════════════════════════════════════════════════════════════════════
    // GESTIONAR
    // ══════════════════════════════════════════════════════════════════════

    @Operation(
        summary = "Gestionar solicitud (cambiar estado + respuesta)",
        description = "Permite al ADMIN cambiar el estado de la solicitud y/o agregar una respuesta. "
                    + "Si el estado es RESUELTA o RECHAZADA, la respuesta es obligatoria. "
                    + "Se registra automáticamente la fecha de respuesta.")
    @PutMapping("/{id}/gestionar")
    public ResponseEntity<ApiResponse<SolicitudEspecialAdminDTO.AdminResponse>> gestionar(
            @Parameter(description = "ID de la solicitud") @PathVariable Long id,
            @Valid @RequestBody SolicitudEspecialAdminDTO.GestionRequest request,
            Authentication auth) {

        SolicitudEspecialAdminDTO.AdminResponse actualizada =
                solicitudService.gestionar(id, request, auth.getName());

        return ResponseEntity.ok(
                ApiResponse.ok("Solicitud gestionada correctamente", actualizada));
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Construye un {@link Pageable} desde los parámetros de la request.
     * El sort se acepta como "campo,dirección" (ej.: "createdAt,desc").
     * Si el formato es inválido, cae al default: createdAt DESC.
     */
    private Pageable buildPageable(int page, int size, String sort) {
        try {
            String[] parts = sort.split(",");
            String campo = parts[0].trim();
            Sort.Direction dir = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            return PageRequest.of(page, Math.min(size, 100), Sort.by(dir, campo));
        } catch (Exception e) {
            return PageRequest.of(page, Math.min(size, 100),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }
    }
}
