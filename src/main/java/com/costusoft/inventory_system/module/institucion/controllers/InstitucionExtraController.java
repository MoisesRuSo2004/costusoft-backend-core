package com.costusoft.inventory_system.module.institucion.controllers;

import com.costusoft.inventory_system.module.institucion.dto.InstitucionDTO;
import com.costusoft.inventory_system.module.institucion.service.InstitucionService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints adicionales del portal institucional (plantillas y seguimiento).
 * Separado para no modificar el controlador original y facilitar despliegues.
 */
@RestController
@RequestMapping("/api/institucion")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INSTITUCION')")
public class InstitucionExtraController {

    private final InstitucionService institucionService;

    @Operation(summary = "Plantilla: crear pedido por grado", description = "Genera una plantilla de pedido a partir de un grado y cantidad de estudiantes. Retorna las prendas sugeridas con tallas disponibles y cantidades sugeridas. El frontend debe usar esta plantilla para completar tallas y luego crear el pedido definitivo.")
    @PostMapping("/pedidos/por-grado")
    public ResponseEntity<ApiResponse<InstitucionDTO.PedidoPorGradoResponse>> crearPedidoPorGrado(
            @RequestBody InstitucionDTO.PedidoPorGradoRequest request,
            Authentication auth) {

        InstitucionDTO.PedidoPorGradoResponse respuesta = institucionService.crearPedidoPorGrado(request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Plantilla de pedido por grado", respuesta));
    }

    @Operation(summary = "Seguimiento de pedido institucional", description = "Retorna el estado global, resumen de insumos (faltantes) y referencia a la salida asociada si existe.")
    @GetMapping("/pedidos/{id}/seguimiento")
    public ResponseEntity<ApiResponse<InstitucionDTO.SeguimientoResponse>> seguimientoPedido(
            @Parameter(description = "ID del pedido") @PathVariable Long id,
            Authentication auth) {

        InstitucionDTO.SeguimientoResponse seguimiento = institucionService.obtenerSeguimientoPedido(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Seguimiento del pedido", seguimiento));
    }
}
