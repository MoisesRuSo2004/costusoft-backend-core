-- ============================================================
-- V4 — Talla a nivel de insumo requerido (uniforme_insumos)
--
-- Motivación:
--   Cada prenda (Uniforme) puede tener insumos con cantidades
--   distintas por talla (S, M, L, XL, 06-08, 10-12, 14-16…).
--   Antes: talla vivía en "uniformes" (1 registro por talla).
--   Ahora: Uniforme = tipo de prenda. UniformeInsumo = insumos por talla.
-- ============================================================

-- 1. Agregar columna talla a uniforme_insumos
ALTER TABLE uniforme_insumos
    ADD COLUMN talla VARCHAR(10) NOT NULL DEFAULT 'UNICA';

-- 2. Eliminar unique constraint viejo (uniforme_id, insumo_id)
ALTER TABLE uniforme_insumos
    DROP CONSTRAINT IF EXISTS uk_uniforme_insumo;

-- 3. Nuevo unique constraint que incluye talla
ALTER TABLE uniforme_insumos
    ADD CONSTRAINT uk_uniforme_insumo_talla
    UNIQUE (uniforme_id, insumo_id, talla);

-- 4. Índice para búsquedas calculadora: insumos de una prenda por talla
CREATE INDEX IF NOT EXISTS idx_ui_uniforme_talla
    ON uniforme_insumos (uniforme_id, talla);

-- 5. La columna talla en uniformes ya no aplica (Uniforme = tipo de prenda)
--    Se elimina para que ddl-auto=validate en prod no falle
ALTER TABLE uniformes DROP COLUMN IF EXISTS talla;
