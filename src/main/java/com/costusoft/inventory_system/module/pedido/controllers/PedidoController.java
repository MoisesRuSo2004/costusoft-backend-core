package com.costusoft.inventory_system.module.pedido.controllers;

import com.costusoft.inventory_system.entity.EstadoPedido;
import com.costusoft.inventory_system.module.pedido.dto.PedidoDTO;
import com.costusoft.inventory_system.module.pedido.service.PedidoService;
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

import java.util.List;

/**
 * Controller REST del módulo Pedido.
 *
 * Flujo principal:
 *   POST   /api/pedidos                         → crear (BORRADOR)
 *   POST   /api/pedidos/{id}/calcular            → verificar stock
 *   POST   /api/pedidos/{id}/confirmar           → CALCULADO → CONFIRMADO
 *   POST   /api/pedidos/{id}/iniciar-produccion  → CONFIRMADO → EN_PRODUCCION
 *   POST   /api/pedidos/{id}/marcar-listo        → EN_PRODUCCION → LISTO
 *   POST   /api/pedidos/{id}/entregar            → LISTO → ENTREGADO (stock --)
 *   POST   /api/pedidos/{id}/cancelar            → any → CANCELADO
 */
@Validated
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Gestión de pedidos de uniformes — desde cotización hasta entrega")
public class PedidoController {

    private final PedidoService pedidoService;

    // ── Crear ────────────────────────────────────────────────────────────

    @Operation(summary = "Crear pedido",
               description = "Crea un pedido en BORRADOR. No toca el inventario.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PedidoDTO.Response>> crear(
            @Valid @RequestBody PedidoDTO.Request request,
            Authentication auth) {

        PedidoDTO.Response creado = pedidoService.crear(request, auth.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Pedido creado en BORRADOR", creado));
    }

    // ── Listar ───────────────────────────────────────────────────────────

    @Operation(summary = "Listar pedidos paginados")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
    public ResponseEntity<ApiResponse<PageDTO<PedidoDTO.Response>>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        return ResponseEntity.ok(ApiResponse.ok("Pedidos obtenidos",
                pedidoService.listar(
                        PageRequest.of(page, Math.min(size, 100),
                                Sort.by("createdAt").descending()))));
    }

    // ── Listar por estado ────────────────────────────────────────────────

    @Operation(summary = "Listar pedidos por estado")
    @GetMapping("/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
    public ResponseEntity<ApiResponse<PageDTO<PedidoDTO.Response>>> listarPorEstado(
            @RequestParam EstadoPedido estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        return ResponseEntity.ok(ApiResponse.ok("Pedidos obtenidos",
                pedidoService.listarPorEstado(estado,
                        PageRequest.of(page, Math.min(size, 100),
                                Sort.by("createdAt").descending()))));
    }

    // ── Listar por colegio ───────────────────────────────────────────────

    @Operation(summary = "Listar pedidos de un colegio")
    @GetMapping("/colegio/{colegioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
    public ResponseEntity<ApiResponse<PageDTO<PedidoDTO.Response>>> listarPorColegio(
            @PathVariable Long colegioId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {

        return ResponseEntity.ok(ApiResponse.ok("Pedidos del colegio obtenidos",
                pedidoService.listarPorColegio(colegioId,
                        PageRequest.of(page, Math.min(size, 100),
                                Sort.by("createdAt").descending()))));
    }

    // ── Obtener por ID ───────────────────────────────────────────────────

    @Operation(summary = "Obtener pedido por ID",
               description = "Incluye detalles de prendas y resumen de insumos consolidados.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
    public ResponseEntity<ApiResponse<PedidoDTO.Response>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Pedido encontrado", pedidoService.obtenerPorId(id)));
    }

    // ── Actualizar ───────────────────────────────────────────────────────

    @Operation(summary = "Actualizar pedido",
               description = "Solo se permite editar pedidos en BORRADOR. Reinicia los resultados de la calculadora.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PedidoDTO.Response>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PedidoDTO.Request request,
            Authentication auth) {

        return ResponseEntity.ok(ApiResponse.ok("Pedido actualizado",
                pedidoService.actualizar(id, request, auth.getName())));
    }

    // ── Eliminar ─────────────────────────────────────────────────────────

    @Operation(summary = "Eliminar pedido",
               description = "Solo ADMIN. Solo pedidos en BORRADOR.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        pedidoService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Pedido eliminado"));
    }

    // ── Calcular ─────────────────────────────────────────────────────────

    @Operation(summary = "Calcular disponibilidad del pedido",
               description = "Ejecuta la calculadora sobre todas las prendas del pedido, "
                       + "consolidando insumos compartidos. "
                       + "Almacena factorCumplimiento y cantidadMaximaFabricable. "
                       + "Puede re-ejecutarse en BORRADOR o CALCULADO.")
    @PostMapping("/{id}/calcular")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PedidoDTO.Response>> calcular(
            @PathVariable Long id,
            Authentication auth) {

        PedidoDTO.Response resultado = pedidoService.calcular(id, auth.getName());
        String msg = Boolean.TRUE.equals(resultado.getDisponibleCompleto())
                ? "Stock suficiente — pedido puede fabricarse completo."
                : String.format("Stock insuficiente — %d%% fabricable. Limitante: %s",
                        resultado.getPorcentajeCumplimiento(),
                        resultado.getInsumoLimitante());

        return ResponseEntity.ok(ApiResponse.ok(msg, resultado));
    }

    // ── Confirmar ────────────────────────────────────────────────────────

    @Operation(summary = "Confirmar pedido",
               description = "USER/ADMIN confirma que el pedido está listo para iniciar producción. "
                       + "Transición: CALCULADO → CONFIRMADO. El pedido ya no puede editarse.")
    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PedidoDTO.Response>> confirmar(
            @PathVariable Long id,
            Authentication auth) {

        return ResponseEntity.ok(ApiResponse.ok("Pedido confirmado. Listo para iniciar producción.",
                pedidoService.confirmar(id, auth.getName())));
    }

    // ── Iniciar producción ───────────────────────────────────────────────

    @Operation(summary = "Iniciar producción",
               description = "Genera una Salida PENDIENTE con todos los insumos agregados del pedido. "
                       + "BODEGA debe confirmar la Salida cuando retire los insumos físicamente. "
                       + "Transición: CONFIRMADO → EN_PRODUCCION.")
    @PostMapping("/{id}/iniciar-produccion")
    @PreAuthorize("hasAnyRole('ADMIN', 'BODEGA')")
    public ResponseEntity<ApiResponse<PedidoDTO.Response>> iniciarProduccion(
            @PathVariable Long id,
            Authentication auth) {

        PedidoDTO.Response resultado = pedidoService.iniciarProduccion(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(
                "Producción iniciada. Salida PENDIENTE generada (id: " + resultado.getSalidaId() + ").",
                resultado));
    }

    // ── Marcar listo ─────────────────────────────────────────────────────

    @Operation(summary = "Marcar pedido como listo para entrega",
               description = "Registra que la fabricación fue completada. "
                       + "Transición: EN_PRODUCCION → LISTO_PARA_ENTREGA.")
    @PostMapping("/{id}/marcar-listo")
    @PreAuthorize("hasAnyRole('ADMIN', 'BODEGA')")
    public ResponseEntity<ApiResponse<PedidoDTO.Response>> marcarListo(
            @PathVariable Long id,
            Authentication auth) {

        return ResponseEntity.ok(ApiResponse.ok("Pedido marcado como listo para entrega.",
                pedidoService.marcarListo(id, auth.getName())));
    }

    // ── Entregar ─────────────────────────────────────────────────────────

    @Operation(summary = "Registrar entrega al colegio",
               description = "Confirma la entrega física al colegio. "
                       + "Confirma la Salida vinculada → descuenta stock del inventario. "
                       + "Transición: LISTO_PARA_ENTREGA → ENTREGADO.")
    @PostMapping("/{id}/entregar")
    @PreAuthorize("hasAnyRole('ADMIN', 'BODEGA')")
    public ResponseEntity<ApiResponse<PedidoDTO.Response>> entregar(
            @PathVariable Long id,
            Authentication auth) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Pedido entregado. Stock descontado del inventario.",
                pedidoService.entregar(id, auth.getName())));
    }

    // ── Cancelar ─────────────────────────────────────────────────────────

    @Operation(summary = "Cancelar pedido",
               description = "Cancela el pedido en cualquier estado no-final. "
                       + "Si existía una Salida PENDIENTE, la rechaza sin tocar el stock.")
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PedidoDTO.Response>> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody PedidoDTO.CancelarRequest request,
            Authentication auth) {

        return ResponseEntity.ok(ApiResponse.ok("Pedido cancelado.",
                pedidoService.cancelar(id, request.getMotivo(), auth.getName())));
    }

    // ── Historial ────────────────────────────────────────────────────────

    @Operation(summary = "Obtener historial de auditoría del pedido",
               description = "Retorna todas las transiciones de estado con usuario, fecha y observación.")
    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'BODEGA')")
    public ResponseEntity<ApiResponse<List<PedidoDTO.HistorialResponse>>> obtenerHistorial(
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.ok("Historial obtenido",
                pedidoService.obtenerHistorial(id)));
    }
}
