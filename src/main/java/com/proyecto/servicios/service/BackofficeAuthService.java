package com.proyecto.servicios.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.proyecto.servicios.client.BrevoEmailClient;
import com.proyecto.servicios.exception.AutenticacionException;
import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.model.email.BrevoEmailContact;
import com.proyecto.servicios.model.email.BrevoSendEmailRequest;
import com.proyecto.servicios.util.EmailTemplateBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Autenticación exclusiva de la Consola Backoffice con credencial única
 * y verificación obligatoria en 2 pasos (código OTP al correo o huella digital en móvil).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long OTP_TTL_SEGUNDOS = 300; // 5 minutos
    private static final long SESION_TTL_SEGUNDOS = 1800; // 30 minutos
    private static final int MAX_INTENTOS_OTP = 5;

    private final BrevoEmailClient brevoEmailClient;

    @Value("${backoffice.auth.email:}")
    private String adminEmail;

    @Value("${backoffice.auth.password:}")
    private String adminPassword;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.security-name:GestoPagos · Seguridad}")
    private String senderName;

    @Value("${brevo.sender.email:zahir.rodriguez.per@gmail.com}")
    private String senderEmail;

    private final Map<String, Desafio2FA> desafiosActivos = new ConcurrentHashMap<>();
    private final Map<String, SesionBackoffice> sesionesActivas = new ConcurrentHashMap<>();

    public Map<String, Object> iniciarLogin(String email, String password, String plataforma) {
        limpiarExpirados();

        String correoLimpio = email != null ? email.trim() : "";
        String passLimpio = password != null ? password : "";
        String emailEsperado = adminEmail != null ? adminEmail.trim() : "";
        String passEsperado = adminPassword != null ? adminPassword : "";

        if (emailEsperado.isEmpty() || passEsperado.isEmpty()) {
            log.error("[BackofficeAuth]: Credenciales de administrador no configuradas en variables de entorno");
            throw new AutenticacionException(CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA,
                    "Acceso administrativo no disponible");
        }

        boolean emailOk = correoLimpio.equalsIgnoreCase(emailEsperado);
        boolean passOk = comparacionConstante(passLimpio, passEsperado);

        if (!emailOk || !passOk) {
            log.warn("[BackofficeAuth]: Intento de acceso fallido para correo={}", enmascararEmail(correoLimpio));
            throw new AutenticacionException(CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA,
                    "Credenciales de Consola Backoffice inválidas");
        }

        boolean esMovil = "MOBILE".equalsIgnoreCase(plataforma);
        String challengeId = UUID.randomUUID().toString();
        String codigoOtp = generarCodigoOtp();
        Instant expiraEn = Instant.now().plusSeconds(OTP_TTL_SEGUNDOS);

        desafiosActivos.put(challengeId, new Desafio2FA(emailEsperado, codigoOtp, expiraEn, 0));

        boolean correoEnviado = enviarCodigoOtpCorreo(emailEsperado, codigoOtp);
        String mensaje = correoEnviado
                ? "Código de verificación enviado al correo " + enmascararEmail(emailEsperado)
                : "Verificación iniciada. No se pudo confirmar el envío por correo; puedes reintentar o usar huella en tu teléfono.";

        return Map.of(
                "requiere2fa", true,
                "challengeId", challengeId,
                "emailEnmascarado", enmascararEmail(emailEsperado),
                "correoEnviado", correoEnviado,
                "permiteHuella", esMovil,
                "expiraEnSegundos", OTP_TTL_SEGUNDOS,
                "mensaje", mensaje
        );
    }

    public Map<String, Object> reenviarOtp(String challengeId) {
        limpiarExpirados();
        Desafio2FA desafio = challengeId != null ? desafiosActivos.get(challengeId.trim()) : null;
        if (desafio == null || Instant.now().isAfter(desafio.expiraEn)) {
            throw new AutenticacionException(CatalogoCodigoError.SESION_EXPIRADA,
                    "El desafío de verificación expiró. Inicia sesión nuevamente.");
        }

        String nuevoOtp = generarCodigoOtp();
        Instant nuevaExpiracion = Instant.now().plusSeconds(OTP_TTL_SEGUNDOS);
        desafiosActivos.put(challengeId.trim(), new Desafio2FA(desafio.email, nuevoOtp, nuevaExpiracion, 0));

        boolean correoEnviado = enviarCodigoOtpCorreo(desafio.email, nuevoOtp);
        return Map.of(
                "reenviado", correoEnviado,
                "emailEnmascarado", enmascararEmail(desafio.email),
                "expiraEnSegundos", OTP_TTL_SEGUNDOS,
                "mensaje", correoEnviado
                        ? "Se envió un nuevo código de 6 dígitos a " + enmascararEmail(desafio.email)
                        : "No se pudo reenviar el correo en este momento. Intenta nuevamente."
        );
    }

    public Map<String, Object> verificarSegundoFactor(
            String challengeId,
            String metodo,
            String codigo,
            Boolean biometriaVerificada,
            String plataforma) {
        limpiarExpirados();

        String idLimpio = challengeId != null ? challengeId.trim() : "";
        Desafio2FA desafio = desafiosActivos.get(idLimpio);
        if (desafio == null || Instant.now().isAfter(desafio.expiraEn)) {
            desafiosActivos.remove(idLimpio);
            throw new AutenticacionException(CatalogoCodigoError.SESION_EXPIRADA,
                    "El código de verificación expiró. Inicia sesión nuevamente.");
        }

        String metodoNorm = metodo != null ? metodo.trim().toUpperCase() : "EMAIL_OTP";

        if ("HUELLA".equals(metodoNorm)) {
            if (!"MOBILE".equalsIgnoreCase(plataforma)) {
                throw new AutenticacionException(CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA,
                        "La verificación por huella digital solo está disponible en el teléfono móvil.");
            }
            if (!Boolean.TRUE.equals(biometriaVerificada)) {
                throw new AutenticacionException(CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA,
                        "No se confirmó la validación de huella digital en el dispositivo.");
            }
        } else if ("EMAIL_OTP".equals(metodoNorm)) {
            String codigoLimpio = codigo != null ? codigo.trim() : "";
            if (!comparacionConstante(codigoLimpio, desafio.codigoOtp)) {
                int intentos = desafio.intentos + 1;
                if (intentos >= MAX_INTENTOS_OTP) {
                    desafiosActivos.remove(idLimpio);
                    throw new AutenticacionException(CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA,
                            "Demasiados intentos fallidos. Solicita un nuevo acceso.");
                }
                desafiosActivos.put(idLimpio, new Desafio2FA(desafio.email, desafio.codigoOtp, desafio.expiraEn, intentos));
                throw new AutenticacionException(CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA,
                        "Código de verificación incorrecto. Te quedan " + (MAX_INTENTOS_OTP - intentos) + " intentos.");
            }
        } else {
            throw new AutenticacionException(CatalogoCodigoError.DATOS_ENTRADA_INVALIDOS,
                    "Método 2FA no soportado. Usa EMAIL_OTP o HUELLA.");
        }

        desafiosActivos.remove(idLimpio);
        String tokenBackoffice = UUID.randomUUID().toString();
        Instant expiraSesion = Instant.now().plusSeconds(SESION_TTL_SEGUNDOS);
        sesionesActivas.put(tokenBackoffice, new SesionBackoffice(desafio.email, expiraSesion));

        log.info("[BackofficeAuth]: Acceso Backoffice autorizado para {} via {}", enmascararEmail(desafio.email), metodoNorm);
        return Map.of(
                "autenticado", true,
                "tokenBackoffice", tokenBackoffice,
                "email", desafio.email,
                "metodoUsado", metodoNorm,
                "expiraEnSegundos", SESION_TTL_SEGUNDOS,
                "mensaje", "Acceso autorizado a Consola Backoffice"
        );
    }

    public boolean validarToken(String token) {
        limpiarExpirados();
        if (token == null || token.isBlank()) {
            return false;
        }
        String limpio = token.replaceAll("(?i)^bearer\\s*", "").trim();
        SesionBackoffice sesion = sesionesActivas.get(limpio);
        if (sesion == null || Instant.now().isAfter(sesion.expiraEn)) {
            sesionesActivas.remove(limpio);
            return false;
        }
        sesionesActivas.put(limpio, new SesionBackoffice(sesion.email, Instant.now().plusSeconds(SESION_TTL_SEGUNDOS)));
        return true;
    }

    public void cerrarSesion(String token) {
        if (token != null && !token.isBlank()) {
            sesionesActivas.remove(token.replaceAll("(?i)^bearer\\s*", "").trim());
        }
    }

    private boolean enviarCodigoOtpCorreo(String destinatario, String codigoOtp) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("[BackofficeAuth]: BREVO_API_KEY no configurada; no se envió correo OTP.");
            return false;
        }
        String html = EmailTemplateBuilder.construirHtmlCodigoOtp(
                "Consola Backoffice · 2FA",
                "Autorización de Acceso Administrativo",
                "Hola, Administrador",
                "Detectamos un inicio de sesión en la Consola Operativa Backoffice de GestoPagos. Ingresa este código de 6 dígitos para confirmar tu identidad:",
                codigoOtp,
                5,
                "Si no reconoces este intento de acceso, te recomendamos revisar las credenciales administrativas de inmediato e ignorar este mensaje.",
                true
        );

        BrevoSendEmailRequest req = BrevoSendEmailRequest.builder()
                .sender(BrevoEmailContact.builder().name(senderName).email(senderEmail).build())
                .to(List.of(BrevoEmailContact.builder().name("Administrador Backoffice").email(destinatario).build()))
                .subject("GestoPagos Seguridad · Código Backoffice: " + codigoOtp)
                .htmlContent(html)
                .build();
        try {
            brevoEmailClient.sendTransacEmail(brevoApiKey, req);
            log.info("[BackofficeAuth]: Código OTP enviado correctamente a {}", enmascararEmail(destinatario));
            return true;
        } catch (Exception e) {
            log.error("[BackofficeAuth]: Error al enviar correo OTP via Brevo ({})", e.getClass().getSimpleName());
            return false;
        }
    }

    private String generarCodigoOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private boolean comparacionConstante(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    private String enmascararEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) {
            return "***";
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }

    private void limpiarExpirados() {
        Instant ahora = Instant.now();
        desafiosActivos.entrySet().removeIf(e -> ahora.isAfter(e.getValue().expiraEn));
        sesionesActivas.entrySet().removeIf(e -> ahora.isAfter(e.getValue().expiraEn));
    }

    private record Desafio2FA(String email, String codigoOtp, Instant expiraEn, int intentos) {}

    private record SesionBackoffice(String email, Instant expiraEn) {}
}
