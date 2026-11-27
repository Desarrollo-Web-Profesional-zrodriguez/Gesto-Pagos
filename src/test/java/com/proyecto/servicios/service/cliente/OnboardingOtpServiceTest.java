package com.proyecto.servicios.service.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.proyecto.servicios.client.BrevoEmailClient;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.cliente.SaldoResponse;
import com.proyecto.servicios.model.email.BrevoSendEmailRequest;
import com.proyecto.servicios.model.email.BrevoSendEmailResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioLoginRepository;

@ExtendWith(MockitoExtension.class)
class OnboardingOtpServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private UsuarioLoginRepository usuarioLoginRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private BrevoEmailClient brevoEmailClient;

    @InjectMocks
    private OnboardingOtpService onboardingOtpService;

    private static final String API_KEY = "xkeysib-test-onboarding";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(onboardingOtpService, "brevoApiKey", API_KEY);
        ReflectionTestUtils.setField(onboardingOtpService, "senderName", "GestoPagos · Seguridad");
        ReflectionTestUtils.setField(onboardingOtpService, "senderEmail", "seguridad@gestopagos.com");
    }

    @Test
    @DisplayName("Debe enviar código OTP de 6 dígitos y luego verificar y registrar al cliente con correo de bienvenida")
    void testFlujoCompletoSolicitarYVerificarOtp() {
        String email = "nuevo.cliente@example.com";
        when(clienteRepository.existsByCurp("PELJ950520HDFRRN09")).thenReturn(false);
        when(clienteRepository.existsByRfc("PELJ9505201A0")).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(email)).thenReturn(false);
        when(usuarioLoginRepository.existsByUsername(email)).thenReturn(false);
        when(brevoEmailClient.sendTransacEmail(eq(API_KEY), any(BrevoSendEmailRequest.class)))
                .thenReturn(new BrevoSendEmailResponse("<msg-otp-1>"));

        Map<String, Object> envio = onboardingOtpService.solicitarCodigoVerificacion(
                email, "Juan", "PELJ950520HDFRRN09", "PELJ9505201A0");

        assertEquals(true, envio.get("enviado"));
        assertEquals("nu***@example.com", envio.get("emailEnmascarado"));

        ArgumentCaptor<BrevoSendEmailRequest> emailCaptor = ArgumentCaptor.forClass(BrevoSendEmailRequest.class);
        verify(brevoEmailClient).sendTransacEmail(eq(API_KEY), emailCaptor.capture());

        String subject = emailCaptor.getValue().getSubject();
        Matcher m = Pattern.compile("(\\d{6})").matcher(subject);
        assertTrue(m.find(), "El asunto debe incluir el OTP de 6 dígitos");
        String otpExtraido = m.group(1);
        assertTrue(emailCaptor.getValue().getHtmlContent().contains(otpExtraido));

        ClienteRegistroRequest regReq = ClienteRegistroRequest.builder()
                .nombre("Juan")
                .correoElectronico(email)
                .build();

        ClienteResponse mockCreado = ClienteResponse.builder()
                .nombre("Juan")
                .nombreCompleto("Juan Perez")
                .correoElectronico(email)
                .cuentas(List.of(CuentaResponse.builder()
                        .numeroCuenta("1234567890123456")
                        .saldo(SaldoResponse.builder().saldoDisponible(1000.0).build())
                        .build()))
                .build();
        when(clienteService.registrarCliente(regReq)).thenReturn(mockCreado);

        ClienteResponse resultado = onboardingOtpService.verificarYRegistrarCliente(otpExtraido, regReq);

        assertNotNull(resultado);
        assertEquals("Juan Perez", resultado.getNombreCompleto());
        verify(brevoEmailClient, times(2)).sendTransacEmail(eq(API_KEY), any(BrevoSendEmailRequest.class));
    }

    @Test
    @DisplayName("Debe rechazar envío de OTP si CURP, RFC o correo ya están registrados")
    void testValidacionDuplicadosPrevios() {
        when(clienteRepository.existsByCurp("PELJ950520HDFRRN09")).thenReturn(true);
        assertThrows(CurpDuplicadaException.class, () ->
                onboardingOtpService.solicitarCodigoVerificacion("a@b.com", "Juan", "PELJ950520HDFRRN09", null));

        when(clienteRepository.existsByCurp("PELJ950520HDFRRN09")).thenReturn(false);
        when(clienteRepository.existsByRfc("PELJ9505201A0")).thenReturn(true);
        assertThrows(RfcDuplicadoException.class, () ->
                onboardingOtpService.solicitarCodigoVerificacion("a@b.com", "Juan", "PELJ950520HDFRRN09", "PELJ9505201A0"));

        when(clienteRepository.existsByRfc("PELJ9505201A0")).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico("a@b.com")).thenReturn(true);
        assertThrows(ClienteYaRegistradoException.class, () ->
                onboardingOtpService.solicitarCodigoVerificacion("a@b.com", "Juan", "PELJ950520HDFRRN09", "PELJ9505201A0"));
    }

    @Test
    @DisplayName("Debe descontar intentos ante código OTP incorrecto y bloquear al quinto intento")
    void testIntentosFallidosYBloqueo() {
        String email = "test.intentos@example.com";
        when(clienteRepository.existsByCorreoElectronico(email)).thenReturn(false);
        when(usuarioLoginRepository.existsByUsername(email)).thenReturn(false);
        when(brevoEmailClient.sendTransacEmail(eq(API_KEY), any(BrevoSendEmailRequest.class)))
                .thenReturn(new BrevoSendEmailResponse("<msg-otp-2>"));

        onboardingOtpService.solicitarCodigoVerificacion(email, "Ana", null, null);

        ClienteRegistroRequest req = ClienteRegistroRequest.builder().correoElectronico(email).build();

        for (int i = 0; i < 4; i++) {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                    onboardingOtpService.verificarYRegistrarCliente("000000", req));
            assertTrue(ex.getMessage().contains("Código de verificación incorrecto"));
        }

        ReglaNegocioException bloqueo = assertThrows(ReglaNegocioException.class, () ->
                onboardingOtpService.verificarYRegistrarCliente("000000", req));
        assertTrue(bloqueo.getMessage().contains("agotaron los intentos"));
    }
}
