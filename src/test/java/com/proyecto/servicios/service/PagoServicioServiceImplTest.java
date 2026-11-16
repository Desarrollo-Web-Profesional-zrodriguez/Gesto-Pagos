package com.proyecto.servicios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Saldo;
import com.proyecto.servicios.entity.cliente.SesionLogin;
import com.proyecto.servicios.entity.cliente.UsuarioLogin;
import com.proyecto.servicios.entity.gestopago.EstatusTransaccion;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.exception.AutenticacionException;
import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.SolicitudDuplicadaException;
import com.proyecto.servicios.model.email.ComprobanteEmailResult;
import com.proyecto.servicios.model.gestopago.catalogo.ProductCategorizedResponse;
import com.proyecto.servicios.model.gestopago.catalogo.ProductItemDto;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;
import com.proyecto.servicios.service.cliente.util.PasswordEncryptionUtil;
import com.proyecto.servicios.model.gestopago.transaccion.PagoEstatusResponse;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioResponse;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx.Veredicto;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse.MensajeTxXml;
import com.proyecto.servicios.model.gestopago.transaccion.ValidarReferenciaRequest;
import com.proyecto.servicios.model.gestopago.transaccion.ValidarReferenciaResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.SesionLoginRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioLoginRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.service.Impl.PagoServicioServiceImpl;
import com.proyecto.servicios.service.PagoLedgerService.Reserva;
import com.proyecto.servicios.service.PagoLedgerService.SaldosFinales;
import com.proyecto.servicios.util.AesEncryption;
import com.proyecto.servicios.util.LogoServicioResolver;

@ExtendWith(MockitoExtension.class)
class PagoServicioServiceImplTest {

    private static final String TEST_SECRET_KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String TEST_IV = "p5ldmBPdd/9pjC0bDC/nSg==";
    private static final String NUM_CUENTA_16 = "1000000001000000";
    private static final String EMBEDDING_VALIDO = "[0.12, -0.45, 0.89, -0.05, 0.67]";

    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private UsuarioLoginRepository usuarioLoginRepository;
    @Mock
    private SesionLoginRepository sesionLoginRepository;
    @Mock
    private ProductoService productoService;
    @Mock
    private PagoLedgerService ledger;
    @Mock
    private GestoPagoSendTxGateway sendTxGateway;
    @Mock
    private GestoPagoTransaccionRepository transaccionRepository;
    @Mock
    private ComprobanteEmailService comprobanteEmailService;
    @Mock
    private LogoServicioResolver logoServicioResolver;

    private PagoServicioServiceImpl pagoServicioService;

    @BeforeEach
    void setUp() {
        pagoServicioService = new PagoServicioServiceImpl(
                cuentaRepository,
                clienteRepository,
                usuarioLoginRepository,
                sesionLoginRepository,
                productoService,
                ledger,
                sendTxGateway,
                transaccionRepository,
                comprobanteEmailService,
                logoServicioResolver,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(pagoServicioService, "aesSecretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(pagoServicioService, "aesIv", TEST_IV);
        ReflectionTestUtils.setField(pagoServicioService, "unidadNegocio", "083");

        lenient().when(logoServicioResolver.resolverUrlLogo(any(), any(), any(), any()))
                .thenReturn("https://upload.wikimedia.org/wikipedia/commons/thumb/6/66/Logo_Izzi_Telecom.png/320px-Logo_Izzi_Telecom.png");
        lenient().when(comprobanteEmailService.enviarComprobantePago(any()))
                .thenReturn(ComprobanteEmailResult.builder().enviado(true).messageId("msg_test_123").build());
    }

    @Test
    @DisplayName("AesEncryption debe cifrar y descifrar correctamente un JSON con las llaves de GestoPago")
    void testAesEncryptionHelper() {
        String jsonOriginal = "{\"idServicio\":79,\"idProducto\":209,\"referencia\":\"40425475190118187271\",\"montoPago\":9999,\"telefono\":\"1111111111\",\"horaLocal\":\"20200401222821\"}";

        String cifrado = AesEncryption.AESCipher("ENCRYPT", jsonOriginal, TEST_IV, TEST_SECRET_KEY);
        assertNotNull(cifrado);

        String descifrado = AesEncryption.AESCipher("DECRYPT", cifrado, TEST_IV, TEST_SECRET_KEY);
        assertEquals(jsonOriginal, descifrado);
    }

    // ------------------------------------------------------------------ caminos de éxito

    @Test
    @DisplayName("Pago exitoso (01): reserva atomica, cifra el payload, confirma el cargo y envia comprobante")
    void testProcesarPago_Exitoso_Codigo01() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuestaExito());
        when(ledger.confirmarExito(eq(NUM_CUENTA_16), anyString(), any()))
                .thenReturn(new SaldosFinales(new BigDecimal("850.00"), new BigDecimal("850.00")));

        PagoServicioResponse response = pagoServicioService.procesarPago(requestValido());

        assertEquals("01", response.getCodigo());
        assertEquals("Pago Exitoso", response.getMensaje());
        assertEquals("291620941", response.getIdTx());
        assertEquals("100074211", response.getNumAutorizacion());
        assertEquals(10, response.getUpc().length());
        assertEquals(new BigDecimal("1000.00"), response.getSaldoAnterior());
        assertEquals(new BigDecimal("850.00"), response.getSaldoDisponibleActual());
        assertEquals(new BigDecimal("850.00"), response.getSaldoContableActual());
        assertTrue(response.getEmailEnviado());
        assertNotNull(response.getUrlLogo());

        verify(ledger, times(1)).reservar(any());
        verify(ledger, never()).liberarRetencion(any(), any(), any());
        verify(comprobanteEmailService, times(1)).enviarComprobantePago(any());
    }

    @Test
    @DisplayName("Registra la transaccion con Idempotency-Key, huella SHA-256 y payload cifrado antes de llamar al proveedor")
    void testProcesarPago_RegistroIncluyeIdempotenciaYHuella() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuestaExito());
        when(ledger.confirmarExito(any(), any(), any()))
                .thenReturn(new SaldosFinales(new BigDecimal("850.00"), new BigDecimal("850.00")));

        pagoServicioService.procesarPago(requestValido(), "clave-idempotencia-001");

        ArgumentCaptor<GestoPagoTransaccion> captor = ArgumentCaptor.forClass(GestoPagoTransaccion.class);
        verify(ledger).reservar(captor.capture());
        GestoPagoTransaccion registro = captor.getValue();
        assertEquals("clave-idempotencia-001", registro.getIdempotencyKey());
        assertEquals(64, registro.getHuella().length());
        assertEquals("PROCESANDO", registro.getEstatus());
        assertNotNull(registro.getPayloadCifrado());
        assertEquals(new BigDecimal("150.00"), registro.getMonto());
    }

    @Test
    @DisplayName("Un fallo de Brevo no interrumpe ni revierte un pago ya aplicado")
    void testProcesarPago_BrevoFalla_NoInterrumpePago() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuestaExito());
        when(ledger.confirmarExito(any(), any(), any()))
                .thenReturn(new SaldosFinales(new BigDecimal("850.00"), new BigDecimal("850.00")));
        when(comprobanteEmailService.enviarComprobantePago(any())).thenThrow(new RuntimeException("Simulated Brevo timeout"));

        PagoServicioResponse response = pagoServicioService.procesarPago(requestValido());

        assertEquals("01", response.getCodigo());
        assertFalse(response.getEmailEnviado());
        verify(ledger, never()).liberarRetencion(any(), any(), any());
    }

    @Test
    @DisplayName("Si el proveedor aplico el pago pero falla el commit local, se responde exito y se deja a conciliacion (sin reversar)")
    void testProcesarPago_FalloLocalTrasExito_NoReversa() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuestaExito());
        when(ledger.confirmarExito(any(), any(), any())).thenThrow(new IllegalStateException("BD caida"));

        PagoServicioResponse response = pagoServicioService.procesarPago(requestValido());

        assertEquals("01", response.getCodigo());
        assertNull(response.getSaldoContableActual());
        verify(ledger, never()).liberarRetencion(any(), any(), any());
    }

    // ------------------------------------------------------------------ escenarios del proveedor

    @Test
    @DisplayName("Rechazo explicito (06) libera la retencion y no expone el texto crudo del proveedor")
    void testProcesarPago_RechazoExplicito_LiberaSaldo() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("500.00"), new BigDecimal("350.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(
                respuesta(Veredicto.RECHAZADO, "06", "Transaccion aplicada anteriormente | Autorizacion [100016171]", null));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(requestValido()));

        assertEquals(6, ex.getCodigoPersonalizado());
        assertEquals("06", ex.getCodigoGestoPago());
        assertEquals(CatalogoCodigoError.GESTOPAGO_06_TRANSACCION_DUPLICADA.getDescripcion(), ex.getMessage());
        verify(ledger, times(1)).liberarRetencion(eq(NUM_CUENTA_16), anyString(), any());
        verify(ledger, never()).confirmarExito(any(), any(), any());
        verify(ledger, never()).marcarPendiente(any(), any());
    }

    @Test
    @DisplayName("IP origen no valida (55) se responde 502 sin filtrar la IP y libera el saldo")
    void testProcesarPago_Codigo55_NoFiltraIp() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("500.00"), new BigDecimal("350.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(
                respuesta(Veredicto.RECHAZADO, "55", "IP Origen no valida. [187.243.48.32]", null));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(requestValido()));

        assertEquals(CatalogoCodigoError.GESTOPAGO_55_ORIGEN_NO_AUTORIZADO, ex.getCatalogoError());
        assertEquals(org.springframework.http.HttpStatus.BAD_GATEWAY, ex.getCatalogoError().getHttpStatus());
        assertFalse(ex.getMessage().contains("187.243"));
        verify(ledger).liberarRetencion(eq(NUM_CUENTA_16), anyString(), any());
    }

    @Test
    @DisplayName("Resultado indeterminado (timeout/5xx): NO libera el saldo, queda pendiente y responde 202")
    void testProcesarPago_Indeterminado_ConservaRetencion() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("500.00"), new BigDecimal("350.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuesta(Veredicto.INDETERMINADO, null, null, null));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(requestValido()));

        assertEquals(CatalogoCodigoError.PAGO_PENDIENTE_CONCILIACION, ex.getCatalogoError());
        assertEquals(org.springframework.http.HttpStatus.ACCEPTED, ex.getCatalogoError().getHttpStatus());
        assertTrue(ex.getMessage().contains("UPC"));
        verify(ledger, times(1)).marcarPendiente(anyString(), any());
        verify(ledger, never()).liberarRetencion(any(), any(), any());
        verify(ledger, never()).confirmarExito(any(), any(), any());
    }

    @Test
    @DisplayName("Si la solicitud no llego al proveedor (token no disponible) se libera el saldo con error 503 del catalogo")
    void testProcesarPago_TokenNoDisponible_LiberaSaldo() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("500.00"), new BigDecimal("350.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(
                respuesta(Veredicto.RECHAZADO, null, null, CatalogoCodigoError.TOKEN_GESTOPAGO_INVALIDO));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(requestValido()));

        assertEquals(CatalogoCodigoError.TOKEN_GESTOPAGO_INVALIDO, ex.getCatalogoError());
        verify(ledger).liberarRetencion(eq(NUM_CUENTA_16), anyString(), any());
    }

    // ------------------------------------------------------------------ validaciones previas (nunca tocan saldo ni proveedor)

    @Test
    @DisplayName("Rechaza el pago cuando el servicio no existe en /productos")
    void testProcesarPago_ServicioNoExisteEnCatalogo() {
        configurarCatalogo(BigDecimal.ZERO);
        PagoServicioRequest request = requestValido();
        request.setIdServicio(9999L);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class, () -> pagoServicioService.procesarPago(request));

        assertEquals(101, ex.getCatalogoError().getCodigo());
        verifyNoInteractions(ledger, sendTxGateway);
    }

    @Test
    @DisplayName("Rechaza el pago cuando el producto no pertenece al servicio")
    void testProcesarPago_ProductoNoPerteneceAlServicio() {
        configurarCatalogo(BigDecimal.ZERO);
        PagoServicioRequest request = requestValido();
        request.setIdProducto(9999L);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class, () -> pagoServicioService.procesarPago(request));

        assertEquals(102, ex.getCatalogoError().getCodigo());
        verifyNoInteractions(ledger, sendTxGateway);
    }

    @Test
    @DisplayName("Producto de monto fijo (tipoFront=1): rechaza un monto distinto al precio del catalogo (monto manipulado)")
    void testProcesarPago_MontoManipuladoContraPrecioFijo() {
        configurarCatalogo(1, new BigDecimal("100.00"));
        PagoServicioRequest request = requestValido(); // 150.00

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));

        assertEquals(CatalogoCodigoError.MONTO_NO_PERMITIDO, ex.getCatalogoError());
        verifyNoInteractions(ledger, sendTxGateway);
    }

    @Test
    @DisplayName("Producto de monto fijo (tipoFront=1): acepta exactamente el precio del catalogo")
    void testProcesarPago_MontoIgualAPrecioFijo() {
        configurarCatalogo(1, new BigDecimal("150.00"));
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuestaExito());
        when(ledger.confirmarExito(any(), any(), any()))
                .thenReturn(new SaldosFinales(new BigDecimal("850.00"), new BigDecimal("850.00")));

        assertEquals("01", pagoServicioService.procesarPago(requestValido()).getCodigo());
    }

    @Test
    @DisplayName("Pago de servicios (tipoFront=2): el precio del catalogo es comision y permite pagar el monto del recibo")
    void testProcesarPago_TipoFront2_PrecioEsComisionYPermiteMontoRecibo() {
        configurarCatalogo(2, new BigDecimal("10.00"));
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("398.01")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuestaExito());
        when(ledger.confirmarExito(any(), any(), any()))
                .thenReturn(new SaldosFinales(new BigDecimal("398.01"), new BigDecimal("398.01")));

        PagoServicioRequest request = requestValido();
        request.setMonto(new BigDecimal("601.99"));

        assertEquals("01", pagoServicioService.procesarPago(request).getCodigo());
    }

    @Test
    @DisplayName("Producto de monto abierto: rechaza montos fuera del rango permitido")
    void testProcesarPago_MontoFueraDeRangoAbierto() {
        configurarCatalogo(BigDecimal.ZERO);
        PagoServicioRequest masAlto = requestValido();
        masAlto.setMonto(new BigDecimal("10000.01"));
        PagoServicioRequest masBajo = requestValido();
        masBajo.setMonto(new BigDecimal("0.50"));

        assertEquals(CatalogoCodigoError.MONTO_NO_PERMITIDO,
                assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(masAlto)).getCatalogoError());
        assertEquals(CatalogoCodigoError.MONTO_NO_PERMITIDO,
                assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(masBajo)).getCatalogoError());
        verifyNoInteractions(ledger, sendTxGateway);
    }

    @Test
    @DisplayName("Rechaza el pago cuando el 2FA no coincide con el titular y no reserva saldo")
    void testProcesarPago_2FAInvalido() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        PagoServicioRequest request = requestValido();
        request.setCodigo2fa("[-0.99, 0.99, -0.50, 0.40, -0.80]");

        AutenticacionException ex = assertThrows(AutenticacionException.class, () -> pagoServicioService.procesarPago(request));

        assertEquals(401, ex.getCatalogoError().getCodigo());
        verifyNoInteractions(ledger, sendTxGateway);
    }

    @Test
    @DisplayName("Rechaza una cuenta inactiva antes de reservar saldo")
    void testProcesarPago_CuentaInactiva() {
        configurarCatalogo(BigDecimal.ZERO);
        Cuenta inactiva = Cuenta.builder().idCuenta(1L).numeroCuenta(NUM_CUENTA_16).estatus("BLOQUEADA").build();
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(inactiva));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(requestValido()));

        assertEquals(CatalogoCodigoError.CUENTA_INACTIVA, ex.getCatalogoError());
        verifyNoInteractions(ledger, sendTxGateway);
    }

    @Test
    @DisplayName("Rechaza una Idempotency-Key con formato invalido")
    void testProcesarPago_IdempotencyKeyInvalida() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> pagoServicioService.procesarPago(requestValido(), "x"));

        assertEquals(CatalogoCodigoError.DATOS_ENTRADA_INVALIDOS, ex.getCatalogoError());
        verifyNoInteractions(ledger, sendTxGateway);
    }

    @Test
    @DisplayName("Una solicitud duplicada detectada por el ledger jamas llega al proveedor")
    void testProcesarPago_DuplicadaNoLlegaAlProveedor() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenThrow(new SolicitudDuplicadaException("ABCDE12345", "PROCESANDO"));

        assertThrows(SolicitudDuplicadaException.class, () -> pagoServicioService.procesarPago(requestValido(), "clave-idempotencia-001"));

        verifyNoInteractions(sendTxGateway);
    }

    // ------------------------------------------------------------------ consulta de estatus

    @Test
    @DisplayName("Consultar estatus de un UPC inexistente devuelve 104")
    void testConsultarEstatus_NoExiste() {
        when(transaccionRepository.findByUpc("ABCDE12345")).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> pagoServicioService.consultarEstatus("ABCDE12345"));

        assertEquals(104, ex.getCatalogoError().getCodigo());
    }

    @Test
    @DisplayName("Consultar estatus indica si el resultado es definitivo y no expone payloads")
    void testConsultarEstatus_Pendiente() {
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("ABCDE12345").estatus("PENDIENTE_CONCILIACION").monto(new BigDecimal("150.00")).build();
        when(transaccionRepository.findByUpc("ABCDE12345")).thenReturn(Optional.of(tx));

        PagoEstatusResponse response = pagoServicioService.consultarEstatus("ABCDE12345");

        assertEquals("PENDIENTE_CONCILIACION", response.getEstatus());
        assertFalse(response.isDefinitivo());
        assertNull(response.getNumAutorizacion());
    }

    // ------------------------------------------------------------------ validacion de referencias e historial

    @Test
    @DisplayName("Validar referencia valida en servicio tipoFront=2 retorna resumen completo con saldo suficiente")
    void testValidarReferencia_Exitosa() {
        configurarCatalogo(2, new BigDecimal("10.00"));
        configurarClienteYCuentaActivosConSaldo(1000.00);

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .referencia("25720381")
                .monto(new BigDecimal("150.00"))
                .build();

        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);

        assertTrue(resp.isValida());
        assertEquals("CFE", resp.getServicio());
        assertEquals("Pago CFE", resp.getProducto());
        assertEquals("25720381", resp.getReferenciaNormalizada());
        assertEquals(new BigDecimal("150.00"), resp.getMontoValidado());
        assertEquals(new BigDecimal("10.00"), resp.getComisionEstimada());
        assertTrue(resp.getSaldoSuficiente());
        assertNotNull(resp.getUrlLogo());
        verifyNoInteractions(ledger, sendTxGateway);
    }

    @Test
    @DisplayName("Validar referencia invalida (solo ceros o muy corta) rechaza con codigo 55")
    void testValidarReferencia_Invalida_RechazaCon55() {
        configurarCatalogo(2, BigDecimal.ZERO);

        ValidarReferenciaRequest reqCeros = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .referencia("00000000")
                .build();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> pagoServicioService.validarReferencia(reqCeros));

        assertEquals(CatalogoCodigoError.GESTOPAGO_55_REFERENCIA_INVALIDA, ex.getCatalogoError());
        verifyNoInteractions(ledger, sendTxGateway);
    }

    @Test
    @DisplayName("Historial por cuenta enriquece cada transaccion con nombre de servicio, producto y logo")
    void testObtenerHistorialPorCuenta_Enriquecido() {
        configurarCatalogo(2, new BigDecimal("10.00"));
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("ABCDE12345")
                .numeroCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .referencia("25720381")
                .telefono("1111111111")
                .monto(new BigDecimal("150.00"))
                .estatus("EXITOSA")
                .build();
        when(transaccionRepository.findByNumeroCuentaOrderByFechaCreacionDesc(NUM_CUENTA_16))
                .thenReturn(List.of(tx));

        List<GestoPagoTransaccion> historial = pagoServicioService.obtenerHistorialPorCuenta(NUM_CUENTA_16);

        assertEquals(1, historial.size());
        assertEquals("CFE", historial.get(0).getServicio());
        assertEquals("Pago CFE", historial.get(0).getProducto());
        assertNotNull(historial.get(0).getUrlLogo());
    }

    // ==================================================================
    // BRANCH HARDENING TESTS (M3 Coverage Expansion)
    // ==================================================================

    @Test
    @DisplayName("ValidarReferencia: tipoFront=1 PIN/Entretenimiento asigna telefono y referencia por defecto")
    void testValidarReferencia_TipoFront1_PinOEntretenimiento() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("Xbox")
                .idProducto(205L)
                .producto("Pin Xbox")
                .tipoFront(1)
                .idCatTipoServicio(10)
                .precio(new BigDecimal("100.00"))
                .build();
        ProductCategorizedResponse catResp = ProductCategorizedResponse.builder()
                .status(200)
                .totalProductos(1)
                .categorias(Map.of(1, List.of(prod)))
                .build();
        when(productoService.obtenerProductosCategorizados()).thenReturn(catResp);
        configurarClienteYCuentaActivos();

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .telefono("")
                .referencia("")
                .build();

        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);
        assertEquals("#1111111111", resp.getTelefonoNormalizado());
        assertEquals("#1111111111", resp.getReferenciaNormalizada());
    }

    @Test
    @DisplayName("ValidarReferencia: tipoFront=1 recarga con prefijo '#' normaliza telefono correctamente")
    void testValidarReferencia_TipoFront1_RecargaConPrefijoHash_Exitoso() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("Telcel")
                .idProducto(205L)
                .producto("Recarga 50")
                .tipoFront(1)
                .idCatTipoServicio(1)
                .precio(new BigDecimal("50.00"))
                .build();
        ProductCategorizedResponse catResp = ProductCategorizedResponse.builder()
                .status(200)
                .totalProductos(1)
                .categorias(Map.of(1, List.of(prod)))
                .build();
        when(productoService.obtenerProductosCategorizados()).thenReturn(catResp);
        configurarClienteYCuentaActivos();

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .telefono("#5512345678")
                .referencia("miRef")
                .build();

        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);
        assertEquals("5512345678", resp.getTelefonoNormalizado());
        assertEquals("miRef", resp.getReferenciaNormalizada());
    }

    @Test
    @DisplayName("ValidarReferencia: tipoFront=1 recarga con telefono de longitud invalida o ceros rechaza con 55")
    void testValidarReferencia_TipoFront1_RecargaTelefonoInvalido_Rechaza55() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("Telcel")
                .idProducto(205L)
                .producto("Recarga 50")
                .tipoFront(1)
                .idCatTipoServicio(1)
                .precio(new BigDecimal("50.00"))
                .build();
        ProductCategorizedResponse catResp = ProductCategorizedResponse.builder()
                .status(200)
                .totalProductos(1)
                .categorias(Map.of(1, List.of(prod)))
                .build();
        when(productoService.obtenerProductosCategorizados()).thenReturn(catResp);

        ValidarReferenciaRequest reqCorta = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .telefono("12345")
                .build();
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.validarReferencia(reqCorta));

        ValidarReferenciaRequest reqCeros = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .telefono("0000000000")
                .build();
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.validarReferencia(reqCeros));
    }

    @Test
    @DisplayName("ValidarReferencia: tipoFront=2 referencia invalida o con caracteres especiales rechaza con 55")
    void testValidarReferencia_TipoFront2_ReferenciaInvalida_Rechaza55() {
        configurarCatalogo(2, new BigDecimal("10.00"));

        ValidarReferenciaRequest reqCorta = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .referencia("12")
                .build();
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.validarReferencia(reqCorta));

        ValidarReferenciaRequest reqEspeciales = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .referencia("ref$$$")
                .build();
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.validarReferencia(reqEspeciales));
    }

    @Test
    @DisplayName("ValidarReferencia: tipoFront=2 asigna telefono por defecto si no es provisto")
    void testValidarReferencia_TipoFront2_TelefonoDefectoServicios() {
        configurarCatalogo(2, new BigDecimal("10.00"));
        configurarClienteYCuentaActivos();

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .referencia("REF1234")
                .telefono("")
                .build();

        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);
        assertEquals("1111111111", resp.getTelefonoNormalizado());
    }

    @Test
    @DisplayName("ValidarReferencia: cuenta inexistente o inactiva lanza excepcion")
    void testValidarReferencia_CuentaNoEncontradaOInactiva() {
        configurarCatalogo(2, new BigDecimal("10.00"));
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.empty());

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .referencia("REF1234")
                .build();
        assertThrows(CuentaNoEncontradaException.class, () -> pagoServicioService.validarReferencia(req));

        Cuenta cuentaInactiva = Cuenta.builder().idCuenta(1L).numeroCuenta(NUM_CUENTA_16).estatus("BLOQUEADA").build();
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cuentaInactiva));
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.validarReferencia(req));
    }

    @Test
    @DisplayName("ValidarReferencia: cliente no asociado o inactivo lanza excepcion")
    void testValidarReferencia_ClienteNoEncontradoOInactivo() {
        configurarCatalogo(2, new BigDecimal("10.00"));
        Cuenta cuenta = Cuenta.builder().idCuenta(1L).numeroCuenta(NUM_CUENTA_16).estatus("ACTIVA").build();
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cuenta));
        when(clienteRepository.findByCuentasNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.empty());

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .referencia("REF1234")
                .build();
        assertThrows(ClienteNoEncontradoException.class, () -> pagoServicioService.validarReferencia(req));

        Cliente clienteInactivo = Cliente.builder().idCliente(10L).activo(false).build();
        when(clienteRepository.findByCuentasNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(clienteInactivo));
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.validarReferencia(req));
    }

    @Test
    @DisplayName("ValidarReferencia: saldo insuficiente reporta saldoSuficiente=false")
    void testValidarReferencia_SaldoInsuficiente() {
        configurarCatalogo(2, new BigDecimal("10.00"));
        configurarClienteYCuentaActivosConSaldo(50.00);

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .referencia("REF1234")
                .monto(new BigDecimal("150.00"))
                .build();

        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);
        assertFalse(resp.getSaldoSuficiente());
    }

    @Test
    @DisplayName("ValidarReferencia: showAyuda true resuelve urlReferenciaAyuda")
    void testValidarReferencia_ShowAyudaTrue_ResuelveUrlAyuda() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("CFE")
                .idProducto(205L)
                .producto("Pago CFE")
                .tipoFront(2)
                .showAyuda(true)
                .build();
        ProductCategorizedResponse catResp = ProductCategorizedResponse.builder()
                .status(200)
                .totalProductos(1)
                .categorias(Map.of(2, List.of(prod)))
                .build();
        when(productoService.obtenerProductosCategorizados()).thenReturn(catResp);
        configurarClienteYCuentaActivos();
        when(logoServicioResolver.resolverUrlReferenciaPuntoRed(76L)).thenReturn("https://img.com/ayuda.png");

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .referencia("REF1234")
                .build();

        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);
        assertEquals("https://img.com/ayuda.png", resp.getUrlReferenciaAyuda());
    }

    @Test
    @DisplayName("ValidarReferencia: producto de precio fijo sin monto en request usa precio de catalogo")
    void testValidarReferencia_PrecioFijoSinMontoRequest_UsaPrecioCatalogo() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("Telcel")
                .idProducto(205L)
                .producto("Recarga 100")
                .tipoFront(1)
                .idCatTipoServicio(1)
                .precio(new BigDecimal("100.00"))
                .build();
        ProductCategorizedResponse catResp = ProductCategorizedResponse.builder()
                .status(200)
                .totalProductos(1)
                .categorias(Map.of(1, List.of(prod)))
                .build();
        when(productoService.obtenerProductosCategorizados()).thenReturn(catResp);
        configurarClienteYCuentaActivos();

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .telefono("5512345678")
                .monto(null)
                .build();

        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);
        assertEquals(new BigDecimal("100.00"), resp.getMontoValidado());
    }

    @Test
    @DisplayName("ProcesarPago 2FA: codigo nulo o vacio lanza AutenticacionException")
    void testProcesarPago_2FA_VacioONulo_LanzaAutenticacionException() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();

        PagoServicioRequest req = requestValido();
        req.setCodigo2fa(null);
        assertThrows(AutenticacionException.class, () -> pagoServicioService.procesarPago(req, "clave-idemp-001"));

        req.setCodigo2fa("   ");
        assertThrows(AutenticacionException.class, () -> pagoServicioService.procesarPago(req, "clave-idemp-001"));
    }

    @Test
    @DisplayName("ProcesarPago 2FA: usuario login no registrado lanza AutenticacionException")
    void testProcesarPago_2FA_UsuarioNoExiste_LanzaAutenticacionException() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(usuarioLoginRepository.findByClienteIdCliente(10L)).thenReturn(Optional.empty());

        assertThrows(AutenticacionException.class, () -> pagoServicioService.procesarPago(requestValido(), "clave-idemp-001"));
    }

    @Test
    @DisplayName("ProcesarPago 2FA: usuario login inactivo lanza AutenticacionException")
    void testProcesarPago_2FA_UsuarioInactivo_LanzaAutenticacionException() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        UsuarioLogin usuarioInactivo = UsuarioLogin.builder().idUsuario(100L).activo(false).build();
        when(usuarioLoginRepository.findByClienteIdCliente(10L)).thenReturn(Optional.of(usuarioInactivo));

        assertThrows(AutenticacionException.class, () -> pagoServicioService.procesarPago(requestValido(), "clave-idemp-001"));
    }

    @Test
    @DisplayName("ProcesarPago 2FA: sesion activa valida aprueba el pago y renueva ultimoAcceso")
    void testProcesarPago_2FA_SesionActivaVigente_Exitoso() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        UsuarioLogin usuarioLogin = UsuarioLogin.builder().idUsuario(100L).activo(true).build();
        when(usuarioLoginRepository.findByClienteIdCliente(10L)).thenReturn(Optional.of(usuarioLogin));

        SesionLogin sesion = SesionLogin.builder()
                .tokenSesion("sesion-activa-123")
                .activa(true)
                .ultimoAcceso(LocalDateTime.now().minusMinutes(1))
                .build();
        when(sesionLoginRepository.findByUsuarioIdUsuarioAndActivaTrue(100L)).thenReturn(List.of(sesion));

        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuestaExito());
        when(ledger.confirmarExito(any(), any(), any())).thenReturn(new SaldosFinales(new BigDecimal("850.00"), new BigDecimal("850.00")));

        PagoServicioRequest req = requestValido();
        req.setCodigo2fa("Bearer sesion-activa-123");

        PagoServicioResponse resp = pagoServicioService.procesarPago(req, "clave-idemp-001");
        assertNotNull(resp);
        verify(sesionLoginRepository).save(sesion);
    }

    @Test
    @DisplayName("ProcesarPago 2FA: sesion expirada por inactividad (>5 min) inactiva sesion y lanza excepcion")
    void testProcesarPago_2FA_SesionExpirada_LanzaAutenticacionException() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        UsuarioLogin usuarioLogin = UsuarioLogin.builder().idUsuario(100L).activo(true).build();
        when(usuarioLoginRepository.findByClienteIdCliente(10L)).thenReturn(Optional.of(usuarioLogin));

        SesionLogin sesion = SesionLogin.builder()
                .tokenSesion("sesion-expirada-123")
                .activa(true)
                .ultimoAcceso(LocalDateTime.now().minusMinutes(10))
                .build();
        when(sesionLoginRepository.findByUsuarioIdUsuarioAndActivaTrue(100L)).thenReturn(List.of(sesion));

        PagoServicioRequest req = requestValido();
        req.setCodigo2fa("sesion-expirada-123");

        assertThrows(AutenticacionException.class, () -> pagoServicioService.procesarPago(req, "clave-idemp-001"));
        assertFalse(sesion.getActiva());
        verify(sesionLoginRepository).save(sesion);
    }

    @Test
    @DisplayName("ProcesarPago 2FA: validacion por contrasena Bcrypt exitosa")
    void testProcesarPago_2FA_PasswordBcrypt_Exitoso() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        String passHash = PasswordEncryptionUtil.cifrarPassword("PassCorrecto123*");
        UsuarioLogin usuarioLogin = UsuarioLogin.builder().idUsuario(100L).passwordHash(passHash).activo(true).build();
        when(usuarioLoginRepository.findByClienteIdCliente(10L)).thenReturn(Optional.of(usuarioLogin));
        when(sesionLoginRepository.findByUsuarioIdUsuarioAndActivaTrue(100L)).thenReturn(List.of());

        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuestaExito());
        when(ledger.confirmarExito(any(), any(), any())).thenReturn(new SaldosFinales(new BigDecimal("850.00"), new BigDecimal("850.00")));

        PagoServicioRequest req = requestValido();
        req.setCodigo2fa("PassCorrecto123*");

        PagoServicioResponse resp = pagoServicioService.procesarPago(req, "clave-idemp-001");
        assertNotNull(resp);
    }

    @Test
    @DisplayName("ProcesarPago 2FA: todas las opciones fallan lanza AutenticacionException")
    void testProcesarPago_2FA_TodasOpcionesFallan_LanzaAutenticacionException() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        UsuarioLogin usuarioLogin = UsuarioLogin.builder()
                .idUsuario(100L)
                .passwordHash(PasswordEncryptionUtil.cifrarPassword("OtroPassword"))
                .biometricoFacialEmbedding("[0.0, 1.0]")
                .activo(true)
                .build();
        when(usuarioLoginRepository.findByClienteIdCliente(10L)).thenReturn(Optional.of(usuarioLogin));
        when(sesionLoginRepository.findByUsuarioIdUsuarioAndActivaTrue(100L)).thenReturn(List.of());

        PagoServicioRequest req = requestValido();
        req.setCodigo2fa("[1.0, 0.0]");

        assertThrows(AutenticacionException.class, () -> pagoServicioService.procesarPago(req, "clave-idemp-001"));
    }

    @Test
    @DisplayName("ProcesarPago: cliente titular inexistente o inactivo lanza excepcion")
    void testProcesarPago_ClienteNoEncontradoOInactivo() {
        configurarCatalogo(BigDecimal.ZERO);
        Cuenta cuenta = Cuenta.builder().idCuenta(1L).numeroCuenta(NUM_CUENTA_16).estatus("ACTIVA").build();
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cuenta));
        when(clienteRepository.findByCuentasNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> pagoServicioService.procesarPago(requestValido(), "clave-idemp-001"));

        Cliente clienteInactivo = Cliente.builder().idCliente(10L).activo(false).build();
        when(clienteRepository.findByCuentasNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(clienteInactivo));
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(requestValido(), "clave-idemp-001"));
    }

    @Test
    @DisplayName("ProcesarPago: tipoFront=1 PIN/Entretenimiento procesa pago con payload de telefono por defecto")
    void testProcesarPago_TipoFront1_PinOEntretenimiento_Exitoso() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("Xbox")
                .idProducto(205L)
                .producto("Pin 100")
                .tipoFront(1)
                .idCatTipoServicio(10)
                .precio(new BigDecimal("100.00"))
                .build();
        ProductCategorizedResponse catResp = ProductCategorizedResponse.builder()
                .status(200)
                .totalProductos(1)
                .categorias(Map.of(1, List.of(prod)))
                .build();
        when(productoService.obtenerProductosCategorizados()).thenReturn(catResp);
        configurarClienteYCuentaActivos();

        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("900.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuestaExito());
        when(ledger.confirmarExito(any(), any(), any())).thenReturn(new SaldosFinales(new BigDecimal("900.00"), new BigDecimal("900.00")));

        PagoServicioRequest req = requestValido();
        req.setMonto(new BigDecimal("100.00"));
        req.setTelefono("");
        req.setReferencia("");

        PagoServicioResponse resp = pagoServicioService.procesarPago(req, "clave-idemp-001");
        assertNotNull(resp);
    }

    @Test
    @DisplayName("ProcesarPago: tipoFront=1 recarga con telefono no de 10 digitos lanza ReglaNegocioException")
    void testProcesarPago_TipoFront1_RecargaTelefonoInvalido_LanzaReglaNegocio() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("Telcel")
                .idProducto(205L)
                .producto("Recarga 50")
                .tipoFront(1)
                .idCatTipoServicio(1)
                .precio(new BigDecimal("50.00"))
                .build();
        ProductCategorizedResponse catResp = ProductCategorizedResponse.builder()
                .status(200)
                .totalProductos(1)
                .categorias(Map.of(1, List.of(prod)))
                .build();
        when(productoService.obtenerProductosCategorizados()).thenReturn(catResp);
        configurarClienteYCuentaActivos();

        PagoServicioRequest req = requestValido();
        req.setMonto(new BigDecimal("50.00"));
        req.setTelefono("123");
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(req, "clave-idemp-001"));
    }

    @Test
    @DisplayName("ProcesarPago: tipoFront=2 referencia vacia lanza ReglaNegocioException")
    void testProcesarPago_TipoFront2_ReferenciaVacia_LanzaReglaNegocio() {
        configurarCatalogo(2, BigDecimal.ZERO);
        configurarClienteYCuentaActivos();

        PagoServicioRequest req = requestValido();
        req.setReferencia("");
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(req, "clave-idemp-001"));
    }

    @Test
    @DisplayName("ProcesarPago: idempotencyKey nula o en blanco permite continuar")
    void testProcesarPago_IdempotencyKeyNulaOBlanco_PermiteContinuar() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuestaExito());
        when(ledger.confirmarExito(any(), any(), any())).thenReturn(new SaldosFinales(new BigDecimal("850.00"), new BigDecimal("850.00")));

        assertNotNull(pagoServicioService.procesarPago(requestValido(), null));
        assertNotNull(pagoServicioService.procesarPago(requestValido(), "    "));
    }

    @Test
    @DisplayName("ProcesarPago: extraer idTx de mensaje XML si no esta en raiz y parseo de comision")
    void testProcesarPago_Exitoso_ExtraeIdTxDeMensajeXml() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));

        SendTxXmlResponse xml = SendTxXmlResponse.builder()
                .idTx(null)
                .numAutorizacion("100074211")
                .comision("1,250.50")
                .fecha("23/Oct/2026 18:55:00")
                .mensaje(new MensajeTxXml("01", "Operacion exitosa", "25720381", "PIN-1234", "Legend instrucciones", "987654321"))
                .build();
        RespuestaSendTx respSend = new RespuestaSendTx(Veredicto.EXITO, "01", "Operacion exitosa", "<RESPONSE/>", xml, null);
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respSend);
        when(ledger.confirmarExito(any(), any(), any())).thenReturn(new SaldosFinales(new BigDecimal("850.00"), new BigDecimal("850.00")));

        PagoServicioResponse resp = pagoServicioService.procesarPago(requestValido(), "clave-idemp-001");
        assertEquals("987654321", resp.getIdTx());
        assertEquals("PIN-1234", resp.getPin());
        assertEquals("Legend instrucciones", resp.getLegend());
        assertEquals(new BigDecimal("1250.50"), resp.getComision());
    }

    @Test
    @DisplayName("ProcesarPago: confirmarExito local fallido captura excepcion y no interrumpe respuesta")
    void testProcesarPago_Exitoso_ConfirmarExitoFalla_SeRegistraSinFallar() {
        configurarCatalogo(BigDecimal.ZERO);
        configurarClienteYCuentaActivos();
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuestaExito());
        when(ledger.confirmarExito(any(), any(), any())).thenThrow(new RuntimeException("Error BD local tras exito proveedor"));

        PagoServicioResponse resp = pagoServicioService.procesarPago(requestValido(), "clave-idemp-001");
        assertNotNull(resp);
        assertEquals(new BigDecimal("850.00"), resp.getSaldoDisponibleActual());
    }

    @Test
    @DisplayName("ConsultarEstatus: cubre estados EXITOSA, RECHAZADA, PROCESANDO y REVISION_MANUAL")
    void testConsultarEstatus_EstadosVarios() {
        GestoPagoTransaccion txExitosa = GestoPagoTransaccion.builder()
                .upc("UPC_EXITOSA").estatus("EXITOSA").numAutorizacion("AUT123").monto(new BigDecimal("100.00")).build();
        when(transaccionRepository.findByUpc("UPC_EXITOSA")).thenReturn(Optional.of(txExitosa));
        PagoEstatusResponse rExitosa = pagoServicioService.consultarEstatus("UPC_EXITOSA");
        assertTrue(rExitosa.isDefinitivo());
        assertEquals("AUT123", rExitosa.getNumAutorizacion());
        assertEquals("El pago fue aplicado correctamente", rExitosa.getMensaje());

        GestoPagoTransaccion txRechazada = GestoPagoTransaccion.builder()
                .upc("UPC_RECHAZADA").estatus("RECHAZADA").monto(new BigDecimal("100.00")).build();
        when(transaccionRepository.findByUpc("UPC_RECHAZADA")).thenReturn(Optional.of(txRechazada));
        PagoEstatusResponse rRechazada = pagoServicioService.consultarEstatus("UPC_RECHAZADA");
        assertTrue(rRechazada.isDefinitivo());
        assertNull(rRechazada.getNumAutorizacion());
        assertTrue(rRechazada.getMensaje().contains("El pago no fue aplicado"));

        GestoPagoTransaccion txProcesando = GestoPagoTransaccion.builder()
                .upc("UPC_PROC").estatus("PROCESANDO").monto(new BigDecimal("100.00")).build();
        when(transaccionRepository.findByUpc("UPC_PROC")).thenReturn(Optional.of(txProcesando));
        PagoEstatusResponse rProc = pagoServicioService.consultarEstatus("UPC_PROC");
        assertFalse(rProc.isDefinitivo());

        GestoPagoTransaccion txRevision = GestoPagoTransaccion.builder()
                .upc("UPC_REV").estatus("REVISION_MANUAL").monto(new BigDecimal("100.00")).build();
        when(transaccionRepository.findByUpc("UPC_REV")).thenReturn(Optional.of(txRevision));
        PagoEstatusResponse rRev = pagoServicioService.consultarEstatus("UPC_REV");
        assertFalse(rRev.isDefinitivo());
        assertTrue(rRev.getMensaje().contains("requiere revision del equipo de soporte"));
    }

    @Test
    @DisplayName("HistorialPorCuenta: lista vacia o nula retorna inmediatamente")
    void testObtenerHistorialPorCuenta_VacioONulo() {
        when(transaccionRepository.findByNumeroCuentaOrderByFechaCreacionDesc("1111")).thenReturn(Collections.emptyList());
        assertTrue(pagoServicioService.obtenerHistorialPorCuenta("1111").isEmpty());

        when(transaccionRepository.findByNumeroCuentaOrderByFechaCreacionDesc("2222")).thenReturn(null);
        assertNull(pagoServicioService.obtenerHistorialPorCuenta("2222"));
    }

    @Test
    @DisplayName("HistorialPorCuenta: fallo en catalogo utiliza valores fallback sin fallar")
    void testObtenerHistorialPorCuenta_CatalogoFalla_Fallback() {
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC1").numeroCuenta("1111").idServicio(99L).idProducto(88L).estatus("EXITOSA").build();
        when(transaccionRepository.findByNumeroCuentaOrderByFechaCreacionDesc("1111")).thenReturn(List.of(tx));
        when(productoService.obtenerProductosCategorizados()).thenThrow(new RuntimeException("Catalogo no responde"));

        List<GestoPagoTransaccion> result = pagoServicioService.obtenerHistorialPorCuenta("1111");
        assertEquals(1, result.size());
        assertEquals("Servicio #99", result.get(0).getServicio());
        assertEquals("Producto #88", result.get(0).getProducto());
    }

    @Test
    @DisplayName("HistorialPorCuenta: coincide solo por servicio en catalogo")
    void testObtenerHistorialPorCuenta_CoincideSoloPorServicio() {
        configurarCatalogo(2, new BigDecimal("10.00"));
        GestoPagoTransaccion tx = GestoPagoTransaccion.builder()
                .upc("UPC2").numeroCuenta("1111").idServicio(76L).idProducto(9999L).estatus("EXITOSA").build();
        when(transaccionRepository.findByNumeroCuentaOrderByFechaCreacionDesc("1111")).thenReturn(List.of(tx));

        List<GestoPagoTransaccion> result = pagoServicioService.obtenerHistorialPorCuenta("1111");
        assertEquals(1, result.size());
        assertEquals("CFE", result.get(0).getServicio());
    }

    @Test
    @DisplayName("ValidarServicioEnCatalogo: catalogo categorizado vacio consulta listado directo")
    void testValidarServicio_CatalogoCategoriasVacio_UsaListaDirecta() {
        ProductCategorizedResponse catVacia = ProductCategorizedResponse.builder().categorias(Map.of()).build();
        when(productoService.obtenerProductosCategorizados()).thenReturn(catVacia);

        ProductItemDto prod = ProductItemDto.builder().idServicio(76L).idProducto(205L).servicio("CFE").producto("Pago CFE").tipoFront(2).build();
        ProductListResponse directo = ProductListResponse.builder().productos(List.of(prod)).build();
        when(productoService.obtenerListaProductos()).thenReturn(directo);
        configurarClienteYCuentaActivos();

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16).idServicio(76L).idProducto(205L).referencia("REF1234").build();
        assertTrue(pagoServicioService.validarReferencia(req).isValida());
    }

    @Test
    @DisplayName("ValidarServicioEnCatalogo: ambos catalogos vacios lanza RecursoNoEncontradoException")
    void testValidarServicio_CatalogoTotalmenteVacio_LanzaRecursoNoEncontrado() {
        when(productoService.obtenerProductosCategorizados()).thenReturn(null);
        when(productoService.obtenerListaProductos()).thenReturn(null);

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16).idServicio(76L).idProducto(205L).referencia("REF1234").build();
        assertThrows(RecursoNoEncontradoException.class, () -> pagoServicioService.validarReferencia(req));
    }

    @Test
    @DisplayName("ValidarServicioEnCatalogo: servicio o producto no registrado lanza excepcion")
    void testValidarServicio_ServicioOProductoNoExiste_LanzaRecursoNoEncontrado() {
        configurarCatalogo(2, BigDecimal.ZERO);

        ValidarReferenciaRequest reqServicio = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16).idServicio(9999L).idProducto(205L).referencia("REF1234").build();
        assertThrows(RecursoNoEncontradoException.class, () -> pagoServicioService.validarReferencia(reqServicio));

        ValidarReferenciaRequest reqProducto = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16).idServicio(76L).idProducto(9999L).referencia("REF1234").build();
        assertThrows(RecursoNoEncontradoException.class, () -> pagoServicioService.validarReferencia(reqProducto));
    }

    @Test
    @DisplayName("CerrarComoExitosa: ramas de mensaje nulo, legend nula, email fallback cliente y texto nulo")
    void testCerrarComoExitosa_RamasNulasYFallback() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("CFE")
                .idProducto(205L)
                .producto("Pago CFE")
                .tipoFront(2)
                .legend("Legend Catalogo")
                .urlLogo("https://img.com/cfe.png")
                .build();
        ProductCategorizedResponse catResp = ProductCategorizedResponse.builder()
                .status(200)
                .totalProductos(1)
                .categorias(Map.of(2, List.of(prod)))
                .build();
        when(productoService.obtenerProductosCategorizados()).thenReturn(catResp);

        Cuenta cuenta = Cuenta.builder().idCuenta(1L).numeroCuenta(NUM_CUENTA_16).estatus("ACTIVA").build();
        Saldo saldo = Saldo.builder().idSaldo(1L).cuenta(cuenta).saldoDisponible(1000.0).saldoContable(1000.0).build();
        cuenta.setSaldo(saldo);
        Cliente cliente = Cliente.builder().idCliente(10L).nombre(null).apellidoPaterno(null).correoElectronico("cliente@mail.com").activo(true).build();
        UsuarioLogin usuarioLogin = UsuarioLogin.builder().idUsuario(100L).cliente(cliente).biometricoFacialEmbedding(EMBEDDING_VALIDO).activo(true).build();

        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cuenta));
        when(clienteRepository.findByCuentasNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cliente));
        when(usuarioLoginRepository.findByClienteIdCliente(10L)).thenReturn(Optional.of(usuarioLogin));

        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));

        SendTxXmlResponse xml = SendTxXmlResponse.builder()
                .idTx("12345")
                .numAutorizacion("AUT999")
                .comision(null)
                .fecha("23/Oct/2026")
                .mensaje(null)
                .build();
        RespuestaSendTx respSend = new RespuestaSendTx(Veredicto.EXITO, "01", null, "<XML/>", xml, null);
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respSend);
        when(ledger.confirmarExito(any(), any(), any())).thenReturn(new SaldosFinales(new BigDecimal("850.00"), new BigDecimal("850.00")));

        PagoServicioRequest req = requestValido();
        req.setCorreoElectronico(null);

        PagoServicioResponse resp = pagoServicioService.procesarPago(req, "clave-idemp-001");
        assertNotNull(resp);
        assertEquals("Legend Catalogo", resp.getLegend());
        assertEquals(new BigDecimal("0.00"), resp.getComision());
    }

    @Test
    @DisplayName("ValidarMonto: rechaza monto <= 0 o fuera de limites abiertos")
    void testValidarMontoContraCatalogo_Ramas() {
        configurarCatalogo(2, BigDecimal.ZERO);
        configurarClienteYCuentaActivos();

        PagoServicioRequest reqCero = requestValido();
        reqCero.setMonto(BigDecimal.ZERO);
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(reqCero, "clave-idemp-001"));

        PagoServicioRequest reqBajo = requestValido();
        reqBajo.setMonto(new BigDecimal("0.50"));
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(reqBajo, "clave-idemp-001"));

        PagoServicioRequest reqAlto = requestValido();
        reqAlto.setMonto(new BigDecimal("15000.00"));
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(reqAlto, "clave-idemp-001"));
    }

    @Test
    @DisplayName("ValidarMonto: tipoFront=1 precio fijo rechaza si monto en request no coincide exactamente")
    void testValidarMonto_TipoFront1_DiscrepanciaPrecio() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("Telcel")
                .idProducto(205L)
                .producto("Recarga 100")
                .tipoFront(1)
                .idCatTipoServicio(1)
                .precio(new BigDecimal("100.00"))
                .build();
        ProductCategorizedResponse catResp = ProductCategorizedResponse.builder()
                .status(200)
                .totalProductos(1)
                .categorias(Map.of(1, List.of(prod)))
                .build();
        when(productoService.obtenerProductosCategorizados()).thenReturn(catResp);
        configurarClienteYCuentaActivos();

        PagoServicioRequest req = requestValido();
        req.setMonto(new BigDecimal("200.00"));
        assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(req, "clave-idemp-001"));
    }

    @Test
    @DisplayName("ValidarReferencia: servicio PIN o entretenimiento normaliza telefono y referencia segun disponibilidad")
    void testValidarReferencia_PinOEntretenimiento_Ramas() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("Xbox Live")
                .idProducto(205L)
                .producto("Membresia 1 mes")
                .tipoFront(1)
                .idCatTipoServicio(10) // PIN
                .precio(new BigDecimal("150.00"))
                .showAyuda(true)
                .build();

        when(productoService.obtenerProductosCategorizados()).thenReturn(ProductCategorizedResponse.builder()
                .status(200).totalProductos(1).categorias(Map.of(1, List.of(prod))).build());
        configurarClienteYCuentaActivos();
        when(logoServicioResolver.resolverUrlReferenciaPuntoRed(76L)).thenReturn("https://ayuda.red/xbox");

        // Caso 1: telefono presente y referencia presente
        ValidarReferenciaRequest req1 = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16).idServicio(76L).idProducto(205L)
                .telefono("5512345678").referencia("PIN-XBOX-999").build();
        ValidarReferenciaResponse resp1 = pagoServicioService.validarReferencia(req1);
        assertEquals("5512345678", resp1.getTelefonoNormalizado());
        assertEquals("PIN-XBOX-999", resp1.getReferenciaNormalizada());
        assertEquals("https://ayuda.red/xbox", resp1.getUrlReferenciaAyuda());

        // Caso 2: telefono vacio y referencia vacia -> usa valores por defecto
        ValidarReferenciaRequest req2 = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16).idServicio(76L).idProducto(205L)
                .telefono("").referencia("").build();
        ValidarReferenciaResponse resp2 = pagoServicioService.validarReferencia(req2);
        assertEquals("#1111111111", resp2.getTelefonoNormalizado());
        assertEquals("#1111111111", resp2.getReferenciaNormalizada());
    }

    @Test
    @DisplayName("ValidarReferencia: telefonia con prefijo # y referencia no vacia")
    void testValidarReferencia_TelefoniaPrefijo_Y_Referencia() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("Telcel")
                .idProducto(205L)
                .producto("Paquete 100")
                .tipoFront(1)
                .idCatTipoServicio(1) // Telefonia
                .precio(new BigDecimal("100.00"))
                .build();

        when(productoService.obtenerProductosCategorizados()).thenReturn(ProductCategorizedResponse.builder()
                .status(200).totalProductos(1).categorias(Map.of(1, List.of(prod))).build());
        configurarClienteYCuentaActivos();

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16).idServicio(76L).idProducto(205L)
                .telefono("#5512345678").referencia("REF-TEL-1").build();
        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);
        assertEquals("5512345678", resp.getTelefonoNormalizado());
        assertEquals("REF-TEL-1", resp.getReferenciaNormalizada());
    }

    @Test
    @DisplayName("ValidarReferencia: servicio no tipoFront1 con telefono normaliza correctamente")
    void testValidarReferencia_ServicioNoTipoFront1_ConTelefono() {
        configurarCatalogo(2, new BigDecimal("10.00")); // comision estimada
        configurarClienteYCuentaActivos();

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16).idServicio(76L).idProducto(205L)
                .telefono("5512345678").referencia("CFE9876543210").build();
        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);
        assertEquals("5512345678", resp.getTelefonoNormalizado());
        assertEquals("CFE9876543210", resp.getReferenciaNormalizada());
        assertEquals(new BigDecimal("10.00"), resp.getComisionEstimada());
    }

    @Test
    @DisplayName("ValidarReferencia: cuenta sin saldo o saldo disponible nulo maneja saldo en cero")
    void testValidarReferencia_SaldoNulo() {
        configurarCatalogo(2, BigDecimal.ZERO);
        Cuenta cuenta = Cuenta.builder().idCuenta(1L).numeroCuenta(NUM_CUENTA_16).estatus("ACTIVA").build();
        Cliente cliente = Cliente.builder().idCliente(10L).curp("PELJ920520HDFRRN09").activo(true).build();
        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cuenta));
        when(clienteRepository.findByCuentasNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cliente));

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16).idServicio(76L).idProducto(205L)
                .referencia("CFE123456").monto(new BigDecimal("100.00")).build();
        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);
        assertEquals(new BigDecimal("0.00"), resp.getSaldoDisponibleActual());
        assertFalse(resp.getSaldoSuficiente());
    }

    @Test
    @DisplayName("ValidarReferencia: producto con precio nulo y tipoFront nulo")
    void testValidarReferencia_ProductoPrecioNulo_Y_TipoFrontNulo() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("Servicio Especial")
                .idProducto(205L)
                .producto("Generico")
                .tipoFront(null)
                .precio(null)
                .build();

        when(productoService.obtenerProductosCategorizados()).thenReturn(ProductCategorizedResponse.builder()
                .status(200).totalProductos(1).categorias(Map.of(0, List.of(prod))).build());
        configurarClienteYCuentaActivos();

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16).idServicio(76L).idProducto(205L)
                .referencia("REFGENERICA").build();
        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);
        assertEquals(new BigDecimal("0.00"), resp.getPrecioCatalogo());
        assertEquals(new BigDecimal("0.00"), resp.getComisionEstimada());
        assertNull(resp.getMontoValidado());
        assertTrue(resp.getSaldoSuficiente());
    }

    @Test
    @DisplayName("ValidarReferencia: sin monto en request para producto precio fijo asigna precioCatalogo")
    void testValidarReferencia_SinMontoEnRequest_ConPrecioFijo() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("Spotify")
                .idProducto(205L)
                .producto("Premium 1 mes")
                .tipoFront(1)
                .idCatTipoServicio(11) // Entretenimiento
                .precio(new BigDecimal("129.00"))
                .showAyuda(false)
                .build();

        when(productoService.obtenerProductosCategorizados()).thenReturn(ProductCategorizedResponse.builder()
                .status(200).totalProductos(1).categorias(Map.of(1, List.of(prod))).build());
        configurarClienteYCuentaActivos();

        ValidarReferenciaRequest req = ValidarReferenciaRequest.builder()
                .numCuenta(NUM_CUENTA_16).idServicio(76L).idProducto(205L)
                .referencia("SPOTIFY-REF-1").build();
        ValidarReferenciaResponse resp = pagoServicioService.validarReferencia(req);
        assertEquals(new BigDecimal("129.00"), resp.getMontoValidado());
        assertNull(resp.getUrlReferenciaAyuda());
    }

    @Test
    @DisplayName("ProcesarPago: cerrarComoExitosa ramas con mensaje nulo, cliente sin email ni nombres, comision no numerica y logo existente")
    void testProcesarPago_CerrarComoExitosa_RamasBorde() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("CFE")
                .idProducto(205L)
                .producto("Luz")
                .tipoFront(2)
                .precio(BigDecimal.ZERO)
                .urlLogo("https://cdn.test/logo-cfe.png")
                .legend("Pagar antes del vencimiento")
                .build();

        when(productoService.obtenerProductosCategorizados()).thenReturn(ProductCategorizedResponse.builder()
                .status(200).totalProductos(1).categorias(Map.of(2, List.of(prod))).build());

        Cuenta cuenta = Cuenta.builder().idCuenta(1L).numeroCuenta(NUM_CUENTA_16).estatus("ACTIVA").build();
        Saldo saldo = Saldo.builder().idSaldo(1L).cuenta(cuenta).saldoDisponible(1000.0).saldoContable(1000.0).build();
        cuenta.setSaldo(saldo);

        // Cliente sin nombre, sin apellido, sin email
        Cliente cliente = Cliente.builder().idCliente(10L).curp("PELJ920520HDFRRN09").activo(true).build();
        UsuarioLogin usuarioLogin = UsuarioLogin.builder()
                .idUsuario(100L)
                .cliente(cliente)
                .biometricoFacialEmbedding(EMBEDDING_VALIDO)
                .activo(true)
                .build();

        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cuenta));
        when(clienteRepository.findByCuentasNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cliente));
        when(usuarioLoginRepository.findByClienteIdCliente(10L)).thenReturn(Optional.of(usuarioLogin));
        when(ledger.reservar(any())).thenReturn(new Reserva(new BigDecimal("1000.00"), new BigDecimal("850.00")));

        SendTxXmlResponse xml = SendTxXmlResponse.builder()
                .idTx(null) // idTx nulo en XML
                .numAutorizacion("AUT999")
                .comision("comision-invalida") // Detona parsearDecimalSeguro NumberFormatException
                .fecha("23/Oct/2026")
                .mensaje(null) // Mensaje nulo
                .build();
        RespuestaSendTx respuesta = new RespuestaSendTx(Veredicto.EXITO, "01", null, "<XML/>", xml, null);
        when(sendTxGateway.ejecutar(anyString(), anyString())).thenReturn(respuesta);

        PagoServicioRequest req = requestValido();
        req.setCorreoElectronico(null); // Correo request nulo y correo cliente nulo

        PagoServicioResponse resp = pagoServicioService.procesarPago(req, "clave-idemp-001");
        assertNotNull(resp);
        assertEquals(new BigDecimal("0.00"), resp.getComision());
        assertNull(resp.getIdTx());
        assertNull(resp.getPin());
        assertEquals("Pagar antes del vencimiento", resp.getLegend());
        assertEquals("https://cdn.test/logo-cfe.png", resp.getUrlLogo());
        assertEquals("Sin mensaje del proveedor", resp.getDetalleProveedor());
        assertEquals("Pago Exitoso", resp.getMensaje());
    }

    @Test
    @DisplayName("ValidarAutenticacion2FA: sesion activa con token distinto continua iterando hasta fallar")
    void testValidarAutenticacion2FA_SesionConTokenDiferente_NoCoincide() {
        configurarCatalogo(BigDecimal.ZERO);
        Cuenta cuenta = Cuenta.builder().idCuenta(1L).numeroCuenta(NUM_CUENTA_16).estatus("ACTIVA").build();
        Cliente cliente = Cliente.builder().idCliente(10L).curp("PELJ920520HDFRRN09").activo(true).build();
        UsuarioLogin usuarioLogin = UsuarioLogin.builder()
                .idUsuario(100L)
                .cliente(cliente)
                .passwordHash(null)
                .biometricoFacialEmbedding(null)
                .activo(true)
                .build();

        when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cuenta));
        when(clienteRepository.findByCuentasNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cliente));
        when(usuarioLoginRepository.findByClienteIdCliente(10L)).thenReturn(Optional.of(usuarioLogin));

        SesionLogin sesionDistinta = SesionLogin.builder()
                .tokenSesion("token-totalmente-distinto")
                .activa(true)
                .build();
        when(sesionLoginRepository.findByUsuarioIdUsuarioAndActivaTrue(100L)).thenReturn(List.of(sesionDistinta));

        PagoServicioRequest req = requestValido();
        req.setCodigo2fa("token-peticion-no-coincidente");

        AutenticacionException ex = assertThrows(AutenticacionException.class,
                () -> pagoServicioService.procesarPago(req, "clave-idemp-001"));
        assertEquals(CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA, ex.getCatalogoError());
    }

    // ------------------------------------------------------------------ helpers

    private PagoServicioRequest requestValido() {
        return PagoServicioRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .monto(new BigDecimal("150.00"))
                .telefono("5512345678")
                .referencia("25720381")
                .codigo2fa(EMBEDDING_VALIDO)
                .build();
    }

    private RespuestaSendTx respuestaExito() {
        SendTxXmlResponse xml = SendTxXmlResponse.builder()
                .idTx("291620941")
                .numAutorizacion("100074211")
                .comision("6.0")
                .fecha("23/Oct/2026 18:55:00")
                .mensaje(new MensajeTxXml("01", "Operacion realizada con exito", "25720381", null, null, null))
                .build();
        return new RespuestaSendTx(Veredicto.EXITO, "01", "Operacion realizada con exito", "<RESPONSE/>", xml, null);
    }

    private RespuestaSendTx respuesta(Veredicto veredicto, String codigo, String texto, CatalogoCodigoError errorInterno) {
        SendTxXmlResponse xml = codigo == null ? null : SendTxXmlResponse.builder()
                .mensaje(new MensajeTxXml(codigo, texto, null, null, null, null)).build();
        return new RespuestaSendTx(veredicto, codigo, texto, null, xml, errorInterno);
    }

    private void configurarCatalogo(BigDecimal precio) {
        configurarCatalogo(2, precio);
    }

    private void configurarCatalogo(int tipoFront, BigDecimal precio) {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("CFE")
                .idProducto(205L)
                .producto("Pago CFE")
                .tipoFront(tipoFront)
                .precio(precio)
                .build();

        ProductCategorizedResponse catResp = ProductCategorizedResponse.builder()
                .status(200)
                .totalProductos(1)
                .categorias(Map.of(tipoFront, List.of(prod)))
                .build();

        when(productoService.obtenerProductosCategorizados()).thenReturn(catResp);
    }

    private void configurarClienteYCuentaActivos() {
        configurarClienteYCuentaActivosConSaldo(1000.00);
    }

    private void configurarClienteYCuentaActivosConSaldo(double saldoDisp) {
        Cuenta cuenta = Cuenta.builder().idCuenta(1L).numeroCuenta(NUM_CUENTA_16).estatus("ACTIVA").build();
        Saldo saldo = Saldo.builder().idSaldo(1L).cuenta(cuenta).saldoDisponible(saldoDisp).saldoContable(saldoDisp).build();
        cuenta.setSaldo(saldo);
        Cliente cliente = Cliente.builder().idCliente(10L).curp("PELJ920520HDFRRN09").activo(true).build();
        UsuarioLogin usuarioLogin = UsuarioLogin.builder()
                .idUsuario(100L)
                .cliente(cliente)
                .biometricoFacialEmbedding(EMBEDDING_VALIDO)
                .activo(true)
                .build();

        lenient().when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cuenta));
        lenient().when(clienteRepository.findByCuentasNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cliente));
        lenient().when(usuarioLoginRepository.findByClienteIdCliente(10L)).thenReturn(Optional.of(usuarioLogin));
    }
}
