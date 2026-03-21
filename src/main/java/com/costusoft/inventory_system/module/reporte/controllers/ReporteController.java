package com.costusoft.inventory_system.module.reporte.controllers;

import com.costusoft.inventory_system.module.reporte.dto.ReporteDTO;
import com.costusoft.inventory_system.module.reporte.service.ReporteService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

/**
 * Controller REST del modulo Reporte.
 *
 * Endpoints:
 * POST /api/reporte — genera reporte en JSON (preview en frontend)
 * POST /api/reporte/exportar/pdf — descarga PDF
 * POST /api/reporte/exportar/excel — descarga Excel (.xlsx)
 *
 * Todos usan POST con body FiltroRequest para evitar
 * URLs largas con multiples query params.
 */
@RestController
@RequestMapping("/api/reporte")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Tag(name = "Reportes", description = "Generacion y exportacion de reportes de inventario")
public class ReporteController {

    private final ReporteService reporteService;

    @Operation(summary = "Generar reporte en JSON", description = "Previsualiza el reporte antes de exportar.")
    @PostMapping
    public ResponseEntity<ApiResponse<ReporteDTO.Response>> generar(
            @Valid @RequestBody ReporteDTO.FiltroRequest filtro) {

        return ResponseEntity.ok(
                ApiResponse.ok("Reporte generado", reporteService.generarReporte(filtro)));
    }

    @Operation(summary = "Exportar reporte en PDF", description = "Descarga el reporte como archivo PDF.")
    @PostMapping("/exportar/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @Valid @RequestBody ReporteDTO.FiltroRequest filtro) {

        ByteArrayInputStream pdf = reporteService.exportarPdf(filtro);
        String nombreArchivo = "reporte-inventario-" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.readAllBytes());
    }

    @Operation(summary = "Exportar reporte en Excel", description = "Descarga el reporte como archivo Excel (.xlsx).")
    @PostMapping("/exportar/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @Valid @RequestBody ReporteDTO.FiltroRequest filtro) {

        ByteArrayInputStream excel = reporteService.exportarExcel(filtro);
        String nombreArchivo = "reporte-inventario-" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel.readAllBytes());
    }
}
