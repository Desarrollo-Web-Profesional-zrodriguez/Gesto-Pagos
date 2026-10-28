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
import com.proyecto.servicios.entity.cliente.UsuarioLogin;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.exception.AutenticacionException;
import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.SolicitudDuplicadaException;
import com.proyecto.servicios.model.email.ComprobanteEmailResult;
import com.proyecto.servicios.model.gestopago.catalogo.ProductCategorizedResponse;
import com.proyecto.servicios.model.gestopago.catalogo.ProductItemDto;
import com.proyecto.servicios.model.gestopago.transaccion.PagoEstatusResponse;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioResponse;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx.Veredicto;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse.MensajeTxXml;
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
        Cuenta cuenta = Cuenta.builder().idCuenta(1L).numeroCuenta(NUM_CUENTA_16).estatus("ACTIVA").build();
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
