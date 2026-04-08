-- ============================================================
-- V3 — Módulo de Pedidos
-- Introduce la gestión de pedidos de uniformes por colegio.
-- El colegio actúa como cliente — no existe entidad Cliente.
--
-- Tablas:
--   pedidos          → cabecera del pedido
--   detalle_pedidos  → prendas del pedido (uniforme + cantidad)
--   pedido_historial → auditoría de transiciones de estado
--
-- Compatible con Supabase / Transaction Pooler (PostgreSQL 16)
-- ============================================================

-- ── Tabla: pedidos ───────────────────────────────────────────

CREATE TABLE IF NOT EXISTS pedidos (
    id                      BIGSERIAL    PRIMARY KEY,
    numero_pedido           VARCHAR(20)  UNIQUE,
    estado                  VARCHAR(25)  NOT NULL DEFAULT 'BORRADOR',

    fecha_estimada_entrega  DATE         NULL,
    observaciones           VARCHAR(500) NULL,

    -- Resultado del último calcular()
    factor_cumplimiento     NUMERIC(7,4) NULL,
    disponible_completo     BOOLEAN      NULL,
    insumo_limitante        VARCHAR(100) NULL,

    -- Relaciones
    colegio_id              BIGINT       NOT NULL REFERENCES colegios(id),
    creado_por              VARCHAR(50)  NOT NULL,
    salida_id               BIGINT       NULL     REFERENCES salidas(id),

    -- Auditoría
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  pedidos IS 'Pedidos de fabricación de uniformes por colegio';
COMMENT ON COLUMN pedidos.estado IS 'BORRADOR | CALCULADO | CONFIRMADO | EN_PRODUCCION | LISTO_PARA_ENTREGA | ENTREGADO | CANCELADO';
COMMENT ON COLUMN pedidos.factor_cumplimiento IS 'Factor 0.0–1.0. Poblado tras ejecutar calcular(). 1.0 = pedido 100% atendible';
COMMENT ON COLUMN pedidos.salida_id IS 'Salida PENDIENTE generada al iniciar producción. Se confirma al entregar';

-- ── Tabla: detalle_pedidos ───────────────────────────────────

CREATE TABLE IF NOT EXISTS detalle_pedidos (
    id                          BIGSERIAL    PRIMARY KEY,
    pedido_id                   BIGINT       NOT NULL REFERENCES pedidos(id),
    uniforme_id                 BIGINT       NOT NULL REFERENCES uniformes(id),
    cantidad                    INTEGER      NOT NULL CHECK (cantidad >= 1 AND cantidad <= 10000),

    -- Talla solicitada por el usuario al agregar la prenda al pedido
    talla                       VARCHAR(10)  NULL,

    -- Resultado del calcular() — null hasta entonces
    cantidad_maxima_fabricable  INTEGER      NULL,
    disponible_individual       BOOLEAN      NULL,

    -- Snapshot histórico
    nombre_uniforme_snapshot    VARCHAR(150) NULL,

    -- Auditoría
    created_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  detalle_pedidos IS 'Prendas incluidas en un pedido con su cantidad solicitada';
COMMENT ON COLUMN detalle_pedidos.cantidad_maxima_fabricable IS 'Máx fabricable para esta prenda considerando el factor global del pedido';

-- ── Tabla: pedido_historial ──────────────────────────────────

CREATE TABLE IF NOT EXISTS pedido_historial (
    id              BIGSERIAL    PRIMARY KEY,
    pedido_id       BIGINT       NOT NULL REFERENCES pedidos(id),
    estado_anterior VARCHAR(30)  NULL,
    estado_nuevo    VARCHAR(30)  NOT NULL,
    accion          VARCHAR(200) NOT NULL,
    observacion     VARCHAR(500) NULL,
    realizado_por   VARCHAR(50)  NOT NULL,
    fecha_accion    TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE pedido_historial IS 'Auditoría de todas las transiciones de estado de un pedido';

-- ── Índices de rendimiento ───────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_pedidos_estado          ON pedidos (estado);
CREATE INDEX IF NOT EXISTS idx_pedidos_colegio         ON pedidos (colegio_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_numero          ON pedidos (numero_pedido);
CREATE INDEX IF NOT EXISTS idx_pedidos_created         ON pedidos (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_detalle_pedidos_pedido  ON detalle_pedidos (pedido_id);
CREATE INDEX IF NOT EXISTS idx_detalle_pedidos_uniforme ON detalle_pedidos (uniforme_id);

CREATE INDEX IF NOT EXISTS idx_historial_pedido        ON pedido_historial (pedido_id);
CREATE INDEX IF NOT EXISTS idx_historial_fecha         ON pedido_historial (fecha_accion DESC);
