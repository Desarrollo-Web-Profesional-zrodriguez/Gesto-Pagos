package com.proyecto.servicios.service.cliente;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.proyecto.servicios.client.BrevoEmailClient;
import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.email.BrevoEmailContact;
import com.proyecto.servicios.model.email.BrevoSendEmailRequest;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioLoginRepository;
import com.proyecto.servicios.util.EmailTemplateBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de verificación de correo electrónico mediante código OTP de 6 dígitos
 * previo a la apertura de una nueva cuenta digital (Onboarding KYC).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OnboardingOtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final long OTP_TTL_SEGUNDOS = 300; // 5 minutos
    private static final int MAX_INTENTOS_OTP = 5;

    private final ClienteRepository clienteRepository;
    private final UsuarioLoginRepository usuarioLoginRepository;
    private final ClienteService clienteService;
    private final BrevoEmailClient brevoEmailClient;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.security-name:GestoPagos · Seguridad}")
    private String senderName;

    @Value("${brevo.sender.email:zahir.rodriguez.per@gmail.com}")
    private String senderEmail;

    private final Map<String, DesafioOnboarding> desafiosPorCorreo = new ConcurrentHashMap<>();

    public Map<String, Object> solicitarCodigoVerificacion(
            String correoElectronico,
            String nombre,
            String curp,
            String rfc) {
        limpiarExpirados();

        String correoNorm = normalizarCorreo(correoElectronico);
        validarDisponibilidadPrevia(correoNorm, curp, rfc);

        String codigoOtp = generarCodigoOtp();
        Instant expiraEn = Instant.now().plusSeconds(OTP_TTL_SEGUNDOS);
        desafiosPorCorreo.put(correoNorm, new DesafioOnboarding(codigoOtp, expiraEn, 0));

        String nombreMostrar = (nombre != null && !nombre.isBlank()) ? nombre.trim() : "Futuro Cliente";
        enviarCorreoOtpApertura(correoNorm, nombreMostrar, codigoOtp);

        String enmascarado = enmascararEmail(correoNorm);
        return Map.of(
                "enviado", true,
                "emailEnmascarado", enmascarado,
                "expiraEnSegundos", OTP_TTL_SEGUNDOS,
                "mensaje", "Enviamos un código de verificación de 6 dígitos a " + enmascarado
        );
    }

    public ClienteResponse verificarYRegistrarCliente(String codigoOtp, ClienteRegistroRequest request) {
        limpiarExpirados();

        String correoNorm = normalizarCorreo(request.getCorreoElectronico());
        DesafioOnboarding desafio = desafiosPorCorreo.get(correoNorm);
        if (desafio == null || Instant.now().isAfter(desafio.expiraEn)) {
            desafiosPorCorreo.remove(correoNorm);
            throw new ReglaNegocioException(CatalogoCodigoError.DATOS_ENTRADA_INVALIDOS,
                    "El código de verificación expiró o no existe para este correo. Solicita un nuevo código.");
        }

        String codigoLimpio = codigoOtp != null ? codigoOtp.trim() : "";
        if (!comparacionConstante(codigoLimpio, desafio.codigoOtp)) {
            int intentos = desafio.intentos + 1;
            if (intentos >= MAX_INTENTOS_OTP) {
                desafiosPorCorreo.remove(correoNorm);
                throw new ReglaNegocioException(CatalogoCodigoError.DATOS_ENTRADA_INVALIDOS,
                        "Se agotaron los intentos permitidos. Solicita un nuevo código de verificación.");
            }
            desafiosPorCorreo.put(correoNorm, new DesafioOnboarding(desafio.codigoOtp, desafio.expiraEn, intentos));
            throw new ReglaNegocioException(CatalogoCodigoError.DATOS_ENTRADA_INVALIDOS,
                    "Código de verificación incorrecto. Te quedan " + (MAX_INTENTOS_OTP - intentos) + " intentos.");
        }

        desafiosPorCorreo.remove(correoNorm);
        ClienteResponse creado = clienteService.registrarCliente(request);
        enviarCorreoBienvenidaNoBloqueante(creado);
        return creado;
    }

    private void validarDisponibilidadPrevia(String correoNorm, String curp, String rfc) {
        if (curp != null && !curp.isBlank() && clienteRepository.existsByCurp(curp.trim().toUpperCase())) {
            throw new CurpDuplicadaException("Ya existe un cliente registrado con la CURP proporcionada");
        }
        if (rfc != null && !rfc.isBlank() && clienteRepository.existsByRfc(rfc.trim().toUpperCase())) {
            throw new RfcDuplicadoException("Ya existe un cliente registrado con el RFC proporcionado");
        }
        if (clienteRepository.existsByCorreoElectronico(correoNorm)
                || usuarioLoginRepository.existsByUsername(correoNorm)) {
            throw new ClienteYaRegistradoException("Ya existe una cuenta registrada con el correo electrónico proporcionado");
        }
    }

    private void enviarCorreoOtpApertura(String destinatario, String nombreMostrar, String codigoOtp) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.error("[OnboardingOTP]: BREVO_API_KEY no configurada para enviar código de verificación.");
            throw new ReglaNegocioException(CatalogoCodigoError.ERROR_INTERNO,
                    "El servicio de verificación por correo no está configurado en este entorno.");
        }

        String html = EmailTemplateBuilder.construirHtmlCodigoOtp(
                "Apertura de Cuenta · Verificación",
                "Confirma tu Correo Electrónico",
                "¡Hola, " + nombreMostrar + "!",
                "Estás a un paso de abrir tu cuenta digital GestoPagos a 16 dígitos. Ingresa este código en la pantalla de registro para verificar que este correo te pertenece:",
                codigoOtp,
                5,
                "Si tú no iniciaste el registro de una cuenta en GestoPagos, puedes ignorar este correo de forma segura; ninguna cuenta será creada sin este código.",
                false
        );

        BrevoSendEmailRequest req = BrevoSendEmailRequest.builder()
                .sender(BrevoEmailContact.builder().name(senderName).email(senderEmail).build())
                .to(List.of(BrevoEmailContact.builder().name(nombreMostrar).email(destinatario).build()))
                .subject("GestoPagos · Código de verificación de cuenta: " + codigoOtp)
                .htmlContent(html)
                .build();

        try {
            brevoEmailClient.sendTransacEmail(brevoApiKey, req);
            log.info("[OnboardingOTP]: Código OTP de apertura enviado a {}", enmascararEmail(destinatario));
        } catch (Exception e) {
            log.error("[OnboardingOTP]: Falla al enviar correo OTP via Brevo ({})", e.getClass().getSimpleName());
            throw new ReglaNegocioException(CatalogoCodigoError.ERROR_INTERNO,
                    "No fue posible enviar el código de verificación a tu correo en este momento. Intenta nuevamente.");
        }
    }

    private void enviarCorreoBienvenidaNoBloqueante(ClienteResponse creado) {
        if (brevoApiKey == null || brevoApiKey.isBlank() || creado == null || creado.getCorreoElectronico() == null) {
            return;
        }
        try {
            CuentaResponse primeraCuenta = (creado.getCuentas() != null && !creado.getCuentas().isEmpty())
                    ? creado.getCuentas().get(0)
                    : null;
            String numCuenta = primeraCuenta != null ? primeraCuenta.getNumeroCuenta() : null;
            double saldo = (primeraCuenta != null && primeraCuenta.getSaldo() != null
                    && primeraCuenta.getSaldo().getSaldoDisponible() != null)
                    ? primeraCuenta.getSaldo().getSaldoDisponible()
                    : 0.0;

            String nombre = creado.getNombreCompleto() != null && !creado.getNombreCompleto().isBlank()
                    ? creado.getNombreCompleto()
                    : creado.getNombre();

            String html = EmailTemplateBuilder.construirHtmlBienvenidaCuenta(
                    nombre,
                    creado.getCorreoElectronico(),
                    enmascararCuenta(numCuenta),
                    CURRENCY_FORMAT.format(saldo)
            );

            BrevoSendEmailRequest req = BrevoSendEmailRequest.builder()
                    .sender(BrevoEmailContact.builder().name("GestoPagos · Banca Digital").email(senderEmail).build())
                    .to(List.of(BrevoEmailContact.builder()
                            .name(nombre != null ? nombre : "Titular")
                            .email(creado.getCorreoElectronico().trim())
                            .build()))
                    .subject("¡Bienvenido a GestoPagos! Tu cuenta digital está activa")
                    .htmlContent(html)
                    .build();

            brevoEmailClient.sendTransacEmail(brevoApiKey, req);
            log.info("[OnboardingOTP]: Correo de bienvenida enviado a {}", enmascararEmail(creado.getCorreoElectronico()));
        } catch (Exception e) {
            log.warn("[OnboardingOTP]: No se pudo enviar el correo de bienvenida no bloqueante ({})", e.getClass().getSimpleName());
        }
    }

    private String normalizarCorreo(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ReglaNegocioException(CatalogoCodigoError.DATOS_ENTRADA_INVALIDOS,
                    "El correo electrónico es obligatorio para enviar el código de verificación.");
        }
        return raw.trim().toLowerCase();
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
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }

    private String enmascararCuenta(String numCuenta) {
        if (numCuenta == null || numCuenta.length() < 4) {
            return "****";
        }
        String clean = numCuenta.trim();
        return "**** **** **** " + clean.substring(clean.length() - 4);
    }

    private void limpiarExpirados() {
        Instant ahora = Instant.now();
        desafiosPorCorreo.entrySet().removeIf(e -> ahora.isAfter(e.getValue().expiraEn));
    }

    private record DesafioOnboarding(String codigoOtp, Instant expiraEn, int intentos) {}
}
