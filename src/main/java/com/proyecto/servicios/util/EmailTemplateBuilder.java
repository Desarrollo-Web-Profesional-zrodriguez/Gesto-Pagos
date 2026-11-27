package com.proyecto.servicios.util;

/**
 * Constructor centralizado de plantillas HTML transaccionales compatibles con
 * Gmail, Outlook, Apple Mail y clientes móviles (tablas de presentación e inline CSS).
 */
public final class EmailTemplateBuilder {

    private EmailTemplateBuilder() {
        // Clase utilitaria
    }

    public static String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Genera plantilla HTML bancaria para envío de códigos OTP de 6 dígitos
     * (tanto para 2FA de Consola Backoffice como para Verificación de Apertura de Cuenta).
     */
    public static String construirHtmlCodigoOtp(
            String badgeSuperior,
            String titulo,
            String saludo,
            String descripcion,
            String codigoOtp,
            int minutosExpiracion,
            String notaSeguridad,
            boolean estiloEjecutivoAmbar) {

        String codigoLimpio = codigoOtp != null ? codigoOtp.trim() : "000000";
        String colorAcento = estiloEjecutivoAmbar ? "#f59e0b" : "#38bdf8";
        String bgBadge = estiloEjecutivoAmbar ? "rgba(245, 158, 11, 0.18)" : "rgba(56, 189, 248, 0.18)";
        String textoBadge = estiloEjecutivoAmbar ? "#fde68a" : "#bae6fd";
        String bordeBadge = estiloEjecutivoAmbar ? "rgba(251, 191, 36, 0.45)" : "rgba(56, 189, 248, 0.45)";

        String celdasDigitos = construirCeldasDigitosOtp(codigoLimpio);
        String preheader = escapeHtml(titulo) + " — Tu código de seguridad es " + escapeHtml(codigoLimpio);

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;-webkit-font-smoothing:antialiased;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">%s</div>
                <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f1f5f9;padding:28px 12px;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width:560px;background-color:#ffffff;border-radius:20px;overflow:hidden;border:1px solid #e2e8f0;box-shadow:0 10px 28px rgba(15,23,42,0.08);">
                                <!-- Encabezado de Marca -->
                                <tr>
                                    <td style="background:linear-gradient(135deg,#0f172a 0%%,#0c4a6e 100%%);padding:28px 28px 24px 28px;text-align:center;">
                                        <div style="font-size:21px;font-weight:800;letter-spacing:-0.5px;color:#ffffff;margin-bottom:12px;">
                                            Gesto<span style="color:#38bdf8;">Pagos</span>
                                        </div>
                                        <div style="display:inline-block;background-color:%s;border:1px solid %s;color:%s;font-size:11px;font-weight:700;letter-spacing:0.8px;text-transform:uppercase;padding:5px 14px;border-radius:999px;">
                                            %s
                                        </div>
                                        <h1 style="margin:14px 0 0 0;color:#ffffff;font-size:21px;font-weight:700;line-height:1.3;">
                                            %s
                                        </h1>
                                    </td>
                                </tr>

                                <!-- Cuerpo Principal -->
                                <tr>
                                    <td style="padding:28px 28px 20px 28px;">
                                        <p style="margin:0 0 8px 0;color:#0f172a;font-size:15px;font-weight:700;">
                                            %s
                                        </p>
                                        <p style="margin:0 0 22px 0;color:#475569;font-size:14px;line-height:1.6;">
                                            %s
                                        </p>

                                        <!-- Contenedor de Código OTP -->
                                        <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:16px;padding:22px 14px;text-align:center;">
                                            <tr>
                                                <td align="center">
                                                    <div style="font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:1px;color:#64748b;margin-bottom:14px;">
                                                        C&oacute;digo de verificaci&oacute;n de 6 d&iacute;gitos
                                                    </div>
                                                    <table role="presentation" border="0" cellspacing="0" cellpadding="0" align="center">
                                                        <tr>
                                                            %s
                                                        </tr>
                                                    </table>
                                                    <div style="margin-top:14px;font-size:12px;color:#64748b;">
                                                        C&oacute;digo directo: <strong style="font-family:'Courier New',Courier,monospace;font-size:14px;color:#0f172a;letter-spacing:2px;">%s</strong>
                                                    </div>
                                                </td>
                                            </tr>
                                        </table>

                                        <!-- Vigencia -->
                                        <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="margin-top:18px;">
                                            <tr>
                                                <td style="background-color:#f0f9ff;border-left:4px solid %s;border-radius:8px;padding:12px 14px;">
                                                    <p style="margin:0;color:#0f172a;font-size:12px;line-height:1.5;">
                                                        <strong>Vigencia:</strong> Este c&oacute;digo es de un solo uso y expira en <strong>%d minutos</strong>.
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>

                                        <!-- Aviso Anti-Phishing -->
                                        <p style="margin:18px 0 0 0;color:#64748b;font-size:12px;line-height:1.6;">
                                            %s
                                        </p>
                                    </td>
                                </tr>

                                <!-- Pie de Seguridad -->
                                <tr>
                                    <td style="background-color:#0f172a;padding:18px 24px;text-align:center;">
                                        <p style="margin:0 0 4px 0;color:#cbd5e1;font-size:12px;font-weight:600;">
                                            GestoPagos &middot; Centro de Seguridad Digital
                                        </p>
                                        <p style="margin:0;color:#64748b;font-size:11px;line-height:1.5;">
                                            Nunca compartas tus c&oacute;digos de verificaci&oacute;n ni contrase&ntilde;as. Ning&uacute;n colaborador de GestoPagos te los solicitar&aacute; por tel&eacute;fono o mensaje.
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
                escapeHtml(titulo),
                preheader,
                bgBadge,
                bordeBadge,
                textoBadge,
                escapeHtml(badgeSuperior),
                escapeHtml(titulo),
                escapeHtml(saludo),
                escapeHtml(descripcion),
                celdasDigitos,
                escapeHtml(codigoLimpio),
                colorAcento,
                minutosExpiracion,
                escapeHtml(notaSeguridad)
        );
    }

    /**
     * Genera plantilla HTML de bienvenida cuando el cliente verifica su correo y activa su cuenta digital.
     */
    public static String construirHtmlBienvenidaCuenta(
            String nombreTitular,
            String correoAcceso,
            String cuentaEnmascarada,
            String saldoInicialFormateado) {

        String nombreSeguro = escapeHtml(nombreTitular != null && !nombreTitular.isBlank() ? nombreTitular : "Titular");
        String preheader = "Bienvenido a GestoPagos. Tu cuenta digital " + escapeHtml(cuentaEnmascarada) + " ya está activa.";

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Bienvenido a GestoPagos</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">%s</div>
                <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f1f5f9;padding:28px 12px;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width:560px;background-color:#ffffff;border-radius:20px;overflow:hidden;border:1px solid #e2e8f0;box-shadow:0 10px 28px rgba(15,23,42,0.08);">
                                <tr>
                                    <td style="background:linear-gradient(135deg,#0f172a 0%%,#065f46 100%%);padding:30px 28px;text-align:center;">
                                        <div style="font-size:21px;font-weight:800;letter-spacing:-0.5px;color:#ffffff;margin-bottom:12px;">
                                            Gesto<span style="color:#34d399;">Pagos</span>
                                        </div>
                                        <div style="display:inline-block;background-color:rgba(16,185,129,0.2);border:1px solid rgba(52,211,153,0.45);color:#a7f3d0;font-size:11px;font-weight:700;letter-spacing:0.8px;text-transform:uppercase;padding:5px 14px;border-radius:999px;">
                                            &#10003; CORREO VERIFICADO &middot; CUENTA ACTIVA
                                        </div>
                                        <h1 style="margin:14px 0 0 0;color:#ffffff;font-size:22px;font-weight:700;">
                                            &iexcl;Bienvenido, %s!
                                        </h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding:28px;">
                                        <p style="margin:0 0 18px 0;color:#475569;font-size:14px;line-height:1.6;">
                                            Tu identidad de correo fue confirmada exitosamente y tu nueva cuenta digital GestoPagos est&aacute; lista para consultar saldos, pagar servicios y realizar recargas.
                                        </p>
                                        <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;padding:16px 18px;">
                                            <tr>
                                                <td style="padding:8px 0;color:#64748b;font-size:13px;border-bottom:1px solid #e2e8f0;">Usuario de acceso:</td>
                                                <td style="padding:8px 0;color:#0f172a;font-size:13px;font-weight:700;text-align:right;border-bottom:1px solid #e2e8f0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding:8px 0;color:#64748b;font-size:13px;border-bottom:1px solid #e2e8f0;">Cuenta digital (16 d&iacute;gitos):</td>
                                                <td style="padding:8px 0;color:#0284c7;font-size:13px;font-family:'Courier New',Courier,monospace;font-weight:700;text-align:right;border-bottom:1px solid #e2e8f0;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding:8px 0;color:#64748b;font-size:13px;">Saldo inicial disponible:</td>
                                                <td style="padding:8px 0;color:#059669;font-size:15px;font-weight:800;text-align:right;">$%s MXN</td>
                                            </tr>
                                        </table>
                                        <p style="margin:18px 0 0 0;color:#64748b;font-size:12px;line-height:1.6;">
                                            Por tu seguridad, tu sesi&oacute;n cuenta con cierre autom&aacute;tico tras 5 minutos de inactividad y confirmaci&oacute;n 2FA en cada operaci&oacute;n.
                                        </p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="background-color:#0f172a;padding:18px 24px;text-align:center;color:#94a3b8;font-size:11px;">
                                        GestoPagos Banca Digital &middot; Notificaci&oacute;n autom&aacute;tica de apertura de cuenta
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
                nombreSeguro,
                escapeHtml(correoAcceso),
                escapeHtml(cuentaEnmascarada),
                escapeHtml(saldoInicialFormateado)
        );
    }

    private static String construirCeldasDigitosOtp(String codigo) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codigo.length(); i++) {
            String ch = escapeHtml(String.valueOf(codigo.charAt(i)));
            sb.append("""
                <td style="padding:0 4px;">
                    <div style="width:42px;height:52px;line-height:52px;background-color:#ffffff;border:2px solid #cbd5e1;border-radius:12px;font-family:'Courier New',Courier,monospace;font-size:24px;font-weight:800;color:#0f172a;text-align:center;box-shadow:0 2px 4px rgba(15,23,42,0.04);">
                        %s
                    </div>
                </td>
                """.formatted(ch));
        }
        return sb.toString();
    }
}
