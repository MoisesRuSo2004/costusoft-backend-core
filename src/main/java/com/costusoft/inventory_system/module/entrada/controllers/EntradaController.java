package com.costusoft.inventory_system.module.entrada.controllers;

import com.costusoft.inventory_system.module.entrada.dto.EntradaDTO;
import com.costusoft.inventory_system.module.entrada.service.EntradaService;
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
 * Controller REST del modulo Entrada.
 *
 * Endpoints:
 * GET /api/entradas — listar paginado (desc por fecha)
 * GET /api/entradas/{id} — obtener por ID con detalles
 * POST /api/entradas — crear y sumar stock
 * PUT /api/entradas/{id} — actualizar (revierte + aplica nuevo stock)
 * DELETE /api/entradas/{id} — eliminar solo ADMIN (sin revertir stock)
 */
@Validated
@RestController
@RequestMapping("/api/entradas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Tag(name = "Entradas", description = "Registro de entradas de insumos al inventario")
public class EntradaController {

    private final EntradaService entradaService;

    @Operation(summary = "Listar entradas paginadas")
    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<EntradaDTO.Response>>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        PageDTO<EntradaDTO.Response> resultado = entradaService.listar(
                PageRequest.of(page, Math.min(size, 100),
                        Sort.by("fecha").descending()));

        return ResponseEntity.ok(ApiResponse.ok("Entradas obtenidas", resultado));
    }

    @Operation(summary = "Obtener entrada por ID con sus detalles")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EntradaDTO.Response>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Entrada encontrada", entradaService.obtenerPorId(id)));
    }

    @Operation(summary = "Registrar nueva entrada", description = "Crea la entrada y suma el stock a cada insumo del detalle de forma atomica.")
    @PostMapping
    public ResponseEntity<ApiResponse<EntradaDTO.Response>> crear(
            @Valid @RequestBody EntradaDTO.Request request) {

        EntradaDTO.Response creada = entradaService.crear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Entrada registrada exitosamente", creada));
    }

    @Operation(summary = "Actualizar entrada", description = "Revierte el stock de los detalles originales y aplica los nuevos. Operacion atomica.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EntradaDTO.Response>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EntradaDTO.Request request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Entrada actualizada exitosamente", entradaService.actualizar(id, request)));
    }

    @Operation(summary = "Eliminar entrada", description = "Solo ADMIN. El stock NO se revierte automaticamente.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        entradaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Entrada eliminada"));
    }
}