package com.proyecto.servicios.entity.gestopago;

import java.util.List;

/**
 * Máquina de estados de una transacción contra GestoPago.
 *
 * <pre>
 * PROCESANDO ──01──────────────▶ EXITOSA
 *     │ ──rechazo explícito────▶ RECHAZADA (saldo liberado)
 *     └─ timeout / 5xx / 07 / 08 / respuesta ilegible
 *                 ▼
 *        PENDIENTE_CONCILIACION ──(job)──▶ EXITOSA | RECHAZADA | REVISION_MANUAL
 * </pre>
 *
 * En PROCESANDO, PENDIENTE_CONCILIACION y REVISION_MANUAL el monto permanece retenido
 * en saldo disponible: nunca se asume que un timeout equivale a "no procesado".
 */
public enum EstatusTransaccion {
    PROCESANDO,
    EXITOSA,
    RECHAZADA,
    PENDIENTE_CONCILIACION,
    REVISION_MANUAL;

    /** Estados en los que el dinero está retenido y el resultado en el proveedor aún se desconoce. */
    public static final List<String> SIN_RESOLVER = List.of(PROCESANDO.name(), PENDIENTE_CONCILIACION.name());

    /** Estados que bloquean una solicitud equivalente dentro de la ventana anti-duplicados. */
    public static final List<String> BLOQUEANTES = List.of(
            PROCESANDO.name(), PENDIENTE_CONCILIACION.name(), REVISION_MANUAL.name(), EXITOSA.name());
}
