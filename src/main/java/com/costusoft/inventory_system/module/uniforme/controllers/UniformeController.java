package com.costusoft.inventory_system.module.uniforme.controllers;

import com.costusoft.inventory_system.module.uniforme.dto.UniformeDTO;
import com.costusoft.inventory_system.module.uniforme.service.UniformeService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST del modulo Uniforme.
 *
 * Endpoints:
 * GET /api/uniformes/{id} — obtener uniforme por ID con insumos
 * GET /api/uniformes/colegio/{colegioId} — listar uniformes de un colegio
 * POST /api/uniformes — crear uniforme
 * PUT /api/uniformes/{id} — actualizar uniforme e insumos requeridos
 * DELETE /api/uniformes/{id} — eliminar (solo ADMIN)
 */
@Validated
@RestController
@RequestMapping("/api/uniformes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Tag(name = "Uniformes", description = "Gestion de uniformes y sus insumos requeridos")
public class UniformeController {

    private final UniformeService uniformeService;

    @Operation(summary = "Listar uniformes de un colegio con sus insumos requeridos")
    @GetMapping("/colegio/{colegioId}")
    public ResponseEntity<ApiResponse<List<UniformeDTO.Response>>> listarPorColegio(
            @PathVariable Long colegioId) {

        List<UniformeDTO.Response> uniformes = uniformeService.listarPorColegio(colegioId);
        return ResponseEntity.ok(
                ApiResponse.ok(
                        uniformes.isEmpty()
                                ? "No hay uniformes registrados para este colegio"
                                : uniformes.size() + " uniforme(s) encontrado(s)",
                        uniformes));
    }

    @Operation(summary = "Obtener uniforme por ID con insumos requeridos")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UniformeDTO.Response>> obtenerPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.ok("Uniforme encontrado", uniformeService.obtenerPorId(id)));
    }

    @Operation(summary = "Crear uniforme", description = "Crea el uniforme y asocia los insumos requeridos en la misma operacion.")
    @PostMapping
    public ResponseEntity<ApiResponse<UniformeDTO.Response>> crear(
            @Valid @RequestBody UniformeDTO.Request request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Uniforme creado exitosamente",
                        uniformeService.crear(request)));
    }

    @Operation(summary = "Actualizar uniforme", description = "Actualiza campos del uniforme y reemplaza completamente la lista de insumos requeridos.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UniformeDTO.Response>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UniformeDTO.Request request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Uniforme actualizado exitosamente",
                        uniformeService.actualizar(id, request)));
    }

    @Operation(summary = "Eliminar uniforme", description = "Solo ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        uniformeService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Uniforme eliminado exitosamente"));
    }
}