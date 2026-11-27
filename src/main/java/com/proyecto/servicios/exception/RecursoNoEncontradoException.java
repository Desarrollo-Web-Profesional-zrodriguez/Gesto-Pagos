package com.proyecto.servicios.exception;

import lombok.Getter;

@Getter
public class RecursoNoEncontradoException extends RuntimeException {

    private final CatalogoCodigoError catalogoError;

    public RecursoNoEncontradoException(String message) {
        super(message);
        this.catalogoError = CatalogoCodigoError.SERVICIO_NO_EXISTE;
    }

    public RecursoNoEncontradoException(CatalogoCodigoError catalogoError, String message) {
        super(message);
        this.catalogoError = catalogoError != null ? catalogoError : CatalogoCodigoError.SERVICIO_NO_EXISTE;
    }
}
