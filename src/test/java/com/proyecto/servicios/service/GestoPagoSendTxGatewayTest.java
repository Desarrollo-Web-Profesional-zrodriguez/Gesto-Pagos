package com.proyecto.servicios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx.Veredicto;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;

@ExtendWith(MockitoExtension.class)
class GestoPagoSendTxGatewayTest {

    @Mock
    private GestoPagoTokenService tokenService;
    @Mock
    private GestoPagoProductClient client;

    private GestoPagoSendTxGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new GestoPagoSendTxGateway(tokenService, client);
    }

    private static String xml(String codigo, String texto) {
        return """
                <?xml version='1.0' encoding='UTF-8'?>
                <RESPONSE>
                    <ID_TX>291620941</ID_TX>
                    <NUM_AUTORIZACION>100074211</NUM_AUTORIZACION>
                    <COMISION>6.0</COMISION>
                    <MENSAJE><CODIGO>%s</CODIGO><TEXTO>%s</TEXTO></MENSAJE>
                </RESPONSE>
                """.formatted(codigo, texto);
    }

    private static Request request() {
        Map<String, Collection<String>> headers = Map.of();
        return Request.create(Request.HttpMethod.POST, "https://proveedor.test/sendTx.do", headers, (byte[]) null,
                StandardCharsets.UTF_8, null);
    }

    private static FeignException http(int status) {
        return FeignException.errorStatus("sendTx",
                Response.builder().status(status).reason("x").request(request()).headers(Map.of()).build());
    }

    private void conTokenValido() {
        when(tokenService.obtenerTokenValido()).thenReturn("token_test");
    }

    @Test
    @DisplayName("CODIGO 01 => EXITO")
    void codigo01_exito() {
        conTokenValido();
        when(client.sendTx(eq("Bearer token_test"), any())).thenReturn(xml("01", "Operacion realizada con exito"));

        RespuestaSendTx r = gateway.ejecutar("cifrado", "UPC0000001");

        assertEquals(Veredicto.EXITO, r.veredicto());
        assertEquals("100074211", r.parsed().getNumAutorizacion());
    }

    @Test
    @DisplayName("Rechazo explicito (06, 55, ...) => RECHAZADO atribuible al proveedor")
    void rechazoExplicito() {
        conTokenValido();
        when(client.sendTx(any(), any())).thenReturn(xml("55", "IP Origen no valida. [187.243.48.32]"));

        RespuestaSendTx r = gateway.ejecutar("cifrado", "UPC0000001");

        assertEquals(Veredicto.RECHAZADO, r.veredicto());
        assertEquals("55", r.codigo());
        assertTrue(r.esRechazoExplicitoDelProveedor());
    }

    @Test
    @DisplayName("CODIGO 07 (en proceso) => INDETERMINADO, no se libera saldo")
    void codigo07_indeterminado() {
        conTokenValido();
        when(client.sendTx(any(), any())).thenReturn(xml("07", "En proceso"));

        assertEquals(Veredicto.INDETERMINADO, gateway.ejecutar("cifrado", "UPC0000001").veredicto());
    }

    @Test
    @DisplayName("Timeout de lectura => INDETERMINADO (jamas se asume que no se proceso)")
    void timeout_indeterminado() {
        conTokenValido();
        RetryableException timeout = new RetryableException(-1, "Read timed out", Request.HttpMethod.POST,
                new SocketTimeoutException("Read timed out"), (Long) null, request());
        when(client.sendTx(any(), any())).thenThrow(timeout);

        RespuestaSendTx r = gateway.ejecutar("cifrado", "UPC0000001");

        assertEquals(Veredicto.INDETERMINADO, r.veredicto());
        assertNull(r.errorInterno());
    }

    @Test
    @DisplayName("HTTP 5xx => INDETERMINADO")
    void http500_indeterminado() {
        conTokenValido();
        when(client.sendTx(any(), any())).thenThrow(http(500));

        assertEquals(Veredicto.INDETERMINADO, gateway.ejecutar("cifrado", "UPC0000001").veredicto());
    }

    @Test
    @DisplayName("HTTP 4xx => RECHAZADO (el proveedor no acepto la peticion), sin exponer su cuerpo")
    void http400_rechazado() {
        conTokenValido();
        when(client.sendTx(any(), any())).thenThrow(http(400));

        RespuestaSendTx r = gateway.ejecutar("cifrado", "UPC0000001");

        assertEquals(Veredicto.RECHAZADO, r.veredicto());
        assertEquals(CatalogoCodigoError.ERROR_COMUNICACION_GESTOPAGO, r.errorInterno());
    }

    @Test
    @DisplayName("Respuesta vacia o ilegible con HTTP 200 => INDETERMINADO (pudo haberse aplicado)")
    void respuestaIlegible_indeterminado() {
        conTokenValido();
        when(client.sendTx(any(), any())).thenReturn("", "<RESPONSE><MENSAJE>no cierra");

        assertEquals(Veredicto.INDETERMINADO, gateway.ejecutar("cifrado", "UPC0000001").veredicto());
        assertEquals(Veredicto.INDETERMINADO, gateway.ejecutar("cifrado", "UPC0000002").veredicto());
    }

    @Test
    @DisplayName("Respuesta sin CODIGO => INDETERMINADO")
    void sinCodigo_indeterminado() {
        conTokenValido();
        when(client.sendTx(any(), any())).thenReturn("<RESPONSE><ID_TX>1</ID_TX></RESPONSE>");

        assertEquals(Veredicto.INDETERMINADO, gateway.ejecutar("cifrado", "UPC0000001").veredicto());
    }

    @Test
    @DisplayName("401: renueva el token y reintenta UNA vez con el mismo UPC (la peticion no se proceso)")
    void token401_renuevaYReintenta() {
        when(tokenService.obtenerTokenValido()).thenReturn("viejo", "nuevo");
        when(client.sendTx(eq("Bearer viejo"), any())).thenThrow(http(401));
        when(client.sendTx(eq("Bearer nuevo"), any())).thenReturn(xml("01", "OK"));

        RespuestaSendTx r = gateway.ejecutar("cifrado", "UPC0000001");

        assertEquals(Veredicto.EXITO, r.veredicto());
        verify(tokenService, times(1)).renovarToken();
    }

    @Test
    @DisplayName("Sin token disponible => RECHAZADO sin invocar sendTx")
    void sinToken_noInvocaProveedor() {
        when(tokenService.obtenerTokenValido()).thenReturn(null);

        RespuestaSendTx r = gateway.ejecutar("cifrado", "UPC0000001");

        assertEquals(Veredicto.RECHAZADO, r.veredicto());
        assertEquals(CatalogoCodigoError.TOKEN_GESTOPAGO_INVALIDO, r.errorInterno());
        assertNotNull(r);
        verify(client, never()).sendTx(any(), any());
    }

    @Test
    @DisplayName("El gateway nunca lanza: una excepcion inesperada => INDETERMINADO")
    void excepcionInesperada_indeterminado() {
        conTokenValido();
        when(client.sendTx(any(), any())).thenThrow(new IllegalStateException("boom"));

        assertEquals(Veredicto.INDETERMINADO, gateway.ejecutar("cifrado", "UPC0000001").veredicto());
    }
}
