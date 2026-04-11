package com.costusoft.inventory_system.module.ia.controllers;

import com.costusoft.inventory_system.module.ia.dto.IaDTO;
import com.costusoft.inventory_system.module.ia.service.IaService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.List;
import java.util.Map;

/**
 * Controller REST del módulo IA.
 *
 * Endpoints:
 *   POST /api/ia/consultar  — ejecuta una consulta de análisis (Nivel 1)
 *   GET  /api/ia/tipos      — lista los tipos de consulta disponibles
 *   GET  /api/ia/estado     — verifica disponibilidad del servicio Groq
 *
 * Todos los roles autenticados pueden usar el asistente IA.
 * Solo lectura — nunca modifica datos del inventario.
 */
@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
@Tag(name = "Inteligencia Artificial", description = "Consultas de análisis del inventario en lenguaje natural vía Groq")
public class IaController {

    private final IaService iaService;

    // ── Consulta principal ────────────────────────────────────────────────

    @Operation(
        summary     = "Ejecutar consulta de análisis con IA",
        description = "Obtiene datos del inventario, los analiza con Groq (LLaMA 3.3) "
                    + "y retorna una respuesta en español. Solo lectura — no modifica datos."
    )
    @PostMapping("/consultar")
    public ResponseEntity<ApiResponse<IaDTO.ConsultaResponse>> consultar(
            @Valid @RequestBody IaDTO.ConsultaRequest request) {

        IaDTO.ConsultaResponse respuesta = iaService.consultar(request);
        return ResponseEntity.ok(ApiResponse.ok("Análisis completado", respuesta));
    }

    // ── Tipos disponibles ─────────────────────────────────────────────────

    @Operation(
        summary     = "Listar tipos de consulta disponibles",
        description = "Retorna todos los TipoConsulta válidos para usar en POST /consultar."
    )
    @GetMapping("/tipos")
    public ResponseEntity<ApiResponse<List<String>>> getTipos() {
        return ResponseEntity.ok(ApiResponse.ok("Tipos disponibles", iaService.getTiposDisponibles()));
    }

    // ── Chat libre ────────────────────────────────────────────────────────

    @Operation(
        summary     = "Chat libre sobre el inventario",
        description = "El usuario escribe en lenguaje natural. El backend inyecta un snapshot "
                    + "completo del inventario como contexto y Groq responde cualquier pregunta "
                    + "sobre el estado actual del sistema."
    )
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<IaDTO.ChatResponse>> chat(
            @Valid @RequestBody IaDTO.ChatRequest request) {

        IaDTO.ChatResponse respuesta = iaService.chat(request);
        return ResponseEntity.ok(ApiResponse.ok("Respuesta del asistente", respuesta));
    }

    // ── Orden de compra ───────────────────────────────────────────────────

    @Operation(
        summary     = "Generar orden de compra",
        description = "Genera el texto formal de una orden de compra basada en los insumos con "
                    + "stock bajo y predicciones ML. Si se especifica proveedorId, la carta se "
                    + "dirige a ese proveedor. Si no, genera una requisición interna general."
    )
    @PostMapping("/orden-compra")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<IaDTO.OrdenCompraResponse>> generarOrdenCompra(
            @Valid @RequestBody IaDTO.OrdenCompraRequest request) {

        IaDTO.OrdenCompraResponse respuesta = iaService.generarOrdenCompra(request);
        return ResponseEntity.ok(ApiResponse.ok("Orden de compra generada", respuesta));
    }

    // ── Briefing diario ───────────────────────────────────────────────────

    @Operation(
        summary     = "Briefing ejecutivo del día",
        description = "Genera un resumen matutino del estado del inventario: stock crítico, "
                    + "pedidos activos, movimientos pendientes y prioridades del día. "
                    + "Ideal para montar en el dashboard del frontend."
    )
    @GetMapping("/briefing")
    public ResponseEntity<ApiResponse<IaDTO.ConsultaResponse>> getBriefing() {
        IaDTO.ConsultaResponse briefing = iaService.getBriefing();
        return ResponseEntity.ok(ApiResponse.ok("Briefing del día generado", briefing));
    }

    // ── Estado del servicio ───────────────────────────────────────────────

    @Operation(
        summary     = "Verificar estado del servicio IA",
        description = "Retorna si el cliente Groq está configurado y disponible."
    )
    @GetMapping("/estado")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEstado() {
        boolean up = iaService.isServiceUp();
        Map<String, Object> estado = Map.of(
                "disponible", up,
                "proveedor",  "Groq",
                "status",     up ? "OK" : "NO_DISPONIBLE"
        );
        return ResponseEntity.ok(ApiResponse.ok("Estado del servicio IA", estado));
    }
}
