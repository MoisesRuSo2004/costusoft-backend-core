package com.costusoft.inventory_system.module.dashboard.controllers;

import com.costusoft.inventory_system.module.dashboard.dto.DashboardDTO;
import com.costusoft.inventory_system.module.dashboard.service.DashboardService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST del modulo Dashboard.
 *
 * Endpoints:
 * GET /api/dashboard/resumen — metricas completas del sistema
 *
 * Un solo endpoint que retorna todo — el frontend hace
 * una sola llamada al montar la pantalla principal.
 * ADMIN y USER pueden ver el dashboard.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Tag(name = "Dashboard", description = "Metricas y resumen general del sistema")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Resumen general del sistema", description = "Retorna contadores, alertas de stock, graficos de movimientos "
            +
            "y actividad reciente en una sola llamada.")
    @GetMapping("/resumen")
    public ResponseEntity<ApiResponse<DashboardDTO>> resumen() {
        return ResponseEntity.ok(
                ApiResponse.ok("Dashboard generado", dashboardService.generarResumen()));
    }
}