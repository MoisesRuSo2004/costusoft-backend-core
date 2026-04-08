package com.costusoft.inventory_system.module.calculadora.controllers;

import com.costusoft.inventory_system.module.calculadora.dto.CalculadoraDTO;
import com.costusoft.inventory_system.module.calculadora.service.CalculadoraService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST del módulo Calculadora de Disponibilidad.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * ENDPOINTS DISPONIBLES
 * ──────────────────────────────────────────────────────────────────────────
 *
 * 1. POST /api/calculadora/verificar
 *    → ¿Puedo fabricar N unidades de UNA prenda con el stock actual?
 *    Body: { "uniformeId": 1, "cantidad": 50 }
 *
 * 2. POST /api/calculadora/pedido
 *    → ¿Puedo completar un pedido con MÚLTIPLES prendas?
 *    Modo A (todas las prendas de un colegio con la misma cantidad):
 *      Body: { "colegioId": 1, "cantidad": 50 }
 *    Modo B (lista explícita con cantidades individuales):
 *      Body: { "prendas": [{"uniformeId": 1, "cantidad": 50}, {"uniformeId": 2, "cantidad": 30}] }
 *
 * 3. GET /api/calculadora/verificar/{uniformeId}?cantidad=50
 *    → Verificación rápida sin body (útil para probar desde Swagger/Postman).
 *
 * Todos los endpoints son de SOLO LECTURA — no modifican el inventario.
 * ──────────────────────────────────────────────────────────────────────────
 */
@Validated
@RestController
@RequestMapping("/api/calculadora")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
@Tag(name = "Calculadora", description = "Verificación de disponibilidad de insumos para producción de uniformes")
public class CalculadoraController {

    private final CalculadoraService calculadoraService;

    // ── Verificar una sola prenda (POST) ─────────────────────────────────

    @Operation(
        summary = "Verificar disponibilidad — una prenda (POST)",
        description = """
                ¿Puedo fabricar N unidades de la prenda X con el stock actual?

                Evalúa cada insumo requerido (tela, botones, hilo, etc.) y retorna:
                - `disponible`: true solo si TODOS los insumos cubren la cantidad solicitada
                - `cantidadMaximaFabricable`: máximo fabricable con el stock actual
                - `detalles[]`: estado por insumo (Disponible / Insuficiente / Sin stock)

                **Ejemplo de body:**
                ```json
                { "uniformeId": 1, "cantidad": 50 }
                ```
                """)
    @PostMapping("/verificar")
    public ResponseEntity<ApiResponse<CalculadoraDTO.Response>> verificar(
            @Valid @RequestBody CalculadoraDTO.Request request) {

        CalculadoraDTO.Response resultado = calculadoraService.verificarDisponibilidad(request);
        String mensaje = resultado.isDisponible()
                ? "✓ Stock suficiente — puedes fabricar " + request.getCantidad() + " unidad(es) de '"
                        + resultado.getNombrePrenda() + "'"
                : "✗ Stock insuficiente para '" + resultado.getNombrePrenda()
                        + "'. Máximo fabricable: " + resultado.getCantidadMaximaFabricable() + " unidad(es)";

        return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
    }

    // ── Verificar una sola prenda (GET — conveniente para pruebas) ───────

    @Operation(
        summary = "Verificar disponibilidad — una prenda (GET)",
        description = """
                Igual que el POST /verificar pero sin necesidad de body.
                Útil para probar rápidamente desde el navegador o Swagger.

                **Ejemplo:** GET /api/calculadora/verificar/1?cantidad=50
                """)
    @GetMapping("/verificar/{uniformeId}")
    public ResponseEntity<ApiResponse<CalculadoraDTO.Response>> verificarGet(
            @PathVariable Long uniformeId,
            @RequestParam @Min(value = 1, message = "La cantidad debe ser al menos 1") int cantidad) {

        CalculadoraDTO.Request request = new CalculadoraDTO.Request();
        request.setUniformeId(uniformeId);
        request.setCantidad(cantidad);

        CalculadoraDTO.Response resultado = calculadoraService.verificarDisponibilidad(request);
        String mensaje = resultado.isDisponible()
                ? "✓ Stock suficiente — máximo fabricable: " + resultado.getCantidadMaximaFabricable()
                : "✗ Stock insuficiente. Máximo fabricable: " + resultado.getCantidadMaximaFabricable();

        return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
    }

    // ── Calcular pedido completo (múltiples prendas) ─────────────────────

    @Operation(
        summary = "Calcular pedido — múltiples prendas",
        description = """
                Calcula si hay stock para completar un pedido con múltiples prendas.
                Consolida insumos compartidos (si Camisa y Pantalón usan la misma Tela,
                se suma el total necesario y se compara contra el stock real).

                **MODO A — Todas las prendas de un colegio con la misma cantidad:**
                ```json
                {
                  "colegioId": 1,
                  "cantidad": 50
                }
                ```

                **MODO B — Lista explícita con cantidades individuales:**
                ```json
                {
                  "prendas": [
                    { "uniformeId": 1, "cantidad": 50 },
                    { "uniformeId": 2, "cantidad": 50 },
                    { "uniformeId": 3, "cantidad": 30 }
                  ]
                }
                ```

                **Respuesta incluye:**
                - `disponibleCompleto`: true si el pedido puede fabricarse íntegro
                - `factorCumplimiento`: 0.0–1.0 (1.0 = pedido 100% posible)
                - `porcentajeCumplimiento`: 0–100 para mostrar en UI
                - `insumoLimitante`: nombre del insumo cuello de botella
                - `prendas[].cantidadMaxima`: máximo por prenda con el factor global
                - `resumenInsumos[]`: todos los insumos consolidados (aquí se ve el impacto real)

                **Nota:** Esta es una operación de solo lectura. Para guardar el resultado
                en un pedido usa `POST /api/pedidos/{id}/calcular`.
                """)
    @PostMapping("/pedido")
    public ResponseEntity<ApiResponse<CalculadoraDTO.PedidoResponse>> calcularPedido(
            @Valid @RequestBody CalculadoraDTO.PedidoRequest request) {

        // Validación explícita de modo de uso (bean validation no cubre cross-field)
        boolean tienePrendas  = request.getPrendas()   != null && !request.getPrendas().isEmpty();
        boolean tieneColegio  = request.getColegioId() != null;
        boolean tieneCantidad = request.getCantidad()  != null && request.getCantidad() >= 1;

        if (!tienePrendas && !tieneColegio) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Debes proporcionar 'prendas' (lista de {uniformeId, cantidad}) "
                    + "o 'colegioId' + 'cantidad'. Ver descripción del endpoint para ejemplos."));
        }

        if (tieneColegio && !tieneCantidad && !tienePrendas) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Cuando usas 'colegioId' también debes indicar 'cantidad' "
                    + "(número de uniformes a fabricar por prenda)."));
        }

        CalculadoraDTO.PedidoResponse resultado = calculadoraService.calcularPedido(request);

        String mensaje = resultado.isDisponibleCompleto()
                ? "✓ Pedido completamente atendible con el stock actual"
                : "✗ Stock insuficiente. Puedes completar el "
                        + resultado.getPorcentajeCumplimiento()
                        + "% del pedido. Limitante: '" + resultado.getInsumoLimitante() + "'";

        return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
    }
}
