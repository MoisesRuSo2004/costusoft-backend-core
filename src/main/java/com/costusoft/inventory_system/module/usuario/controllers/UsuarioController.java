package com.costusoft.inventory_system.module.usuario.controllers;

import com.costusoft.inventory_system.module.usuario.dto.UsuarioDTO;
import com.costusoft.inventory_system.module.usuario.service.UsuarioService;
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
 * Controller REST del modulo Usuario.
 *
 * Endpoints ADMIN:
 * GET /api/usuarios — listar paginado
 * GET /api/usuarios/{id} — obtener por ID
 * POST /api/usuarios — crear usuario
 * PUT /api/usuarios/{id} — actualizar usuario
 * DELETE /api/usuarios/{id} — eliminar usuario
 * PATCH /api/usuarios/{id}/toggle — activar / desactivar
 *
 * Endpoints ADMIN + USER (sobre la propia cuenta):
 * PATCH /api/usuarios/mi-password — cambiar password propio
 */
@Validated
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestion de usuarios del sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ── ADMIN: CRUD completo ─────────────────────────────────────────────

    @Operation(summary = "Listar usuarios paginados")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageDTO<UsuarioDTO.Response>>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return ResponseEntity.ok(
                ApiResponse.ok("Usuarios obtenidos",
                        usuarioService.listar(PageRequest.of(page, Math.min(size, 100), sort))));
    }

    @Operation(summary = "Obtener usuario por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioDTO.Response>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Usuario encontrado", usuarioService.obtenerPorId(id)));
    }

    @Operation(summary = "Crear nuevo usuario")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioDTO.Response>> crear(
            @Valid @RequestBody UsuarioDTO.CreateRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario creado exitosamente",
                        usuarioService.crear(request)));
    }

    @Operation(summary = "Actualizar usuario", description = "Si no se envia password, se mantiene el actual.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioDTO.Response>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioDTO.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Usuario actualizado exitosamente",
                        usuarioService.actualizar(id, request)));
    }

    @Operation(summary = "Eliminar usuario", description = "No se puede eliminar la propia cuenta.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario eliminado exitosamente"));
    }

    @Operation(summary = "Activar o desactivar usuario", description = "Toggle del estado activo. No se puede desactivar la propia cuenta.")
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioDTO.Response>> toggleActivo(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Estado del usuario actualizado",
                        usuarioService.toggleActivo(id)));
    }

    // ── TODOS: Mi perfil ────────────────────────────────────────────────

    @Operation(summary = "Mi perfil", description = "Retorna los datos del usuario autenticado. "
            + "Disponible para todos los roles (ADMIN, USER, BODEGA, INSTITUCION).")
    @GetMapping("/mi-perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UsuarioDTO.Response>> miPerfil(Authentication auth) {
        return ResponseEntity.ok(
                ApiResponse.ok("Mi perfil", usuarioService.obtenerPorUsername(auth.getName())));
    }

    // ── ADMIN + USER: cambio de password propio ──────────────────────────

    @Operation(summary = "Cambiar mi password", description = "Disponible para ADMIN y USER. Valida la contrasena actual antes de cambiar.")
    @PatchMapping("/mi-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
            @Valid @RequestBody UsuarioDTO.ChangePasswordRequest request) {

        usuarioService.cambiarPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Contrasena actualizada exitosamente"));
    }
}