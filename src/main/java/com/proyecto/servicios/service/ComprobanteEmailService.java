package com.proyecto.servicios.service;

import com.proyecto.servicios.model.email.ComprobanteEmailResult;
import com.proyecto.servicios.model.email.ComprobantePagoEmailDto;

public interface ComprobanteEmailService {

    /**
     * Envía el comprobante digital de pago a través de Brevo v3 API.
     */
    ComprobanteEmailResult enviarComprobantePago(ComprobantePagoEmailDto dto);

    /**
     * Genera el HTML enriquecido y responsivo del comprobante de pago para el correo.
     */
    String generarHtmlComprobante(ComprobantePagoEmailDto dto);
}
