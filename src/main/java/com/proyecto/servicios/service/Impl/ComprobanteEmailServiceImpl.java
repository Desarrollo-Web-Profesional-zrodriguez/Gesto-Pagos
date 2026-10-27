package com.proyecto.servicios.service.Impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.proyecto.servicios.client.BrevoEmailClient;
import com.proyecto.servicios.model.email.BrevoEmailContact;
import com.proyecto.servicios.model.email.BrevoSendEmailRequest;
import com.proyecto.servicios.model.email.BrevoSendEmailResponse;
import com.proyecto.servicios.model.email.ComprobanteEmailResult;
import com.proyecto.servicios.model.email.ComprobantePagoEmailDto;
import com.proyecto.servicios.service.ComprobanteEmailService;
import com.proyecto.servicios.util.LogoServicioResolver;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComprobanteEmailServiceImpl implements ComprobanteEmailService {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

    private final BrevoEmailClient brevoEmailClient;
    private final LogoServicioResolver logoServicioResolver;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.name:Comprobante de Pago Digital}")
    private String senderName;

    @Value("${brevo.sender.email:zahir.rodriguez.per@gmail.com}")
    private String senderEmail;

    @Override
    public ComprobanteEmailResult enviarComprobantePago(ComprobantePagoEmailDto dto) {
        String destinatario = dto.getDestinatarioEmail();
        if (destinatario == null || destinatario.trim().isEmpty()) {
            log.warn("[Brevo]: No se proporciono correo electronico de destino para la transaccion UPC={}. No se envia comprobante.",
                    dto.getUpc());
            return ComprobanteEmailResult.builder()
                    .enviado(false)
                    .mensaje("No hay correo electronico configurado para el destinatario")
                    .build();
        }

        // Asegurar que el logo este resuelto
        if (dto.getLogoUrl() == null || dto.getLogoUrl().trim().isEmpty()) {
            dto.setLogoUrl(logoServicioResolver.resolverUrlLogo(dto.getServicio(), dto.getProducto(), dto.getIdServicio(), dto.getIdProducto()));
        }

        String htmlContent = generarHtmlComprobante(dto);
        String subject = String.format("Comprobante de Pago Exitoso - %s (Aut: %s)",
                dto.getServicio() != null ? dto.getServicio() : "Servicio",
                dto.getNumAutorizacion() != null ? dto.getNumAutorizacion() : "N/A");

        BrevoSendEmailRequest request = BrevoSendEmailRequest.builder()
                .sender(BrevoEmailContact.builder()
                        .name(senderName)
                        .email(senderEmail)
                        .build())
                .to(List.of(BrevoEmailContact.builder()
                        .name(dto.getDestinatarioNombre() != null ? dto.getDestinatarioNombre() : "Estimado Cliente")
                        .email(destinatario.trim())
                        .build()))
                .subject(subject)
                .htmlContent(htmlContent)
                .build();

        try {
            log.info("[Brevo]: Enviando comprobante de pago por correo a {} para la transaccion UPC={}, Servicio={}",
                    destinatario, dto.getUpc(), dto.getServicio());

            BrevoSendEmailResponse response = brevoEmailClient.sendTransacEmail(brevoApiKey, request);
            String messageId = response != null ? response.getMessageId() : "ENVIADO_SIN_ID";

            log.info("[Brevo]: Comprobante enviado exitosamente a {} con messageId={}", destinatario, messageId);

            return ComprobanteEmailResult.builder()
                    .enviado(true)
                    .messageId(messageId)
                    .mensaje("Comprobante enviado exitosamente al correo " + destinatario)
                    .build();

        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            log.error("[Brevo ERROR]: Error al enviar comprobante con Brevo (HTTP {}): {}", e.status(), errorBody);

            String detalleError = errorBody;
            if (errorBody != null && errorBody.contains("unrecognised IP address")) {
                detalleError = "IP no autorizada en Brevo. Requiere registrar la IP publica en https://app.brevo.com/security/authorised_ips";
            }

            return ComprobanteEmailResult.builder()
                    .enviado(false)
                    .mensaje("Error al enviar comprobante con Brevo (HTTP " + e.status() + ")")
                    .errorDetalle(detalleError)
                    .build();

        } catch (Exception e) {
            log.error("[Brevo ERROR]: Error inesperado al invocar Brevo: {}", e.getMessage(), e);
            return ComprobanteEmailResult.builder()
                    .enviado(false)
                    .mensaje("Error inesperado al enviar comprobante: " + e.getMessage())
                    .errorDetalle(e.getMessage())
                    .build();
        }
    }

    @Override
    public String generarHtmlComprobante(ComprobantePagoEmailDto dto) {
        String logoUrl = dto.getLogoUrl() != null && !dto.getLogoUrl().trim().isEmpty()
                ? dto.getLogoUrl()
                : LogoServicioResolver.LOGO_DEFAULT_SERVICIO;

        String montoFormateado = dto.getMontoPagado() != null
                ? CURRENCY_FORMAT.format(dto.getMontoPagado())
                : "0.00";

        String comisionFormateada = dto.getComision() != null && dto.getComision().compareTo(BigDecimal.ZERO) > 0
                ? CURRENCY_FORMAT.format(dto.getComision())
                : null;

        String cuentaEnmascarada = enmascararCuenta(dto.getNumCuenta());
        String fecha = dto.getFechaProveedor() != null ? dto.getFechaProveedor() : dto.getHoraLocal();

        boolean esTelefonico = esServicioTelefonico(dto.getTipoFront(), dto.getServicio(), dto.getProducto());

        // Generar filas para el identificador (Telefono / Referencia segun aplique)
        StringBuilder filasIdentificador = new StringBuilder();

        if (esTelefonico) {
            // Escenario 1: Recarga o pago de telefonía móvil -> El identificador principal es el Teléfono
            filasIdentificador.append("""
                <tr>
                    <td style="padding: 10px 0; color: #64748b; font-size: 14px; border-bottom: 1px solid #f1f5f9;">Numero de Telefono:</td>
                    <td style="padding: 10px 0; color: #0f172a; font-size: 14px; font-weight: 700; text-align: right; border-bottom: 1px solid #f1f5f9;">%s</td>
                </tr>
            """.formatted(dto.getTelefono() != null ? dto.getTelefono() : "No especificado"));

            if (dto.getReferencia() != null && !dto.getReferencia().trim().isEmpty()
                    && !dto.getReferencia().equals(dto.getTelefono()) && !"0".equals(dto.getReferencia())) {
                filasIdentificador.append("""
                    <tr>
                        <td style="padding: 10px 0; color: #64748b; font-size: 14px; border-bottom: 1px solid #f1f5f9;">Folio / Referencia:</td>
                        <td style="padding: 10px 0; color: #0f172a; font-size: 14px; font-weight: 600; text-align: right; border-bottom: 1px solid #f1f5f9;">%s</td>
                    </tr>
                """.formatted(dto.getReferencia()));
            }
        } else {
            // Escenario 2: Pago de servicios residenciales/empresariales (Luz, Internet, TV, Agua, Gas) -> El identificador principal es la Referencia
            filasIdentificador.append("""
                <tr>
                    <td style="padding: 10px 0; color: #64748b; font-size: 14px; border-bottom: 1px solid #f1f5f9;">Referencia de Pago:</td>
                    <td style="padding: 10px 0; color: #0f172a; font-size: 14px; font-weight: 700; text-align: right; border-bottom: 1px solid #f1f5f9;">%s</td>
                </tr>
            """.formatted(dto.getReferencia() != null ? dto.getReferencia() : "No especificada"));

            if (dto.getTelefono() != null && !dto.getTelefono().trim().isEmpty() && !"0".equals(dto.getTelefono())) {
                filasIdentificador.append("""
                    <tr>
                        <td style="padding: 10px 0; color: #64748b; font-size: 14px; border-bottom: 1px solid #f1f5f9;">Telefono Asociado:</td>
                        <td style="padding: 10px 0; color: #0f172a; font-size: 14px; font-weight: 600; text-align: right; border-bottom: 1px solid #f1f5f9;">%s</td>
                    </tr>
                """.formatted(dto.getTelefono()));
            }
        }

        String filaComision = comisionFormateada != null ? """
            <tr>
                <td style="padding: 10px 0; color: #64748b; font-size: 14px; border-bottom: 1px solid #f1f5f9;">Comision de Servicio:</td>
                <td style="padding: 10px 0; color: #0f172a; font-size: 14px; font-weight: 600; text-align: right; border-bottom: 1px solid #f1f5f9;">$%s MXN</td>
            </tr>
        """.formatted(comisionFormateada) : "";

        String seccionLegend = (dto.getLegend() != null && !dto.getLegend().trim().isEmpty()) ? """
            <div style="margin-top: 24px; padding: 16px; background-color: #f8fafc; border-left: 4px solid #3b82f6; border-radius: 6px;">
                <p style="margin: 0 0 6px 0; color: #1e293b; font-size: 13px; font-weight: 700;">Aclaraciones y Soporte:</p>
                <p style="margin: 0; color: #475569; font-size: 12px; line-height: 1.5;">%s</p>
            </div>
        """.formatted(dto.getLegend()) : "";

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Comprobante de Pago Digital</title>
            </head>
            <body style="margin: 0; padding: 20px; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);">
                    <!-- Encabezado con degradado moderno -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #1e293b 0%%, #0f172a 100%%); padding: 32px 24px; text-align: center;">
                            <div style="background-color: #ffffff; width: 72px; height: 72px; margin: 0 auto 16px auto; border-radius: 14px; padding: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.15); display: flex; align-items: center; justify-content: center;">
                                <img src="%s" alt="%s" style="max-width: 56px; max-height: 56px; object-fit: contain; vertical-align: middle;" />
                            </div>
                            <h1 style="margin: 0; color: #ffffff; font-size: 22px; font-weight: 700; letter-spacing: -0.5px;">Comprobante de Pago</h1>
                            <p style="margin: 6px 0 0 0; color: #94a3b8; font-size: 13px;">Transaccion aplicada exitosamente</p>
                            <div style="margin-top: 14px; display: inline-block; background-color: rgba(16, 185, 129, 0.2); border: 1px solid #10b981; color: #34d399; font-size: 12px; font-weight: 700; padding: 4px 14px; border-radius: 20px;">
                                &#10003; PAGO EXITOSO
                            </div>
                        </td>
                    </tr>

                    <!-- Contenedor del Monto Principal -->
                    <tr>
                        <td style="padding: 24px; text-align: center; background-color: #f8fafc; border-bottom: 1px solid #e2e8f0;">
                            <span style="color: #64748b; font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px;">Monto Total Pagado</span>
                            <div style="color: #0f172a; font-size: 36px; font-weight: 800; margin: 6px 0;">$%s <span style="font-size: 18px; font-weight: 600; color: #64748b;">MXN</span></div>
                            <span style="color: #059669; font-size: 13px; font-weight: 600;">Cargo aplicado a cuenta %s</span>
                        </td>
                    </tr>

                    <!-- Detalle de la Transacción -->
                    <tr>
                        <td style="padding: 24px;">
                            <h3 style="margin: 0 0 16px 0; color: #1e293b; font-size: 15px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px;">
                                Detalle de la Operacion
                            </h3>
                            <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td style="padding: 10px 0; color: #64748b; font-size: 14px; border-bottom: 1px solid #f1f5f9;">Numero de Autorizacion:</td>
                                    <td style="padding: 10px 0; color: #0284c7; font-size: 15px; font-weight: 800; text-align: right; border-bottom: 1px solid #f1f5f9;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 10px 0; color: #64748b; font-size: 14px; border-bottom: 1px solid #f1f5f9;">Servicio:</td>
                                    <td style="padding: 10px 0; color: #0f172a; font-size: 14px; font-weight: 700; text-align: right; border-bottom: 1px solid #f1f5f9;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 10px 0; color: #64748b; font-size: 14px; border-bottom: 1px solid #f1f5f9;">Producto:</td>
                                    <td style="padding: 10px 0; color: #0f172a; font-size: 14px; font-weight: 600; text-align: right; border-bottom: 1px solid #f1f5f9;">%s</td>
                                </tr>
                                %s
                                %s
                                <tr>
                                    <td style="padding: 10px 0; color: #64748b; font-size: 14px; border-bottom: 1px solid #f1f5f9;">Fecha y Hora:</td>
                                    <td style="padding: 10px 0; color: #0f172a; font-size: 14px; font-weight: 600; text-align: right; border-bottom: 1px solid #f1f5f9;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 10px 0; color: #64748b; font-size: 14px; border-bottom: 1px solid #f1f5f9;">Folio UPC:</td>
                                    <td style="padding: 10px 0; color: #475569; font-size: 13px; font-family: monospace; font-weight: 600; text-align: right; border-bottom: 1px solid #f1f5f9;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 10px 0; color: #64748b; font-size: 14px;">ID Transaccion (Proveedor):</td>
                                    <td style="padding: 10px 0; color: #475569; font-size: 13px; font-family: monospace; font-weight: 600; text-align: right;">%s</td>
                                </tr>
                            </table>

                            %s
                        </td>
                    </tr>

                    <!-- Pie de Pagina -->
                    <tr>
                        <td style="background-color: #0f172a; padding: 20px 24px; text-align: center; color: #94a3b8; font-size: 12px; line-height: 1.6;">
                            <p style="margin: 0 0 6px 0; color: #cbd5e1; font-weight: 600;">Comprobante Digital Oficial con Validez Legal</p>
                            <p style="margin: 0 0 6px 0;">Este correo es una notificacion automatica de tu pago electronico a traves de Brevo.</p>
                            <p style="margin: 0; color: #64748b;">Conserva este comprobante para cualquier duda o aclaracion con el proveedor del servicio.</p>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.formatted(
                logoUrl,
                dto.getServicio() != null ? dto.getServicio() : "Servicio",
                montoFormateado,
                cuentaEnmascarada,
                dto.getNumAutorizacion() != null ? dto.getNumAutorizacion() : "N/A",
                dto.getServicio() != null ? dto.getServicio() : "N/A",
                dto.getProducto() != null ? dto.getProducto() : "N/A",
                filasIdentificador.toString(),
                filaComision,
                fecha != null ? fecha : "N/A",
                dto.getUpc() != null ? dto.getUpc() : "N/A",
                dto.getIdTx() != null ? dto.getIdTx() : "N/A",
                seccionLegend
        );
    }

    private String enmascararCuenta(String numCuenta) {
        if (numCuenta == null || numCuenta.length() < 4) {
            return "****";
        }
        String clean = numCuenta.trim();
        return "**** **** **** " + clean.substring(clean.length() - 4);
    }

    public static boolean esServicioTelefonico(Integer tipoFront, String servicio, String producto) {
        if (tipoFront != null && tipoFront == 1) {
            return true;
        }
        String texto = ((servicio != null ? servicio : "") + " " + (producto != null ? producto : "")).toUpperCase();
        return texto.contains("TELCEL")
                || texto.contains("MOVISTAR")
                || texto.contains("AT&T")
                || texto.contains("ATT")
                || texto.contains("UNEFON")
                || texto.contains("BAIT")
                || texto.contains("VIRGIN")
                || texto.contains("RECARGA")
                || texto.contains("PAQUETE");
    }
}
