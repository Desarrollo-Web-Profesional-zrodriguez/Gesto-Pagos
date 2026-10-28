package com.proyecto.servicios.service;

import java.util.List;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.model.gestopago.transaccion.PagoEstatusResponse;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioResponse;

public interface PagoServicioService {

    /**
     * Procesa el pago de un servicio o recarga validando cliente activo, saldo disponible,
     * 2FA, existencia de servicio/producto en /productos, cifrado AES-256 y envío a sendTx.do.
     *
     * @param idempotencyKey clave opcional (cabecera Idempotency-Key) para que un reintento del cliente
     *                       nunca genere un segundo cargo; puede ser null.
     */
    PagoServicioResponse procesarPago(PagoServicioRequest request, String idempotencyKey);

    default PagoServicioResponse procesarPago(PagoServicioRequest request) {
        return procesarPago(request, null);
    }

    /**
     * Consulta el historial / evidencia de transacciones de una cuenta específica.
     */
    List<GestoPagoTransaccion> obtenerHistorialPorCuenta(String numeroCuenta);

    /**
     * Consulta el estatus de una operación por su UPC. Permite al cliente conocer el desenlace de una
     * operación que respondió 202 (pendiente de conciliación) sin tener que reintentarla.
     */
    PagoEstatusResponse consultarEstatus(String upc);
}
