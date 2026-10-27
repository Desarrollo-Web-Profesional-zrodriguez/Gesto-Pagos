package com.proyecto.servicios.model.gestopago.transaccion;

import com.proyecto.servicios.exception.CatalogoCodigoError;

/**
 * Resultado clasificado de una invocación a sendTx.do.
 *
 * @param veredicto    decisión financiera derivada de la respuesta
 * @param codigo       CODIGO devuelto por el proveedor (null si nunca hubo respuesta interpretable)
 * @param texto        TEXTO devuelto por el proveedor (solo para bitácora, no se expone al cliente)
 * @param respuestaRaw cuerpo crudo recibido (evidencia)
 * @param parsed       XML ya deserializado (null si no fue posible)
 * @param errorInterno causa técnica cuando la solicitud NO llegó a procesarse (token, HTTP 4xx); null si el proveedor respondió
 */
public record RespuestaSendTx(
        Veredicto veredicto,
        String codigo,
        String texto,
        String respuestaRaw,
        SendTxXmlResponse parsed,
        CatalogoCodigoError errorInterno) {

    public enum Veredicto {
        /** CODIGO 01: el proveedor aplicó la operación. */
        EXITO,
        /** El proveedor NO aplicó la operación (rechazo explícito o la solicitud ni siquiera fue aceptada): es seguro liberar el saldo. */
        RECHAZADO,
        /** No se puede saber si el proveedor aplicó la operación (timeout, 5xx, respuesta ilegible, 07/08): NO liberar ni reintentar a ciegas. */
        INDETERMINADO
    }

    public boolean esRechazoExplicitoDelProveedor() {
        return veredicto == Veredicto.RECHAZADO && errorInterno == null && parsed != null;
    }
}
