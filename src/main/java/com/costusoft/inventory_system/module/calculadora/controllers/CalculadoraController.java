package com.costusoft.inventory_system.module.calculadora.controllers;

import com.costusoft.inventory_system.module.calculadora.dto.CalculadoraDTO;
import com.costusoft.inventory_system.module.calculadora.service.CalculadoraService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST del módulo Calculadora de Disponibilidad.
 *
 * Endpoints:
 *   POST /api/calculadora/verificar  — ¿Puedo fabricar N unidades de UNA prenda?
 *   POST /api/calculadora/pedido     — ¿Puedo completar un pedido con MÚLTIPLES prendas?
 *
 * Ambos son de solo lectura — no modifican ningún dato.
 * Accesible para ADMIN, USER y BODEGA.
 */
@RestController
@RequestMapping("/api/calculadora")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
@Tag(name = "Calculadora", description = "Verificación de disponibilidad de insumos para producción de uniformes")
public class CalculadoraController {

    private final CalculadoraService calculadoraService;

    // ── Verificar una sola prenda ────────────────────────────────────────

    @Operation(
        summary = "Verificar disponibilidad — una prenda",
        description = """
                Responde: ¿puedo fabricar N unidades de la prenda X con el stock actual?

                Evalúa cada insumo requerido (tela, botones, hilo, etc.) y retorna:
                - Estado por insumo: Disponible / Insuficiente / Sin stock
                - cantidadMaximaFabricable: máximo que puede producirse con el stock actual
                - disponible: true solo si TODOS los insumos cubren la cantidad solicitada

                Operación de solo lectura — no modifica ningún dato.
                """)
    @PostMapping("/verificar")
    public ResponseEntity<ApiResponse<CalculadoraDTO.Response>> verificar(
            @Valid @RequestBody CalculadoraDTO.Request request) {

        CalculadoraDTO.Response resultado = calculadoraService.verificarDisponibilidad(request);

        String mensaje = resultado.isDisponible()
                ? "Stock suficiente — puedes fabricar " + request.getCantidad() + " unidad(es)"
                : "Stock insuficiente. Máximo fabricable: " + resultado.getCantidadMaximaFabricable() + " unidad(es)";

        return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
    }

    // ── Calcular pedido completo (múltiples prendas) ─────────────────────

    @Operation(
        summary = "Calcular pedido — múltiples prendas",
        description = """
                Calcula si hay stock para completar un pedido que incluye múltiples prendas.

                Maneja insumos compartidos: si "Tela azul" la usan Camisa (2m) y Pantalón (1.5m),
                el resumen consolidado mostrará totalNecesario = 3.5m vs el stock real.

                Modos de uso:

                a) Por colegio — todas las prendas del colegio con la misma cantidad:
                   { "colegioId": 1, "cantidad": 50 }

                b) Lista explícita — prendas y cantidades individuales:
                   { "prendas": [{"uniformeId": 1, "cantidad": 50}, {"uniformeId": 2, "cantidad": 50}] }

                La respuesta incluye:
                - disponibleCompleto: true si el pedido puede atenderse íntegro
                - factorCumplimiento: 0.0–1.0 (1.0 = pedido completo posible)
                - porcentajeCumplimiento: 0–100 para mostrar en UI
                - insumoLimitante: cuello de botella (null si es disponible)
                - prendas[].cantidadMaxima: máximo por prenda con el factor global
                - resumenInsumos[]: vista consolidada — aquí se ve el impacto real de insumos compartidos

                Operación de solo lectura — no modifica ningún dato.
                """)
    @PostMapping("/pedido")
    public ResponseEntity<ApiResponse<CalculadoraDTO.PedidoResponse>> calcularPedido(
            @Valid @RequestBody CalculadoraDTO.PedidoRequest request) {

        CalculadoraDTO.PedidoResponse resultado = calculadoraService.calcularPedido(request);

        String mensaje = resultado.isDisponibleCompleto()
                ? "Pedido completamente atendible con el stock actual"
                : "Stock insuficiente. Puedes completar el " + resultado.getPorcentajeCumplimiento()
                        + "% del pedido. Insumo limitante: " + resultado.getInsumoLimitante();

        return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
    }
}
