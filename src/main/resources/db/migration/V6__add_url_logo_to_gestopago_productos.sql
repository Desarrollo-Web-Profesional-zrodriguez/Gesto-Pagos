-- =========================================================================================
-- Migración V6: Agregar columna url_logo a la tabla gestopago_productos
-- =========================================================================================

ALTER TABLE gestopago_productos ADD COLUMN IF NOT EXISTS url_logo VARCHAR(500);
