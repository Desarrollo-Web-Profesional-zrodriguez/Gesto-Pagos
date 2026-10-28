package com.proyecto.servicios.exception;

public class CurpDuplicadaException extends RuntimeException {
    public CurpDuplicadaException(String curp) {
        super("Ya existe un cliente registrado con la CURP: " + curp);
    }
}
