package com.costusoft.inventory_system.module.notificacion.evento;

/**
 * Se publica al registrar una entrada o salida (queda PENDIENTE hasta que
 * Bodega la confirma). Lleva solo datos simples: se procesa despues del
 * commit, fuera de la transaccion.
 *
 * @param tipo      "ENTRADA" o "SALIDA"
 * @param tercero   proveedor (entrada) o colegio (salida); puede ser null
 * @param renglones cantidad de insumos distintos del movimiento
 */
public record MovimientoPendienteEvent(String tipo, Long movimientoId, String tercero, int renglones) {
}
