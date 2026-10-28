-- =========================================================================================
-- Migración V4: Catálogo de Nacionalidades en Base de Datos
-- =========================================================================================

CREATE TABLE IF NOT EXISTS cat_nacionalidades (
    id_nacionalidad     BIGSERIAL PRIMARY KEY,
    clave_iso           VARCHAR(3) NOT NULL,
    pais                VARCHAR(100) NOT NULL,
    gentilicio          VARCHAR(100) NOT NULL,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_cat_nacionalidades_clave UNIQUE (clave_iso)
);

CREATE INDEX IF NOT EXISTS idx_cat_nacionalidades_gentilicio ON cat_nacionalidades (gentilicio);

-- Cargar catálogo inicial de nacionalidades
INSERT INTO cat_nacionalidades (id_nacionalidad, clave_iso, pais, gentilicio, activo) VALUES
(1, 'MEX', 'México', 'Mexicana', TRUE),
(2, 'USA', 'Estados Unidos', 'Estadounidense', TRUE),
(3, 'CAN', 'Canadá', 'Canadiense', TRUE),
(4, 'ESP', 'España', 'Española', TRUE),
(5, 'COL', 'Colombia', 'Colombiana', TRUE),
(6, 'ARG', 'Argentina', 'Argentina', TRUE),
(7, 'CHL', 'Chile', 'Chilena', TRUE),
(8, 'PER', 'Perú', 'Peruana', TRUE),
(9, 'VEN', 'Venezuela', 'Venezolana', TRUE),
(10, 'GTM', 'Guatemala', 'Guatemalteca', TRUE),
(11, 'BRA', 'Brasil', 'Brasileña', TRUE),
(12, 'CUB', 'Cuba', 'Cubana', TRUE),
(13, 'FRA', 'Francia', 'Francesa', TRUE),
(14, 'DEU', 'Alemania', 'Alemana', TRUE),
(15, 'ITA', 'Italia', 'Italiana', TRUE)
ON CONFLICT (id_nacionalidad) DO NOTHING;

-- Ajustar secuencia para nuevos registros
SELECT setval(pg_get_serial_sequence('cat_nacionalidades', 'id_nacionalidad'), COALESCE(MAX(id_nacionalidad), 1)) FROM cat_nacionalidades;

-- Asociar columna id_nacionalidad en la tabla clientes
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS id_nacionalidad BIGINT;
UPDATE clientes SET id_nacionalidad = 1 WHERE id_nacionalidad IS NULL;
