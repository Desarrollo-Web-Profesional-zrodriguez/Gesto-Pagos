package com.proyecto.servicios.exception;

public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(String numeroCuenta) {
        super("No se encontro ninguna cuenta con el numero: " + numeroCuenta);
    }
}
