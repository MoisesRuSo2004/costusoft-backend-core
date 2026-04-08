package com.costusoft.inventory_system.entity;

/**
 * Ciclo de vida de un pedido de uniformes.
 *
 * Transiciones válidas:
 *
 *   BORRADOR ──────────────→ CALCULADO ──────────────→ CONFIRMADO
 *      │                         │                          │
 *      └──────────────────────────────────────────────→ CANCELADO
 *
 *   CONFIRMADO ──→ EN_PRODUCCION ──→ LISTO_PARA_ENTREGA ──→ ENTREGADO
 *       │               │                    │
 *       └───────────────┴────────────────────┘
 *                       ↓
 *                   CANCELADO
 *
 * Reglas de stock:
 *   - BORRADOR..CONFIRMADO  → sin efecto en inventario
 *   - EN_PRODUCCION         → crea Salida PENDIENTE (stock reservado, no descontado)
 *   - ENTREGADO             → confirma Salida → stock decrementado
 *   - CANCELADO             → rechaza Salida si existía → stock intacto
 */
public enum EstadoPedido {

    /** Pedido en construcción. Editable libremente. */
    BORRADOR,

    /**
     * Calculadora ejecutada. Se almacenaron factorCumplimiento,
     * cantidadMaximaFabricable e insumoLimitante.
     */
    CALCULADO,

    /**
     * ADMIN/USER confirmó el pedido. Listo para iniciar producción.
     * Ya no es editable.
     */
    CONFIRMADO,

    /**
     * Producción iniciada. Se generó una Salida PENDIENTE con todos
     * los insumos necesarios. BODEGA debe confirmarla físicamente.
     */
    EN_PRODUCCION,

    /** Fabricación terminada. En espera de entrega al colegio. */
    LISTO_PARA_ENTREGA,

    /**
     * Entregado al colegio. La Salida fue confirmada por BODEGA
     * y el stock fue descontado. Estado final.
     */
    ENTREGADO,

    /**
     * Pedido cancelado. La Salida fue rechazada (si existía),
     * sin afectar el inventario. Estado final.
     */
    CANCELADO
}
