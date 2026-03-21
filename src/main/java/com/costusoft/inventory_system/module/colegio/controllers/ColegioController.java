package com.costusoft.inventory_system.module.colegio.controllers;

import com.costusoft.inventory_system.module.colegio.dto.ColegioDTO;
import com.costusoft.inventory_system.module.colegio.service.ColegioService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST del modulo Colegio.
 *
 * Endpoints:
 * GET /api/colegios — listar paginado
 * GET /api/colegios/{id} — obtener con uniformes incluidos
 * POST /api/colegios — crear
 * PUT /api/colegios/{id} — actualizar
 * DELETE /api/colegios/{id} — eliminar (solo ADMIN, falla si tiene uniformes)
 */
@Validated
@RestController
@RequestMapping("/api/colegios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Tag(name = "Colegios", description = "Gestion de colegios clientes")
public class ColegioController {

    private final ColegioService colegioService;

    @Operation(summary = "Listar colegios paginados")
    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<ColegioDTO.Response>>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "nombre") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return ResponseEntity.ok(
                ApiResponse.ok("Colegios obtenidos",
                        colegioService.listar(PageRequest.of(page, Math.min(size, 100), sort))));
    }

    @Operation(summary = "Obtener colegio por ID con sus uniformes")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ColegioDTO.ResponseConUniformes>> obtenerPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.ok("Colegio encontrado", colegioService.obtenerPorId(id)));
    }

    @Operation(summary = "Crear nuevo colegio")
    @PostMapping
    public ResponseEntity<ApiResponse<ColegioDTO.Response>> crear(
            @Valid @RequestBody ColegioDTO.Request request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Colegio creado exitosamente", colegioService.crear(request)));
    }

    @Operation(summary = "Actualizar colegio")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ColegioDTO.Response>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ColegioDTO.Request request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Colegio actualizado exitosamente", colegioService.actualizar(id, request)));
    }

    @Operation(summary = "Eliminar colegio", description = "Solo ADMIN. Falla si el colegio tiene uniformes asociados.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        colegioService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Colegio eliminado exitosamente"));
    }
}
