package com.costusoft.inventory_system.module.salida.controllers;

import com.costusoft.inventory_system.entity.EstadoMovimiento;
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
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST del módulo Salida.
 *
 * Flujo BODEGA:
 *   POST   /api/salidas              → USER/ADMIN crea solicitud PENDIENTE
 *   GET    /api/salidas/estado       → ADMIN/USER/BODEGA filtra por estado
 *   PATCH  /api/salidas/{id}/confirmar → ADMIN/BODEGA confirma, valida stock y descuenta
 *   PATCH  /api/salidas/{id}/rechazar  → ADMIN/BODEGA rechaza con motivo
 */
@Validated
@RestController
@RequestMapping("/api/salidas")
@RequiredArgsConstructor
@Tag(name = "Salidas", description = "Registro y gestión de salidas de insumos del inventario")
public class SalidaController {

    private final SalidaService salidaService;

    // ── Listar paginado ──────────────────────────────────────────────────

    @Operation(summary = "Listar salidas paginadas")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
    public ResponseEntity<ApiResponse<PageDTO<SalidaDTO.Response>>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        return ResponseEntity.ok(ApiResponse.ok("Salidas obtenidas",
                salidaService.listar(
                        PageRequest.of(page, Math.min(size, 100), Sort.by("fecha").descending()))));
    }

    // ── Listar por estado ────────────────────────────────────────────────

    @Operation(summary = "Listar salidas por estado",
               description = "Permite a BODEGA consultar las solicitudes PENDIENTES que debe gestionar.")
    @GetMapping("/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
    public ResponseEntity<ApiResponse<PageDTO<SalidaDTO.Response>>> listarPorEstado(
            @RequestParam EstadoMovimiento estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        return ResponseEntity.ok(ApiResponse.ok("Salidas obtenidas",
                salidaService.listarPorEstado(estado,
                        PageRequest.of(page, Math.min(size, 100), Sort.by("fecha").descending()))));
    }

    // ── Obtener por ID ───────────────────────────────────────────────────

    @Operation(summary = "Obtener salida por ID con sus detalles")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
    public ResponseEntity<ApiResponse<SalidaDTO.Response>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Salida encontrada", salidaService.obtenerPorId(id)));
    }

    // ── Crear ────────────────────────────────────────────────────────────

    @Operation(summary = "Registrar solicitud de salida",
               description = "Crea la solicitud en estado PENDIENTE. El stock NO se descuenta hasta que BODEGA/ADMIN confirme.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<SalidaDTO.Response>> crear(
            @Valid @RequestBody SalidaDTO.Request request) {

        SalidaDTO.Response creada = salidaService.crear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Solicitud de salida registrada. En espera de confirmación por BODEGA.", creada));
    }

    // ── Confirmar ────────────────────────────────────────────────────────

    @Operation(summary = "Confirmar salida",
               description = "BODEGA/ADMIN verifica físicamente y confirma. "
                       + "Valida stock suficiente de todos los insumos antes de descontar. "
                       + "Si algún insumo no tiene stock retorna 422 sin modificar nada.")
    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN', 'BODEGA')")
    public ResponseEntity<ApiResponse<SalidaDTO.Response>> confirmar(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        SalidaDTO.Response confirmada = salidaService.confirmar(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Salida confirmada. Stock descontado.", confirmada));
    }

    // ── Rechazar ─────────────────────────────────────────────────────────

    @Operation(summary = "Rechazar salida",
               description = "BODEGA/ADMIN rechaza con motivo. El stock permanece intacto.")
    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN', 'BODEGA')")
    public ResponseEntity<ApiResponse<SalidaDTO.Response>> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody SalidaDTO.RechazarRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        SalidaDTO.Response rechazada = salidaService.rechazar(id, request.getMotivo(), username);
        return ResponseEntity.ok(ApiResponse.ok("Salida rechazada.", rechazada));
    }

    // ── Actualizar ───────────────────────────────────────────────────────

    @Operation(summary = "Actualizar salida PENDIENTE",
               description = "Solo se permite editar salidas en estado PENDIENTE. El stock no se modifica.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<SalidaDTO.Response>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SalidaDTO.Request request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Salida actualizada", salidaService.actualizar(id, request)));
    }

    // ── Eliminar ─────────────────────────────────────────────────────────

    @Operation(summary = "Eliminar salida",
               description = "Solo ADMIN. No se permite eliminar salidas CONFIRMADAS (el stock ya fue descontado).")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        salidaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Salida eliminada"));
    }
}
