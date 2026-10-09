package com.costusoft.inventory_system.module.notificacion.evento;

/**
 * Se publica cuando un pedido pasa a LISTO_PARA_ENTREGA.
 * Lleva solo datos simples: se procesa despues del commit, fuera de la transaccion.
 */
public record PedidoListoEvent(Long pedidoId, String numeroPedido, Long colegioId) {
}
