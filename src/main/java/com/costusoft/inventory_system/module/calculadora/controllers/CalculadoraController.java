package com.costusoft.inventory_system.module.calculadora.controllers;

import com.costusoft.inventory_system.module.calculadora.dto.CalculadoraDTO;
import com.costusoft.inventory_system.module.calculadora.service.CalculadoraService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST del módulo Calculadora de Disponibilidad.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * ENDPOINTS
 * ──────────────────────────────────────────────────────────────────────────
 *
 * 1. POST /api/calculadora/verificar
 * → ¿Puedo fabricar N unidades de UNA prenda en talla X con el stock actual?
 * Body: { "uniformeId": 1, "cantidad": 50, "talla": "M" }
 *
 * 2. GET /api/calculadora/verificar/{uniformeId}?cantidad=50&talla=M
 * → Igual que el POST pero sin body. Útil para pruebas en Swagger/Postman.
 *
 * 3. POST /api/calculadora/pedido
 * → ¿Puedo completar un pedido con MÚLTIPLES prendas y tallas?
 * Modo A — por colegio: { "colegioId": 1, "cantidad": 50, "talla": "M" }
 * Modo B — lista explícita: { "prendas":
 * [{"uniformeId":1,"cantidad":50,"talla":"M"},...] }
 *
 * Todos los endpoints son de SOLO LECTURA — no modifican el inventario.
 *
 * NOTA sobre Educación Física:
 * Las prendas de Ed. Física son UNISEX (genero=null). La calculadora las
 * procesa exactamente igual que cualquier otra prenda. El campo 'genero'
 * en la respuesta será null para estas prendas, lo cual es correcto.
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

        // ── 1. Verificar una sola prenda (POST) ──────────────────────────────

        @Operation(summary = "Verificar disponibilidad — una prenda (POST)", description = """
                        ¿Puedo fabricar N unidades de la prenda X en talla Y con el stock actual?

                        La talla es OBLIGATORIA porque los insumos varían por talla.
                        Usa GET /api/uniformes/{id}/tallas para ver las tallas disponibles.

                        Evalúa cada insumo requerido (tela, botones, cremallera, etc.) y retorna:
                        - `disponible`: true solo si TODOS los insumos cubren la cantidad solicitada
                        - `cantidadMaximaFabricable`: máximo fabricable con el stock actual
                        - `detalles[]`: estado por insumo (Disponible / Insuficiente / Sin stock)

                        **Ejemplo de body:**
                        ```json
                        { "uniformeId": 1, "cantidad": 50, "talla": "M" }
                        ```
                        """)
        @PostMapping("/verificar")
        public ResponseEntity<ApiResponse<CalculadoraDTO.Response>> verificar(
                        @Valid @RequestBody CalculadoraDTO.Request request) {

                CalculadoraDTO.Response resultado = calculadoraService.verificarDisponibilidad(request);
                String mensaje = resultado.isDisponible()
                                ? "✓ Stock suficiente — puedes fabricar " + request.getCantidad()
                                                + " unidad(es) de '" + resultado.getNombrePrenda()
                                                + "' talla " + resultado.getTalla()
                                : "✗ Stock insuficiente para '" + resultado.getNombrePrenda()
                                                + "' talla " + resultado.getTalla()
                                                + ". Máximo fabricable: " + resultado.getCantidadMaximaFabricable()
                                                + " unidad(es)";

                return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
        }

        // ── 2. Verificar una sola prenda (GET — conveniente para pruebas) ────

        @Operation(summary = "Verificar disponibilidad — una prenda (GET)", description = """
                        Igual que POST /verificar pero sin necesidad de body.
                        Útil para probar rápidamente desde el navegador o Swagger.

                        La talla es OBLIGATORIA.

                        **Ejemplos:**
                        - GET /api/calculadora/verificar/1?cantidad=50&talla=M
                        - GET /api/calculadora/verificar/3?cantidad=30&talla=06-08
                        """)
        @GetMapping("/verificar/{uniformeId}")
        public ResponseEntity<ApiResponse<CalculadoraDTO.Response>> verificarGet(
                        @PathVariable Long uniformeId,
                        @RequestParam @Min(value = 1, message = "La cantidad debe ser al menos 1") int cantidad,
                        @Parameter(description = "Talla a fabricar. Ej: S, M, L, XL, 06-08, 10-12, 14-16") @RequestParam @NotBlank(message = "La talla es obligatoria") @Size(max = 10) String talla) {

                CalculadoraDTO.Request request = new CalculadoraDTO.Request();
                request.setUniformeId(uniformeId);
                request.setCantidad(cantidad);
                request.setTalla(talla);

                CalculadoraDTO.Response resultado = calculadoraService.verificarDisponibilidad(request);
                String mensaje = resultado.isDisponible()
                                ? "✓ Stock suficiente — máximo fabricable: " + resultado.getCantidadMaximaFabricable()
                                                + " unidad(es) de '" + resultado.getNombrePrenda() + "' talla "
                                                + resultado.getTalla()
                                : "✗ Stock insuficiente. Máximo fabricable: " + resultado.getCantidadMaximaFabricable()
                                                + " unidad(es) de '" + resultado.getNombrePrenda() + "' talla "
                                                + resultado.getTalla();

                return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
        }

        // ── 3. Calcular pedido completo (múltiples prendas + tallas) ────────

        @Operation(summary = "Calcular pedido — múltiples prendas y tallas", description = """
                        Calcula si hay stock para completar un pedido con múltiples (prenda, talla).

                        Consolida insumos compartidos entre prendas: si "Tela lacoste blanco"
                        la usan Suéter-M y Suéter-XL, se suma el total necesario y se compara
                        contra el stock real. Aquí se ve el impacto real del pedido.

                        **MODO A — Todas las prendas de un colegio con la misma talla y cantidad:**
                        ```json
                        {
                          "colegioId": 1,
                          "cantidad": 50,
                          "talla": "M"
                        }
                        ```

                        **MODO B — Lista explícita con talla y cantidad individual por prenda:**
                        ```json
                        {
                          "prendas": [
                            { "uniformeId": 1, "cantidad": 50, "talla": "M"     },
                            { "uniformeId": 2, "cantidad": 50, "talla": "06-08" },
                            { "uniformeId": 3, "cantidad": 30, "talla": "L"     }
                          ]
                        }
                        ```

                        **Respuesta incluye:**
                        - `disponibleCompleto`: true si el pedido puede fabricarse íntegro
                        - `factorCumplimiento`: 0.0–1.0 (1.0 = 100% posible)
                        - `porcentajeCumplimiento`: 0–100 para mostrar en UI
                        - `insumoLimitante`: insumo cuello de botella (null si todo OK)
                        - `prendas[].cantidadMaxima`: máximo por prenda con el factor global
                        - `resumenInsumos[]`: todos los insumos consolidados del pedido

                        **Nota Educación Física:** Las prendas de Ed. Física son unisex (genero=null).
                        Se calculan igual que cualquier otra prenda.

                        **Esta operación es de solo lectura** — no modifica el inventario.
                        Para guardar el resultado en un pedido usa POST /api/pedidos/{id}/calcular.
                        """)
        @PostMapping("/pedido")
        public ResponseEntity<ApiResponse<CalculadoraDTO.PedidoResponse>> calcularPedido(
                        @Valid @RequestBody CalculadoraDTO.PedidoRequest request) {

                boolean tienePrendas = request.getPrendas() != null && !request.getPrendas().isEmpty();
                boolean tieneColegio = request.getColegioId() != null;
                boolean tieneCantidad = request.getCantidad() != null && request.getCantidad() >= 1;
                boolean tieneTalla = request.getTalla() != null && !request.getTalla().isBlank();

                if (!tienePrendas && !tieneColegio) {
                        return ResponseEntity.badRequest().body(ApiResponse.error(
                                        "Debes proporcionar 'prendas' [{uniformeId, cantidad, talla}] "
                                                        + "o 'colegioId' + 'cantidad' + 'talla'.",
                                        (CalculadoraDTO.PedidoResponse) null));
                }

                if (tieneColegio && !tienePrendas) {
                        if (!tieneCantidad) {
                                return ResponseEntity.badRequest().body(ApiResponse.error(
                                                "Cuando usas 'colegioId' también debes indicar 'cantidad' (mínimo 1).",
                                                (CalculadoraDTO.PedidoResponse) null));
                        }
                        if (!tieneTalla) {
                                return ResponseEntity.badRequest().body(ApiResponse.error(
                                                "Cuando usas 'colegioId' debes indicar 'talla' (ej. 'M', '06-08'). "
                                                                + "Para múltiples tallas usa la lista 'prendas' con talla individual.",
                                                (CalculadoraDTO.PedidoResponse) null));
                        }
                }

                CalculadoraDTO.PedidoResponse resultado = calculadoraService.calcularPedido(request);

                String mensaje = resultado.isDisponibleCompleto()
                                ? "✓ Pedido completamente atendible con el stock actual"
                                : "✗ Stock insuficiente. Puedes completar el "
                                                + resultado.getPorcentajeCumplimiento()
                                                + "% del pedido. Insumo limitante: '" + resultado.getInsumoLimitante()
                                                + "'";

                return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
        }
}