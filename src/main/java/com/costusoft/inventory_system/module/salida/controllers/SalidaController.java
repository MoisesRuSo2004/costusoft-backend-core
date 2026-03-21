package com.costusoft.inventory_system.module.salida.controllers;

import com.costusoft.inventory_system.module.salida.dto.SalidaDTO;
import com.costusoft.inventory_system.module.salida.service.SalidaService;
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
 * Controller REST del modulo Salida.
 *
 * Endpoints:
 * GET /api/salidas — listar paginado (desc por fecha)
 * GET /api/salidas/{id} — obtener por ID con detalles
 * POST /api/salidas — crear y descontar stock
 * PUT /api/salidas/{id} — actualizar (revierte + valida + aplica nuevo stock)
 * DELETE /api/salidas/{id} — eliminar solo ADMIN
 */
@Validated
@RestController
@RequestMapping("/api/salidas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Tag(name = "Salidas", description = "Registro de salidas de insumos del inventario")
public class SalidaController {

    private final SalidaService salidaService;

    @Operation(summary = "Listar salidas paginadas")
    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<SalidaDTO.Response>>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        PageDTO<SalidaDTO.Response> resultado = salidaService.listar(
                PageRequest.of(page, Math.min(size, 100),
                        Sort.by("fecha").descending()));

        return ResponseEntity.ok(ApiResponse.ok("Salidas obtenidas", resultado));
    }

    @Operation(summary = "Obtener salida por ID con sus detalles")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalidaDTO.Response>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Salida encontrada", salidaService.obtenerPorId(id)));
    }

    @Operation(summary = "Registrar nueva salida", description = "Valida stock de todos los insumos y descuenta de forma atomica. "
            +
            "Si cualquier insumo no tiene stock suficiente, retorna 422 sin modificar nada.")
    @PostMapping
    public ResponseEntity<ApiResponse<SalidaDTO.Response>> crear(
            @Valid @RequestBody SalidaDTO.Request request) {

        SalidaDTO.Response creada = salidaService.crear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Salida registrada exitosamente", creada));
    }

    @Operation(summary = "Actualizar salida", description = "Revierte el stock original, valida el nuevo y aplica los descuentos. Operacion atomica.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SalidaDTO.Response>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SalidaDTO.Request request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Salida actualizada exitosamente", salidaService.actualizar(id, request)));
    }

    @Operation(summary = "Eliminar salida", description = "Solo ADMIN. El stock NO se revierte automaticamente.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        salidaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Salida eliminada"));
    }
}
