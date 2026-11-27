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
        String codigoLimpio = (codigoGp != null) ? codigoGp.trim() : null;
        CatalogoCodigoError entradaCatalogo = CatalogoCodigoError.desdeCodigoGestoPago(codigoLimpio);
        if ("55".equals(codigoLimpio)) {
            boolean esErrorReferencia = textoProveedor != null
                    && textoProveedor.toLowerCase().contains("referencia");
            if (esErrorReferencia) {
                entradaCatalogo = CatalogoCodigoError.GESTOPAGO_55_REFERENCIA_INVALIDA;
            } else {
                entradaCatalogo = CatalogoCodigoError.GESTOPAGO_55_ORIGEN_NO_AUTORIZADO;
            }
        }
        Integer codigoNumerico = entradaCatalogo.getCodigo();
        if (codigoLimpio != null) {
            try {
                codigoNumerico = Integer.parseInt(codigoLimpio);
            } catch (NumberFormatException ignored) {
                // Conservar el codigo del catalogo si no es un entero directo
            }
        }
        return new ReglaNegocioException(entradaCatalogo, codigoNumerico, codigoLimpio,
                entradaCatalogo.getDescripcion());
    }
}
