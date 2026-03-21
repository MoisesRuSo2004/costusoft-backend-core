package com.costusoft.inventory_system.module.proveedor.controllers;

import com.costusoft.inventory_system.module.proveedor.dto.ProveedorDTO;
import com.costusoft.inventory_system.module.proveedor.service.ProveedorService;
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
 * Controller REST del modulo Proveedor.
 *
 * Todos los endpoints requieren rol ADMIN.
 * Los proveedores son datos administrativos sensibles.
 *
 * Endpoints:
 * GET /api/proveedores — listar paginado
 * GET /api/proveedores/{id} — obtener por ID
 * POST /api/proveedores — crear
 * PUT /api/proveedores/{id} — actualizar
 * DELETE /api/proveedores/{id} — eliminar
 */
@Validated
@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Proveedores", description = "Gestion de proveedores de insumos")
public class ProveedorController {

    private final ProveedorService proveedorService;

    @Operation(summary = "Listar proveedores paginados")
    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<ProveedorDTO.Response>>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "nombre") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        PageDTO<ProveedorDTO.Response> resultado = proveedorService
                .listar(PageRequest.of(page, Math.min(size, 100), sort));

        return ResponseEntity.ok(ApiResponse.ok("Proveedores obtenidos", resultado));
    }

    @Operation(summary = "Obtener proveedor por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProveedorDTO.Response>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Proveedor encontrado", proveedorService.obtenerPorId(id)));
    }

    @Operation(summary = "Crear nuevo proveedor")
    @PostMapping
    public ResponseEntity<ApiResponse<ProveedorDTO.Response>> crear(
            @Valid @RequestBody ProveedorDTO.Request request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Proveedor creado exitosamente", proveedorService.crear(request)));
    }

    @Operation(summary = "Actualizar proveedor existente")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProveedorDTO.Response>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorDTO.Request request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Proveedor actualizado exitosamente", proveedorService.actualizar(id, request)));
    }

    @Operation(summary = "Eliminar proveedor")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        proveedorService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Proveedor eliminado exitosamente"));
    }
}