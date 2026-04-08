package com.costusoft.inventory_system.entity;

/**
 * Estado del ciclo de vida de un movimiento (Entrada o Salida).
 *
 * Flujo:
 *   PENDIENTE → CONFIRMADA  (BODEGA/ADMIN verifica físicamente y aprueba → stock actualizado)
 *   PENDIENTE → RECHAZADA   (BODEGA/ADMIN rechaza con motivo → stock sin cambios)
 *
 * Stock solo se modifica al CONFIRMAR, nunca al crear la solicitud.
 */
public enum EstadoMovimiento {

    /** Solicitud creada por USER/ADMIN. Stock intacto. */
    PENDIENTE,

    /** BODEGA/ADMIN verificó y aprobó. Stock actualizado. */
    CONFIRMADA,

    /** BODEGA/ADMIN rechazó con motivo. Stock sin cambios. */
    RECHAZADA
}
