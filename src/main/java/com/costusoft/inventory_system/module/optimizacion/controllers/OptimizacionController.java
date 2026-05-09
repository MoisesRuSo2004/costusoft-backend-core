package com.costusoft.inventory_system.module.optimizacion.controllers;

import com.costusoft.inventory_system.module.optimizacion.dto.OptimizacionDTO;
import com.costusoft.inventory_system.module.optimizacion.service.OptimizacionService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/optimizacion")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Tag(name = "Optimizacion", description = "Motor ILP de optimización de producción por colegio (PuLP + CBC)")
public class OptimizacionController {

    private final OptimizacionService optimizacionService;

    @Operation(summary = "Estado del microservicio de optimización")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> status() {
        boolean up = optimizacionService.servicioDisponible();
        return ResponseEntity.ok(ApiResponse.ok(
                up ? "Servicio de optimización disponible"
                   : "Servicio de optimización no disponible",
                up));
    }

    @Operation(summary = "Ejecutar optimización de producción para un colegio")
    @PostMapping
    public ResponseEntity<ApiResponse<OptimizacionDTO.Response>> optimizar(
            @RequestParam Long colegioId,
            @RequestParam(defaultValue = "true") boolean incluirDemanda,
            Authentication auth) {

        String usuario = auth != null ? auth.getName() : "sistema";
        OptimizacionDTO.Response resultado = optimizacionService.ejecutar(colegioId, incluirDemanda, usuario);

        String mensaje = "OPTIMAL".equals(resultado.getEstado())
                ? String.format("Plan óptimo generado para %s — Utilidad: $%,.0f COP",
                        resultado.getNombreColegio() != null ? resultado.getNombreColegio() : "colegio",
                        resultado.getUtilidadTotal())
                : "No se encontró solución óptima: " + resultado.getMensaje();

        return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
    }

    @Operation(summary = "Descargar PDF de una optimización")
    @GetMapping("/historial/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable int id) {
        byte[] pdf = optimizacionService.descargarPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"optimizacion-" + id + ".pdf\"")
                .body(pdf);
    }

    @Operation(summary = "Historial de optimizaciones ejecutadas")
    @GetMapping("/historial")
    public ResponseEntity<ApiResponse<OptimizacionDTO.HistorialResponse>> historial(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Long colegioId) {

        OptimizacionDTO.HistorialResponse data = optimizacionService.historial(limit, colegioId);
        return ResponseEntity.ok(ApiResponse.ok(
                data.getTotal() + " ejecuciones encontradas", data));
    }
}
