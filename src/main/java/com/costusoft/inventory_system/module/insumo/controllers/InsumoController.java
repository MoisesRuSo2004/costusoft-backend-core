package com.costusoft.inventory_system.module.insumo.controllers;

import com.costusoft.inventory_system.module.insumo.dto.InsumoDTO;
import com.costusoft.inventory_system.module.insumo.service.InsumoService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST del modulo Insumo.
 *
 * Endpoints:
 * GET /api/insumos — listar paginado
 * GET /api/insumos/{id} — obtener por ID
 * GET /api/insumos/buscar — busqueda por nombre (autocompletado)
 * GET /api/insumos/stock-bajo — insumos bajo su stockMinimo
 * POST /api/insumos — crear
 * PUT /api/insumos/{id} — actualizar
 * PATCH /api/insumos/{id}/stock — ajuste manual de stock
 * DELETE /api/insumos/{id} — eliminar (solo ADMIN)
 */
@Validated
@RestController
@RequestMapping("/api/insumos")
@RequiredArgsConstructor
@Tag(name = "Insumos", description = "Gestion del inventario de insumos")
public class InsumoController {

    private final InsumoService insumoService;

    // ── GET /api/insumos ─────────────────────────────────────────────────

    @Operation(summary = "Listar insumos paginados", description = "Retorna todos los insumos con paginacion y ordenamiento.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<InsumoDTO.Response>>> listar(
            @Parameter(description = "Numero de pagina (desde 0)") @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Cantidad por pagina (max 100)") @RequestParam(defaultValue = "10") @Min(1) int size,

            @Parameter(description = "Campo por el que ordenar") @RequestParam(defaultValue = "nombre") String sortBy,

            @Parameter(description = "Direccion: asc o desc") @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        PageDTO<InsumoDTO.Response> resultado = insumoService.listar(pageable);

        return ResponseEntity.ok(ApiResponse.ok("Insumos obtenidos", resultado));
    }

    // ── GET /api/insumos/{id} ────────────────────────────────────────────

    @Operation(summary = "Obtener insumo por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InsumoDTO.Response>> obtenerPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.ok("Insumo encontrado", insumoService.obtenerPorId(id)));
    }

    // ── GET /api/insumos/buscar?nombre=tela ──────────────────────────────

    @Operation(summary = "Buscar insumos por nombre", description = "Busqueda parcial insensible a mayusculas. Util para autocompletado.")
    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<List<InsumoDTO.Response>>> buscar(
            @Parameter(description = "Texto a buscar en el nombre del insumo") @RequestParam @Size(min = 1, max = 100, message = "El termino debe tener entre 1 y 100 caracteres") String nombre) {

        return ResponseEntity.ok(
                ApiResponse.ok("Resultados de busqueda", insumoService.buscarPorNombre(nombre)));
    }

    // ── GET /api/insumos/stock-bajo ──────────────────────────────────────

    @Operation(summary = "Insumos con stock bajo", description = "Retorna insumos cuyo stock es igual o menor a su stockMinimo configurado.")
    @GetMapping("/stock-bajo")
    public ResponseEntity<ApiResponse<List<InsumoDTO.Response>>> stockBajo() {
        List<InsumoDTO.Response> criticos = insumoService.obtenerConStockBajo();

        return ResponseEntity.ok(
                ApiResponse.ok(
                        criticos.isEmpty()
                                ? "No hay insumos con stock bajo"
                                : criticos.size() + " insumo(s) con stock bajo",
                        criticos));
    }

    // ── POST /api/insumos ────────────────────────────────────────────────

    @Operation(summary = "Crear nuevo insumo")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<InsumoDTO.Response>> crear(
            @Valid @RequestBody InsumoDTO.Request request) {

        InsumoDTO.Response creado = insumoService.crear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Insumo creado exitosamente", creado));
    }

    // ── PUT /api/insumos/{id} ────────────────────────────────────────────

    @Operation(summary = "Actualizar insumo existente")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<InsumoDTO.Response>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody InsumoDTO.Request request) {

        InsumoDTO.Response actualizado = insumoService.actualizar(id, request);
        return ResponseEntity.ok(
                ApiResponse.ok("Insumo actualizado exitosamente", actualizado));
    }

    // ── PATCH /api/insumos/{id}/stock ────────────────────────────────────

    @Operation(summary = "Ajuste manual de stock", description = "Permite corregir el stock de un insumo sin registrar una entrada/salida formal.")
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InsumoDTO.Response>> ajustarStock(
            @PathVariable Long id,
            @Valid @RequestBody InsumoDTO.StockUpdateRequest request) {

        InsumoDTO.Response actualizado = insumoService.ajustarStock(id, request);
        return ResponseEntity.ok(
                ApiResponse.ok("Stock ajustado exitosamente", actualizado));
    }

    // ── DELETE /api/insumos/{id} ─────────────────────────────────────────

    @Operation(summary = "Eliminar insumo", description = "Solo disponible para ADMIN. Elimina permanentemente el insumo.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        insumoService.eliminar(id);
        return ResponseEntity.ok(
                ApiResponse.ok("Insumo eliminado exitosamente"));
    }
}
