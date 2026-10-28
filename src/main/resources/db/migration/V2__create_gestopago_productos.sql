CREATE TABLE IF NOT EXISTS gestopago_productos (
    id                      BIGSERIAL PRIMARY KEY,
    id_producto             BIGINT          NOT NULL,
    id_servicio             BIGINT,
    servicio                VARCHAR(150),
    producto                VARCHAR(200),
    precio                  NUMERIC(12, 2),
    id_cat_tipo_servicio    INTEGER,
    tipo_front              INTEGER,
    has_digito_verificador  BOOLEAN         DEFAULT FALSE,
    show_ayuda              BOOLEAN         DEFAULT FALSE,
    tipo_referencia         VARCHAR(50),
    legend                  TEXT,
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_gestopago_producto_id UNIQUE (id_producto)
);

-- Índices para optimizar las consultas por categoría (tipoFront) y por servicio
CREATE INDEX IF NOT EXISTS idx_gestopago_productos_tipo_front ON gestopago_productos (tipo_front);
CREATE INDEX IF NOT EXISTS idx_gestopago_productos_id_servicio ON gestopago_productos (id_servicio);
