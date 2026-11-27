-- =========================================================================================
-- Migración V7: Endurecimiento del flujo de pagos (idempotencia, conciliación, integridad)
-- =========================================================================================

-- Idempotencia, huella anti-duplicados y control de conciliación
ALTER TABLE gestopago_transacciones
    ADD COLUMN IF NOT EXISTS idempotency_key       VARCHAR(80),
    ADD COLUMN IF NOT EXISTS huella                VARCHAR(64),
    ADD COLUMN IF NOT EXISTS intentos_conciliacion INTEGER   NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fecha_actualizacion   TIMESTAMP NOT NULL DEFAULT NOW();

-- El UPC es el identificador único de la operación ante el proveedor: no puede repetirse
CREATE UNIQUE INDEX IF NOT EXISTS ux_gestopago_tx_upc
    ON gestopago_transacciones (upc);

-- Una misma Idempotency-Key solo puede registrarse una vez por cuenta (la BD es la última barrera)
CREATE UNIQUE INDEX IF NOT EXISTS ux_gestopago_tx_idempotency
    ON gestopago_transacciones (numero_cuenta, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Consultas de la ventana anti-duplicados y del job de conciliación
CREATE INDEX IF NOT EXISTS idx_gestopago_tx_huella
    ON gestopago_transacciones (huella, fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_gestopago_tx_estatus
    ON gestopago_transacciones (estatus, fecha_actualizacion);

-- Defensa en profundidad: el saldo disponible jamás puede quedar negativo
-- (NOT VALID: aplica a nuevas escrituras sin revalidar datos históricos)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_saldo_disponible_no_negativo') THEN
        ALTER TABLE saldos
            ADD CONSTRAINT chk_saldo_disponible_no_negativo CHECK (saldo_disponible >= 0) NOT VALID;
    END IF;
END $$;
