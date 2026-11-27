package com.proyecto.servicios.exception;

import lombok.Getter;

/**
 * Se lanza cuando ya existe una operación equivalente (misma Idempotency-Key o misma huella
 * dentro de la ventana anti-duplicados). Se traduce a HTTP 409.
 */
@Getter
public class SolicitudDuplicadaException extends RuntimeException {

    private final String upcExistente;
    private final String estatusExistente;

    public SolicitudDuplicadaException(String upcExistente, String estatusExistente) {
        super("Ya existe una solicitud equivalente registrada (UPC: " + upcExistente
                + ", estatus: " + estatusExistente + "). Consulte su estatus antes de reintentar");
        this.upcExistente = upcExistente;
        this.estatusExistente = estatusExistente;
    }
}
