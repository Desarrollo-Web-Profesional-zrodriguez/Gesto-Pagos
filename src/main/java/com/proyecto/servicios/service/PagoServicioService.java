package com.proyecto.servicios.service;

import java.util.List;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioResponse;

public interface PagoServicioService {

    /**
     * Procesa el pago de un servicio o recarga validando cliente activo, saldo disponible,
     * 2FA, existencia de servicio/producto en /productos, cifrado AES-256 y envío a sendTx.do.
     */
    PagoServicioResponse procesarPago(PagoServicioRequest request);

    /**
     * Consulta el historial / evidencia de transacciones de una cuenta específica.
     */
    List<GestoPagoTransaccion> obtenerHistorialPorCuenta(String numeroCuenta);
}
