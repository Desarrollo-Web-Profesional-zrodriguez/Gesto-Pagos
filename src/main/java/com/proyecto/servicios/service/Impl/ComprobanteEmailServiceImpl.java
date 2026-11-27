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
import com.proyecto.servicios.util.EmailTemplateBuilder;
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

    @Value("${brevo.sender.name:GestoPagos · Comprobantes}")
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

        if (dto.getLogoUrl() == null || dto.getLogoUrl().trim().isEmpty()) {
            dto.setLogoUrl(logoServicioResolver.resolverUrlLogo(dto.getServicio(), dto.getProducto(), dto.getIdServicio(), dto.getIdProducto()));
        }

        String htmlContent = generarHtmlComprobante(dto);
        String subject = String.format("Comprobante GestoPagos · %s ($%s MXN · Aut: %s)",
                dto.getServicio() != null ? dto.getServicio() : "Servicio",
                formatearMonto(dto.getMontoPagado()),
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
        String logoUrl = EmailTemplateBuilder.escapeHtml(resolverLogoUrl(dto.getLogoUrl()));
        String servicio = EmailTemplateBuilder.escapeHtml(dto.getServicio() != null ? dto.getServicio() : "N/A");
        String producto = EmailTemplateBuilder.escapeHtml(dto.getProducto() != null ? dto.getProducto() : "N/A");
        String titular = EmailTemplateBuilder.escapeHtml(
                dto.getDestinatarioNombre() != null && !dto.getDestinatarioNombre().isBlank()
                        ? dto.getDestinatarioNombre()
                        : "Estimado Cliente");
        String montoFormateado = formatearMonto(dto.getMontoPagado());
        String comisionFormateada = formatearComision(dto.getComision());
        String cuentaEnmascarada = EmailTemplateBuilder.escapeHtml(enmascararCuenta(dto.getNumCuenta()));
        String fechaRaw = dto.getFechaProveedor() != null ? dto.getFechaProveedor() : dto.getHoraLocal();
        String fecha = EmailTemplateBuilder.escapeHtml(fechaRaw != null ? fechaRaw : "N/A");
        String autorizacion = EmailTemplateBuilder.escapeHtml(dto.getNumAutorizacion() != null ? dto.getNumAutorizacion() : "N/A");
        String upc = EmailTemplateBuilder.escapeHtml(dto.getUpc() != null ? dto.getUpc() : "N/A");
        String idTx = EmailTemplateBuilder.escapeHtml(dto.getIdTx() != null ? dto.getIdTx() : "N/A");

        boolean esTelefonico = esServicioTelefonico(dto.getTipoFront(), dto.getServicio(), dto.getProducto());
        String filasIdentificador = construirFilasIdentificador(dto, esTelefonico);
        String filaComision = construirFilaComision(comisionFormateada);
        String seccionPin = construirSeccionPin(dto.getPin());
        String seccionLegend = construirSeccionLegend(dto.getLegend());
        String preheader = "Pago exitoso de " + servicio + " por $" + montoFormateado + " MXN · Aut: " + autorizacion;

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Comprobante de Pago Digital · GestoPagos</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;-webkit-font-smoothing:antialiased;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">%s</div>
                <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f1f5f9;padding:28px 12px;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width:600px;background-color:#ffffff;border-radius:22px;overflow:hidden;border:1px solid #e2e8f0;box-shadow:0 12px 32px rgba(15,23,42,0.08);">
                                <!-- Encabezado Bancario -->
                                <tr>
                                    <td style="background:linear-gradient(135deg,#0f172a 0%%,#0c4a6e 100%%);padding:28px 24px 26px 24px;text-align:center;">
                                        <div style="font-size:20px;font-weight:800;letter-spacing:-0.5px;color:#ffffff;margin-bottom:16px;">
                                            Gesto<span style="color:#38bdf8;">Pagos</span>
                                        </div>
                                        <table role="presentation" border="0" cellspacing="0" cellpadding="0" align="center" style="margin:0 auto 14px auto;">
                                            <tr>
                                                <td align="center" valign="middle" style="width:72px;height:72px;background-color:#ffffff;border-radius:16px;padding:8px;box-shadow:0 6px 16px rgba(0,0,0,0.2);">
                                                    <img src="%s" alt="%s" style="max-width:56px;max-height:56px;display:block;margin:0 auto;border:0;" />
                                                </td>
                                            </tr>
                                        </table>
                                        <div style="display:inline-block;background-color:rgba(16,185,129,0.22);border:1px solid rgba(52,211,153,0.55);color:#6ee7b7;font-size:11px;font-weight:700;letter-spacing:0.8px;padding:5px 14px;border-radius:999px;">
                                            &#10003; PAGO EXITOSO
                                        </div>
                                        <h1 style="margin:12px 0 4px 0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.3px;">
                                            Comprobante Digital de Operaci&oacute;n
                                        </h1>
                                        <p style="margin:0;color:#cbd5e1;font-size:13px;">
                                            Hola, <strong>%s</strong> &middot; Tu pago fue aplicado y verificado
                                        </p>
                                    </td>
                                </tr>

                                <!-- Ticket de Importe Principal con Línea Punteada -->
                                <tr>
                                    <td style="padding:26px 24px;text-align:center;background-color:#f8fafc;border-bottom:2px dashed #cbd5e1;">
                                        <div style="color:#64748b;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:1px;">
                                            Importe Total Aplicado
                                        </div>
                                        <div style="color:#0f172a;font-size:38px;font-weight:800;letter-spacing:-1px;margin:8px 0;">
                                            $%s <span style="font-size:16px;font-weight:700;color:#64748b;">MXN</span>
                                        </div>
                                        <div style="display:inline-block;background-color:#ecfdf5;border:1px solid #a7f3d0;color:#047857;font-size:12px;font-weight:700;padding:4px 12px;border-radius:999px;">
                                            Cuenta cargo: %s
                                        </div>
                                    </td>
                                </tr>

                                <!-- Franja Destacada de Folios (Autorización + UPC) -->
                                <tr>
                                    <td style="padding:20px 24px 0 24px;">
                                        <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f0f9ff;border:1px solid #bae6fd;border-radius:14px;">
                                            <tr>
                                                <td width="50%%" style="padding:12px 14px;border-right:1px solid #bae6fd;">
                                                    <div style="font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:0.6px;color:#0369a1;">
                                                        Numero de Autorizacion:
                                                    </div>
                                                    <div style="margin-top:3px;font-family:'Courier New',Courier,monospace;font-size:15px;font-weight:800;color:#0f172a;">
                                                        %s
                                                    </div>
                                                </td>
                                                <td width="50%%" style="padding:12px 14px;">
                                                    <div style="font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:0.6px;color:#0369a1;">
                                                        Folio UPC
                                                    </div>
                                                    <div style="margin-top:3px;font-family:'Courier New',Courier,monospace;font-size:15px;font-weight:800;color:#0f172a;">
                                                        %s
                                                    </div>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Detalle de la Transacción -->
                                <tr>
                                    <td style="padding:22px 24px 26px 24px;">
                                        %s
                                        <h3 style="margin:0 0 12px 0;color:#0f172a;font-size:12px;font-weight:800;text-transform:uppercase;letter-spacing:0.8px;border-bottom:1px solid #e2e8f0;padding-bottom:8px;">
                                            Desglose del Comprobante
                                        </h3>
                                        <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0">
                                            <tr>
                                                <td style="padding:10px 0;color:#64748b;font-size:13px;border-bottom:1px solid #f1f5f9;">Servicio:</td>
                                                <td style="padding:10px 0;color:#0f172a;font-size:13px;font-weight:700;text-align:right;border-bottom:1px solid #f1f5f9;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding:10px 0;color:#64748b;font-size:13px;border-bottom:1px solid #f1f5f9;">Producto:</td>
                                                <td style="padding:10px 0;color:#0f172a;font-size:13px;font-weight:600;text-align:right;border-bottom:1px solid #f1f5f9;">%s</td>
                                            </tr>
                                            %s
                                            %s
                                            <tr>
                                                <td style="padding:10px 0;color:#64748b;font-size:13px;border-bottom:1px solid #f1f5f9;">Fecha y Hora:</td>
                                                <td style="padding:10px 0;color:#0f172a;font-size:13px;font-weight:600;text-align:right;border-bottom:1px solid #f1f5f9;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding:10px 0;color:#64748b;font-size:13px;">ID Transaccion Proveedor:</td>
                                                <td style="padding:10px 0;color:#475569;font-size:12px;font-family:'Courier New',Courier,monospace;font-weight:700;text-align:right;">%s</td>
                                            </tr>
                                        </table>

                                        %s
                                    </td>
                                </tr>

                                <!-- Pie de Página -->
                                <tr>
                                    <td style="background-color:#0f172a;padding:20px 24px;text-align:center;">
                                        <p style="margin:0 0 6px 0;color:#e2e8f0;font-size:12px;font-weight:700;">
                                            GestoPagos &middot; Comprobante Digital Verificado
                                        </p>
                                        <p style="margin:0 0 6px 0;color:#94a3b8;font-size:11px;line-height:1.5;">
                                            Operaci&oacute;n protegida con autenticaci&oacute;n 2FA y cifrado transaccional AES-256.
                                        </p>
                                        <p style="margin:0;color:#64748b;font-size:11px;line-height:1.5;">
                                            Conserva este folio UPC y n&uacute;mero de autorizaci&oacute;n para cualquier aclaraci&oacute;n con el proveedor del servicio.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(
                preheader,
                logoUrl,
                servicio,
                titular,
                montoFormateado,
                cuentaEnmascarada,
                autorizacion,
                upc,
                seccionPin,
                servicio,
                producto,
                filasIdentificador,
                filaComision,
                fecha,
                idTx,
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

    private String resolverLogoUrl(String logoUrl) {
        return (logoUrl != null && !logoUrl.trim().isEmpty())
                ? logoUrl
                : LogoServicioResolver.LOGO_DEFAULT_SERVICIO;
    }

    private String formatearMonto(BigDecimal monto) {
        return monto != null ? CURRENCY_FORMAT.format(monto) : "0.00";
    }

    private String formatearComision(BigDecimal comision) {
        return (comision != null && comision.compareTo(BigDecimal.ZERO) > 0)
                ? CURRENCY_FORMAT.format(comision)
                : null;
    }

    private String construirFilasIdentificador(ComprobantePagoEmailDto dto, boolean esTelefonico) {
        StringBuilder filas = new StringBuilder();
        String tel = EmailTemplateBuilder.escapeHtml(dto.getTelefono() != null ? dto.getTelefono() : "No especificado");
        String ref = EmailTemplateBuilder.escapeHtml(dto.getReferencia() != null ? dto.getReferencia() : "No especificada");

        if (esTelefonico) {
            filas.append("""
                <tr>
                    <td style="padding:10px 0;color:#64748b;font-size:13px;border-bottom:1px solid #f1f5f9;">Numero de Telefono:</td>
                    <td style="padding:10px 0;color:#0f172a;font-size:13px;font-family:'Courier New',Courier,monospace;font-weight:700;text-align:right;border-bottom:1px solid #f1f5f9;">%s</td>
                </tr>
            """.formatted(tel));

            if (dto.getReferencia() != null && !dto.getReferencia().trim().isEmpty()
                    && !dto.getReferencia().equals(dto.getTelefono()) && !"0".equals(dto.getReferencia())) {
                filas.append("""
                    <tr>
                        <td style="padding:10px 0;color:#64748b;font-size:13px;border-bottom:1px solid #f1f5f9;">Folio / Referencia:</td>
                        <td style="padding:10px 0;color:#0f172a;font-size:13px;font-family:'Courier New',Courier,monospace;font-weight:600;text-align:right;border-bottom:1px solid #f1f5f9;">%s</td>
                    </tr>
                """.formatted(ref));
            }
        } else {
            filas.append("""
                <tr>
                    <td style="padding:10px 0;color:#64748b;font-size:13px;border-bottom:1px solid #f1f5f9;">Referencia de Pago:</td>
                    <td style="padding:10px 0;color:#0f172a;font-size:13px;font-family:'Courier New',Courier,monospace;font-weight:700;text-align:right;border-bottom:1px solid #f1f5f9;">%s</td>
                </tr>
            """.formatted(ref));

            if (dto.getTelefono() != null && !dto.getTelefono().trim().isEmpty() && !"0".equals(dto.getTelefono())) {
                filas.append("""
                    <tr>
                        <td style="padding:10px 0;color:#64748b;font-size:13px;border-bottom:1px solid #f1f5f9;">Telefono Asociado:</td>
                        <td style="padding:10px 0;color:#0f172a;font-size:13px;font-family:'Courier New',Courier,monospace;font-weight:600;text-align:right;border-bottom:1px solid #f1f5f9;">%s</td>
                    </tr>
                """.formatted(tel));
            }
        }
        return filas.toString();
    }

    private String construirFilaComision(String comisionFormateada) {
        if (comisionFormateada == null) {
            return "";
        }
        return """
            <tr>
                <td style="padding:10px 0;color:#64748b;font-size:13px;border-bottom:1px solid #f1f5f9;">Comision de Servicio:</td>
                <td style="padding:10px 0;color:#0f172a;font-size:13px;font-weight:600;text-align:right;border-bottom:1px solid #f1f5f9;">$%s MXN</td>
            </tr>
        """.formatted(comisionFormateada);
    }

    private String construirSeccionPin(String pin) {
        if (pin == null || pin.trim().isEmpty()) {
            return "";
        }
        return """
            <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="margin-bottom:18px;background-color:#fffbeb;border:1px solid #fcd34d;border-radius:12px;">
                <tr>
                    <td style="padding:14px 16px;text-align:center;">
                        <div style="font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.8px;color:#92400e;">
                            PIN / C&oacute;digo Electr&oacute;nico Adquirido
                        </div>
                        <div style="margin-top:6px;font-family:'Courier New',Courier,monospace;font-size:20px;font-weight:800;letter-spacing:2px;color:#78350f;">
                            %s
                        </div>
                    </td>
                </tr>
            </table>
        """.formatted(EmailTemplateBuilder.escapeHtml(pin.trim()));
    }

    private String construirSeccionLegend(String legend) {
        if (legend == null || legend.trim().isEmpty()) {
            return "";
        }
        return """
            <div style="margin-top:20px;padding:14px 16px;background-color:#f8fafc;border-left:4px solid #0284c7;border-radius:8px;">
                <p style="margin:0 0 4px 0;color:#0f172a;font-size:12px;font-weight:700;">Instrucciones y Aclaraciones del Proveedor:</p>
                <p style="margin:0;color:#475569;font-size:12px;line-height:1.5;">%s</p>
            </div>
        """.formatted(EmailTemplateBuilder.escapeHtml(legend));
    }
}
