package com.costusoft.inventory_system.module.entrada.controllers;

import com.costusoft.inventory_system.entity.EstadoMovimiento;
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
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST del módulo Entrada.
 *
 * Flujo BODEGA:
 * POST /api/entradas → USER/ADMIN crea solicitud PENDIENTE
 * GET /api/entradas/estado → ADMIN/USER/BODEGA filtra por estado
 * PATCH /api/entradas/{id}/confirmar → ADMIN/BODEGA confirma y suma stock
 * PATCH /api/entradas/{id}/rechazar → ADMIN/BODEGA rechaza con motivo
 */
@Validated
@RestController
@RequestMapping("/api/entradas")
@RequiredArgsConstructor
@Tag(name = "Entradas", description = "Registro y gestión de entradas de insumos al inventario")
public class EntradaController {

    private final EntradaService entradaService;

    // ── Listar paginado ──────────────────────────────────────────────────

    @Operation(summary = "Listar entradas paginadas")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
    public ResponseEntity<ApiResponse<PageDTO<EntradaDTO.Response>>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        return ResponseEntity.ok(ApiResponse.ok("Entradas obtenidas",
                entradaService.listar(
                        PageRequest.of(page, Math.min(size, 100), Sort.by("fecha").descending()))));
    }

    // ── Listar por estado ────────────────────────────────────────────────

    @Operation(summary = "Listar entradas por estado", description = "Permite a BODEGA consultar las solicitudes PENDIENTES que debe gestionar.")
    @GetMapping("/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
    public ResponseEntity<ApiResponse<PageDTO<EntradaDTO.Response>>> listarPorEstado(
            @RequestParam EstadoMovimiento estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        return ResponseEntity.ok(ApiResponse.ok("Entradas obtenidas",
                entradaService.listarPorEstado(estado,
                        PageRequest.of(page, Math.min(size, 100), Sort.by("fecha").descending()))));
    }

    // ── Obtener por ID ───────────────────────────────────────────────────

    @Operation(summary = "Obtener entrada por ID con sus detalles")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
    public ResponseEntity<ApiResponse<EntradaDTO.Response>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Entrada encontrada", entradaService.obtenerPorId(id)));
    }

    // ── Crear ────────────────────────────────────────────────────────────

    @Operation(summary = "Registrar solicitud de entrada", description = "Crea la solicitud en estado PENDIENTE. El stock NO se modifica hasta que BODEGA/ADMIN confirme.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<EntradaDTO.Response>> crear(
            @Valid @RequestBody EntradaDTO.Request request) {

        EntradaDTO.Response creada = entradaService.crear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Solicitud de entrada registrada. En espera de confirmación por BODEGA.", creada));
    }

    // ── Confirmar ────────────────────────────────────────────────────────

    @Operation(summary = "Confirmar entrada", description = "BODEGA/ADMIN verifica físicamente y confirma. El stock se incrementa de forma atómica.")
    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN', 'BODEGA')")
    public ResponseEntity<ApiResponse<EntradaDTO.Response>> confirmar(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        EntradaDTO.Response confirmada = entradaService.confirmar(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Entrada confirmada. Stock incrementado.", confirmada));
    }

    // ── Rechazar ─────────────────────────────────────────────────────────

    @Operation(summary = "Rechazar entrada", description = "BODEGA/ADMIN rechaza con motivo. El stock permanece intacto.")
    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN', 'BODEGA')")
    public ResponseEntity<ApiResponse<EntradaDTO.Response>> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody EntradaDTO.RechazarRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        EntradaDTO.Response rechazada = entradaService.rechazar(id, request.getMotivo(), username);
        return ResponseEntity.ok(ApiResponse.ok("Entrada rechazada.", rechazada));
    }

    // ── Actualizar ───────────────────────────────────────────────────────

    @Operation(summary = "Actualizar entrada PENDIENTE", description = "Solo se permite editar entradas en estado PENDIENTE. El stock no se modifica.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<EntradaDTO.Response>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EntradaDTO.Request request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Entrada actualizada", entradaService.actualizar(id, request)));
    }

    // ── Eliminar ─────────────────────────────────────────────────────────

    @Operation(summary = "Eliminar entrada", description = "Solo ADMIN. No se permite eliminar entradas CONFIRMADAS (el stock ya fue aplicado).")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        entradaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Entrada eliminada"));
    }
}
