package com.proyecto.servicios.exception;

import lombok.Getter;

@Getter
public class AutenticacionException extends RuntimeException {

    private final CatalogoCodigoError catalogoError;

    public AutenticacionException(String mensaje) {
        super(mensaje);
        this.catalogoError = CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA;
    }

    public AutenticacionException(CatalogoCodigoError catalogoError, String mensaje) {
        super(mensaje);
        this.catalogoError = catalogoError != null ? catalogoError : CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA;
    }
}
