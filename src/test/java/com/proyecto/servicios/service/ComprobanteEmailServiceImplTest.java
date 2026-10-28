package com.proyecto.servicios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.proyecto.servicios.client.BrevoEmailClient;
import com.proyecto.servicios.model.email.BrevoSendEmailRequest;
import com.proyecto.servicios.model.email.BrevoSendEmailResponse;
import com.proyecto.servicios.model.email.ComprobanteEmailResult;
import com.proyecto.servicios.model.email.ComprobantePagoEmailDto;
import com.proyecto.servicios.service.Impl.ComprobanteEmailServiceImpl;
import com.proyecto.servicios.util.LogoServicioResolver;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

@ExtendWith(MockitoExtension.class)
class ComprobanteEmailServiceImplTest {

    @Mock
    private BrevoEmailClient brevoEmailClient;

    @Mock
    private LogoServicioResolver logoServicioResolver;

    @InjectMocks
    private ComprobanteEmailServiceImpl comprobanteEmailService;

    private static final String TEST_BREVO_KEY = "xkeysib-test-12345";
    private static final String SENDER_EMAIL = "soporte@servicios.com";
    private static final String SENDER_NAME = "Comprobante Digital";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(comprobanteEmailService, "brevoApiKey", TEST_BREVO_KEY);
        ReflectionTestUtils.setField(comprobanteEmailService, "senderEmail", SENDER_EMAIL);
        ReflectionTestUtils.setField(comprobanteEmailService, "senderName", SENDER_NAME);
    }

    @Test
    @DisplayName("Escenario 1: Recarga telefonica (tipoFront=1) debe incluir Numero de Telefono en el comprobante HTML")
    void testGenerarHtmlComprobante_EscenarioTelefonico() {
        ComprobantePagoEmailDto dto = ComprobantePagoEmailDto.builder()
                .servicio("Telcel")
                .producto("Recarga 100")
                .numAutorizacion("100016171")
                .montoPagado(new BigDecimal("100.00"))
                .telefono("5512345678")
                .referencia("5512345678")
                .numCuenta("8324433882000000")
                .fechaProveedor("23/Oct/2026 19:26:58")
                .legend("Conserva tu comprobante de recarga.")
                .tipoFront(1)
                .logoUrl("https://example.com/telcel.png")
                .build();

        String html = comprobanteEmailService.generarHtmlComprobante(dto);

        assertNotNull(html);
        assertTrue(html.contains("Numero de Telefono:"));
        assertTrue(html.contains("5512345678"));
        assertTrue(html.contains("100016171"));
        assertTrue(html.contains("100.00"));
        assertTrue(html.contains("Telcel"));
        assertTrue(html.contains("Conserva tu comprobante"));
        assertTrue(html.contains("PAGO EXITOSO"));
    }

    @Test
    @DisplayName("Escenario 2: Pago de servicio (Izzi, CFE) debe incluir Referencia de Pago y Telefono Asociado en el comprobante HTML")
    void testGenerarHtmlComprobante_EscenarioServicioReferencia() {
        ComprobantePagoEmailDto dto = ComprobantePagoEmailDto.builder()
                .servicio("IZZI Telecom")
                .producto("Izzi Telecom")
                .numAutorizacion("100016171")
                .idTx("945875845")
                .montoPagado(new BigDecimal("151.00"))
                .comision(new BigDecimal("7.00"))
                .telefono("5512345678")
                .referencia("25720381")
                .numCuenta("8324433882000000")
                .fechaProveedor("23/Oct/2026 19:26:58")
                .legend("Para cualquier duda o aclaracion con tu pago, comunicate al servicio de Atencion a Clientes de Izzi.")
                .tipoFront(2)
                .logoUrl("https://example.com/izzi.png")
                .build();

        String html = comprobanteEmailService.generarHtmlComprobante(dto);

        assertNotNull(html);
        assertTrue(html.contains("Referencia de Pago:"));
        assertTrue(html.contains("25720381"));
        assertTrue(html.contains("Telefono Asociado:"));
        assertTrue(html.contains("5512345678"));
        assertTrue(html.contains("100016171"));
        assertTrue(html.contains("151.00"));
        assertTrue(html.contains("7.00"));
        assertTrue(html.contains("IZZI Telecom"));
        assertTrue(html.contains("945875845"));
    }

    @Test
    @DisplayName("Debe enviar el correo exitosamente a traves de Brevo v3 API")
    void testEnviarComprobantePago_Exitoso() {
        ComprobantePagoEmailDto dto = ComprobantePagoEmailDto.builder()
                .destinatarioEmail("cliente@example.com")
                .destinatarioNombre("Juan Perez")
                .servicio("IZZI Telecom")
                .numAutorizacion("100016171")
                .montoPagado(new BigDecimal("151.00"))
                .telefono("5512345678")
                .referencia("25720381")
                .numCuenta("8324433882000000")
                .fechaProveedor("23/Oct/2026 19:26:58")
                .legend("Conserva tu comprobante.")
                .tipoFront(2)
                .build();

        when(logoServicioResolver.resolverUrlLogo(any(), any(), any(), any()))
                .thenReturn("https://example.com/izzi.png");

        when(brevoEmailClient.sendTransacEmail(eq(TEST_BREVO_KEY), any(BrevoSendEmailRequest.class)))
                .thenReturn(new BrevoSendEmailResponse("<msg-test-12345@smtp-relay.brevo.com>"));

        ComprobanteEmailResult result = comprobanteEmailService.enviarComprobantePago(dto);

        assertNotNull(result);
        assertTrue(result.isEnviado());
        assertEquals("<msg-test-12345@smtp-relay.brevo.com>", result.getMessageId());
        verify(brevoEmailClient).sendTransacEmail(eq(TEST_BREVO_KEY), any(BrevoSendEmailRequest.class));
    }

    @Test
    @DisplayName("Debe manejar de forma no bloqueante el error 401 de Brevo por IP no autorizada")
    void testEnviarComprobantePago_ErrorIpNoAutorizada() {
        ComprobantePagoEmailDto dto = ComprobantePagoEmailDto.builder()
                .destinatarioEmail("cliente@example.com")
                .servicio("IZZI Telecom")
                .numAutorizacion("100016171")
                .montoPagado(new BigDecimal("151.00"))
                .telefono("5512345678")
                .referencia("25720381")
                .numCuenta("8324433882000000")
                .build();

        Request mockRequest = Request.create(Request.HttpMethod.POST, "/v3/smtp/email", new HashMap<>(), Request.Body.empty(), new RequestTemplate());
        String errorJson = "{\"message\":\"We have detected you are using an unrecognised IP address 187.140.51.171.\",\"code\":\"unauthorized\"}";

        FeignException.Unauthorized unauthorizedException = new FeignException.Unauthorized(
                "Unauthorized", mockRequest, errorJson.getBytes(), new HashMap<>()
        );

        when(brevoEmailClient.sendTransacEmail(eq(TEST_BREVO_KEY), any(BrevoSendEmailRequest.class)))
                .thenThrow(unauthorizedException);

        ComprobanteEmailResult result = comprobanteEmailService.enviarComprobantePago(dto);

        assertNotNull(result);
        assertFalse(result.isEnviado());
        assertTrue(result.getErrorDetalle().contains("IP no autorizada en Brevo"));
    }

    @Test
    @DisplayName("No debe enviar correo si el destinatario viene vacio o nulo")
    void testEnviarComprobantePago_SinDestinatario() {
        ComprobantePagoEmailDto dto = ComprobantePagoEmailDto.builder()
                .destinatarioEmail(null)
                .servicio("IZZI Telecom")
                .build();

        ComprobanteEmailResult result = comprobanteEmailService.enviarComprobantePago(dto);

        assertNotNull(result);
        assertFalse(result.isEnviado());
        assertTrue(result.getMensaje().contains("No hay correo"));
    }
}
