-- ============================================================
-- V2 — Implementación del rol BODEGA
-- Agrega ciclo de vida (estado) a entradas y salidas.
-- Compatible con Supabase / Transaction Pooler (PostgreSQL 16)
--
-- Los registros existentes reciben CONFIRMADA como default
-- porque ya tienen su stock aplicado antes de esta migración.
-- ============================================================

-- ── Tabla: entradas ─────────────────────────────────────────

ALTER TABLE entradas
    ADD COLUMN IF NOT EXISTS estado          VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMADA',
    ADD COLUMN IF NOT EXISTS confirmada_por  VARCHAR(50)  NULL,
    ADD COLUMN IF NOT EXISTS motivo_rechazo  VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS confirmada_at   TIMESTAMP    NULL;

-- Índice para que BODEGA filtre rápido por estado
CREATE INDEX IF NOT EXISTS idx_entradas_estado ON entradas (estado);

-- ── Tabla: salidas ──────────────────────────────────────────

ALTER TABLE salidas
    ADD COLUMN IF NOT EXISTS estado          VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMADA',
    ADD COLUMN IF NOT EXISTS confirmada_por  VARCHAR(50)  NULL,
    ADD COLUMN IF NOT EXISTS motivo_rechazo  VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS confirmada_at   TIMESTAMP    NULL;

-- Índice para que BODEGA filtre rápido por estado
CREATE INDEX IF NOT EXISTS idx_salidas_estado ON salidas (estado);

-- ── Comentario informativo ──────────────────────────────────
COMMENT ON COLUMN entradas.estado IS 'PENDIENTE = solicitud creada (stock intacto) | CONFIRMADA = aprobada por BODEGA/ADMIN (stock actualizado) | RECHAZADA = rechazada por BODEGA/ADMIN (stock intacto)';
COMMENT ON COLUMN salidas.estado  IS 'PENDIENTE = solicitud creada (stock intacto) | CONFIRMADA = aprobada por BODEGA/ADMIN (stock descontado) | RECHAZADA = rechazada por BODEGA/ADMIN (stock intacto)';
