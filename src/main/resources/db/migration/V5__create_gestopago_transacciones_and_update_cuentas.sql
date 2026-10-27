-- =========================================================================================
-- Migración V5: Normalización de cuentas a 16 dígitos y tabla de evidencia de transacciones
-- =========================================================================================

-- 1. Ajustar cuentas existentes que tengan menos de 16 dígitos para cumplir el estándar de 16 dígitos
UPDATE cuentas
SET numero_cuenta = RPAD(numero_cuenta, 16, '0'),
    fecha_modificacion = NOW()
WHERE LENGTH(TRIM(numero_cuenta)) < 16;

-- 2. Tabla de bitácora / evidencia de transacciones enviadas a GestoPago (sendTx)
CREATE TABLE IF NOT EXISTS gestopago_transacciones (
    id_transaccion_local    BIGSERIAL PRIMARY KEY,
    upc                     VARCHAR(20)     NOT NULL,
    numero_cuenta           VARCHAR(16)     NOT NULL,
    id_servicio             BIGINT          NOT NULL,
    id_producto             BIGINT          NOT NULL,
    telefono                VARCHAR(20)     NOT NULL,
    referencia              VARCHAR(100)    NOT NULL,
    monto                   NUMERIC(12, 2)  NOT NULL,
    hora_local              VARCHAR(15)     NOT NULL,
    unidad                  VARCHAR(25)     NOT NULL,
    payload_json            TEXT,
    payload_cifrado         TEXT,
    response_xml            TEXT,
    codigo_respuesta        VARCHAR(10),
    mensaje_respuesta       TEXT,
    id_tx_gestopago         VARCHAR(50),
    num_autorizacion        VARCHAR(60),
    comision                NUMERIC(12, 2),
    pin                      VARCHAR(250),
    legend                  TEXT,
    estatus                 VARCHAR(30)     NOT NULL,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gestopago_tx_upc ON gestopago_transacciones (upc);
CREATE INDEX IF NOT EXISTS idx_gestopago_tx_cuenta ON gestopago_transacciones (numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_gestopago_tx_fecha ON gestopago_transacciones (fecha_creacion);
