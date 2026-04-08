package com.costusoft.inventory_system.module.pedido.service;

import com.costusoft.inventory_system.entity.EstadoPedido;
import com.costusoft.inventory_system.module.pedido.dto.PedidoDTO;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Contrato de negocio del módulo Pedido.
 *
 * Flujo principal:
 *   crear() → calcular() → confirmar() → iniciarProduccion()
 *             → marcarListo() → entregar()
 *
 * En cualquier estado no-final: cancelar()
 */
public interface PedidoService {

    // ── CRUD base ────────────────────────────────────────────────────────

    /** Crea un pedido en BORRADOR. No toca stock. */
    PedidoDTO.Response crear(PedidoDTO.Request request, String username);

    /** Solo BORRADOR. Reemplaza detalles completos. */
    PedidoDTO.Response actualizar(Long id, PedidoDTO.Request request, String username);

    /** Solo ADMIN. Solo BORRADOR. */
    void eliminar(Long id);

    // ── Consultas ────────────────────────────────────────────────────────

    PedidoDTO.Response obtenerPorId(Long id);

    PageDTO<PedidoDTO.Response> listar(Pageable pageable);

    PageDTO<PedidoDTO.Response> listarPorEstado(EstadoPedido estado, Pageable pageable);

    PageDTO<PedidoDTO.Response> listarPorColegio(Long colegioId, Pageable pageable);

    List<PedidoDTO.HistorialResponse> obtenerHistorial(Long id);

    // ── Transiciones de estado ───────────────────────────────────────────

    /**
     * Ejecuta la calculadora sobre todas las prendas del pedido.
     * Almacena factorCumplimiento, cantidadMaximaFabricable e insumoLimitante.
     * Transición: BORRADOR | CALCULADO → CALCULADO
     */
    PedidoDTO.Response calcular(Long id, String username);

    /**
     * USER/ADMIN confirma que el pedido está listo para producción.
     * Transición: CALCULADO → CONFIRMADO
     */
    PedidoDTO.Response confirmar(Long id, String username);

    /**
     * Inicia la producción: genera una Salida PENDIENTE con los insumos
     * agregados de todo el pedido. BODEGA deberá confirmarla físicamente.
     * Transición: CONFIRMADO → EN_PRODUCCION
     */
    PedidoDTO.Response iniciarProduccion(Long id, String username);

    /**
     * Marca el pedido como listo para entrega (fabricación terminada).
     * Transición: EN_PRODUCCION → LISTO_PARA_ENTREGA
     */
    PedidoDTO.Response marcarListo(Long id, String username);

    /**
     * Registra la entrega al colegio. Confirma la Salida vinculada → stock descontado.
     * Transición: LISTO_PARA_ENTREGA → ENTREGADO
     */
    PedidoDTO.Response entregar(Long id, String username);

    /**
     * Cancela el pedido. Rechaza la Salida si existía (stock intacto).
     * Válido en cualquier estado no-final.
     * Transición: cualquier no-final → CANCELADO
     */
    PedidoDTO.Response cancelar(Long id, String motivo, String username);
}
