package com.proyecto.servicios.exception;

import lombok.Getter;

@Getter
public class ReglaNegocioException extends RuntimeException {

    private final CatalogoCodigoError catalogoError;
    private final Integer codigoPersonalizado;
    private final String codigoGestoPago;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
        this.catalogoError = CatalogoCodigoError.REGLA_NEGOCIO_GENERAL;
        this.codigoPersonalizado = CatalogoCodigoError.REGLA_NEGOCIO_GENERAL.getCodigo();
        this.codigoGestoPago = null;
    }

    public ReglaNegocioException(CatalogoCodigoError catalogoError, String mensaje) {
        super(mensaje);
        this.catalogoError = catalogoError != null ? catalogoError : CatalogoCodigoError.REGLA_NEGOCIO_GENERAL;
        this.codigoPersonalizado = this.catalogoError.getCodigo();
        this.codigoGestoPago = this.catalogoError.getCodigoGestoPago();
    }

    private ReglaNegocioException(CatalogoCodigoError catalogoError, Integer codigoNumerico, String codigoGestoPago, String mensaje) {
        super(mensaje);
        this.catalogoError = catalogoError;
        this.codigoPersonalizado = codigoNumerico;
        this.codigoGestoPago = codigoGestoPago;
    }

    /**
     * Construye una excepción usando directamente el código devuelto por GestoPago (ej. "06" -> codigo: 6).
     */
    public static ReglaNegocioException desdeGestoPago(String codigoGp, String textoProveedor) {
        CatalogoCodigoError entradaCatalogo = CatalogoCodigoError.desdeCodigoGestoPago(codigoGp);
        Integer codigoNumerico = entradaCatalogo.getCodigo();
        if (codigoGp != null) {
            try {
                codigoNumerico = Integer.parseInt(codigoGp.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        String detalleLimpio = (textoProveedor != null && !textoProveedor.trim().isEmpty())
                ? textoProveedor.trim().replaceAll("\\s*\\n\\s*", " | ")
                : entradaCatalogo.getDescripcion();
        return new ReglaNegocioException(entradaCatalogo, codigoNumerico, codigoGp != null ? codigoGp.trim() : null, detalleLimpio);
    }
}
