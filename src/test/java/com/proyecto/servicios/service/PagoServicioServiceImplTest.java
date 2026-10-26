package com.proyecto.servicios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Saldo;
import com.proyecto.servicios.entity.cliente.UsuarioLogin;
import com.proyecto.servicios.exception.AutenticacionException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.gestopago.catalogo.ProductCategorizedResponse;
import com.proyecto.servicios.model.gestopago.catalogo.ProductItemDto;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.SaldoRepository;
import com.proyecto.servicios.repositorys.cliente.SesionLoginRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioLoginRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.service.Impl.PagoServicioServiceImpl;
import com.proyecto.servicios.util.AesEncryption;

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
    private SaldoRepository saldoRepository;
    @Mock
    private UsuarioLoginRepository usuarioLoginRepository;
    @Mock
    private SesionLoginRepository sesionLoginRepository;
    @Mock
    private ProductoService productoService;
    @Mock
    private GestoPagoTokenService tokenService;
    @Mock
    private GestoPagoProductClient gestoPagoClient;
    @Mock
    private GestoPagoTransaccionRepository transaccionRepository;

    private PagoServicioServiceImpl pagoServicioService;

    @BeforeEach
    void setUp() {
        pagoServicioService = new PagoServicioServiceImpl(
                cuentaRepository,
                clienteRepository,
                saldoRepository,
                usuarioLoginRepository,
                sesionLoginRepository,
                productoService,
                tokenService,
                gestoPagoClient,
                transaccionRepository,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(pagoServicioService, "aesSecretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(pagoServicioService, "aesIv", TEST_IV);
        ReflectionTestUtils.setField(pagoServicioService, "unidadNegocio", "083");

        lenient().when(tokenService.obtenerTokenValido()).thenReturn("token_jwt_test");
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

    @Test
    @DisplayName("Debe procesar el pago exitosamente (CODIGO 01), cifrar peticion y descontar saldo disponible y contable")
    void testProcesarPago_Exitoso_Codigo01() {
        configurarCatalogoMock();
        configurarClienteYCuentaActivos(1000.00);

        String xmlExito = """
            <?xml version='1.0' encoding='UTF-8'?>
            <RESPONSE>
                <ID_TX>291620941</ID_TX>
                <NUM_AUTORIZACION>100074211</NUM_AUTORIZACION>
                <SALDO>154997.91</SALDO>
                <COMISION>6.0</COMISION>
                <SALDO_F>154,997.91</SALDO_F>
                <COMISION_F>6</COMISION_F>
                <FECHA>23/Oct/2026 18:55:00</FECHA>
                <MONTO>150.00</MONTO>
                <MENSAJE>
                    <CODIGO>01</CODIGO>
                    <TEXTO>Operacion realizada con exito</TEXTO>
                    <REFERENCIA>25720381</REFERENCIA>
                </MENSAJE>
            </RESPONSE>
            """;

        when(gestoPagoClient.sendTx(eq("Bearer token_jwt_test"), any())).thenReturn(xmlExito);

        PagoServicioRequest request = PagoServicioRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .monto(new BigDecimal("150.00"))
                .telefono("5512345678")
                .referencia("25720381")
                .codigo2fa(EMBEDDING_VALIDO)
                .build();

        PagoServicioResponse response = pagoServicioService.procesarPago(request);

        assertNotNull(response);
        assertEquals("01", response.getCodigo());
        assertEquals("Pago Exitoso", response.getMensaje());
        assertEquals("291620941", response.getIdTx());
        assertEquals("100074211", response.getNumAutorizacion());
        assertEquals(10, response.getUpc().length());
        assertEquals(new BigDecimal("1000.00"), response.getSaldoAnterior());
        assertEquals(new BigDecimal("850.00"), response.getSaldoDisponibleActual());
        assertEquals(new BigDecimal("850.00"), response.getSaldoContableActual());

        // Se guarda 2 veces el saldo: Fase 1 (retención en disponible) y Fase 2 (confirmación en contable)
        verify(saldoRepository, times(2)).save(any(Saldo.class));
        verify(transaccionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Debe revertir la retencion del saldo cuando GestoPago devuelve CODIGO distinto de 01 (ej. 06 Duplicado)")
    void testProcesarPago_RechazadoPorGestoPago_RevierteSaldo() {
        configurarCatalogoMock();
        Saldo saldo = configurarClienteYCuentaActivos(500.00);

        String xmlError = """
            <?xml version='1.0' encoding='UTF-8'?>
            <RESPONSE>
                <ID_TX>291620941</ID_TX>
                <NUM_AUTORIZACION>-1</NUM_AUTORIZACION>
                <MENSAJE>
                    <CODIGO>06</CODIGO>
                    <TEXTO>Transaccion aplicada anteriormente</TEXTO>
                </MENSAJE>
            </RESPONSE>
            """;

        when(gestoPagoClient.sendTx(eq("Bearer token_jwt_test"), any())).thenReturn(xmlError);

        PagoServicioRequest request = PagoServicioRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .monto(new BigDecimal("150.00"))
                .telefono("5512345678")
                .referencia("25720381")
                .codigo2fa(EMBEDDING_VALIDO)
                .build();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> pagoServicioService.procesarPago(request));
        assertEquals(6, ex.getCodigoPersonalizado());
        assertEquals("06", ex.getCodigoGestoPago());

        // Verifica que el saldo disponible fue restaurado a 500.00 tras el reverso automático
        assertEquals(500.00, saldo.getSaldoDisponible());
        assertEquals(500.00, saldo.getSaldoContable());
    }

    @Test
    @DisplayName("Debe rechazar el pago cuando el servicio o producto no existe en /productos")
    void testProcesarPago_ServicioNoExisteEnCatalogo() {
        configurarCatalogoMock();

        PagoServicioRequest request = PagoServicioRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(9999L)
                .idProducto(205L)
                .monto(new BigDecimal("150.00"))
                .telefono("5512345678")
                .referencia("25720381")
                .codigo2fa(EMBEDDING_VALIDO)
                .build();

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class, () -> pagoServicioService.procesarPago(request));
        assertEquals(101, ex.getCatalogoError().getCodigo());
    }

    @Test
    @DisplayName("Debe rechazar el pago cuando el 2FA no coincide con el cliente")
    void testProcesarPago_2FAInvalido() {
        configurarCatalogoMock();
        configurarClienteYCuentaActivos(1000.00);

        PagoServicioRequest request = PagoServicioRequest.builder()
                .numCuenta(NUM_CUENTA_16)
                .idServicio(76L)
                .idProducto(205L)
                .monto(new BigDecimal("150.00"))
                .telefono("5512345678")
                .referencia("25720381")
                .codigo2fa("[-0.99, 0.99, -0.50, 0.40, -0.80]")
                .build();

        AutenticacionException ex = assertThrows(AutenticacionException.class, () -> pagoServicioService.procesarPago(request));
        assertEquals(401, ex.getCatalogoError().getCodigo());
    }

    private void configurarCatalogoMock() {
        ProductItemDto prod = ProductItemDto.builder()
                .idServicio(76L)
                .servicio("CFE")
                .idProducto(205L)
                .producto("Pago CFE")
                .tipoFront(2)
                .precio(new BigDecimal("10.00"))
                .build();

        ProductCategorizedResponse catResp = ProductCategorizedResponse.builder()
                .status(200)
                .totalProductos(1)
                .categorias(Map.of(2, List.of(prod)))
                .build();

        when(productoService.obtenerProductosCategorizados()).thenReturn(catResp);
    }

    private Saldo configurarClienteYCuentaActivos(double montoSaldo) {
        Cuenta cuenta = Cuenta.builder()
                .idCuenta(1L)
                .numeroCuenta(NUM_CUENTA_16)
                .estatus("ACTIVA")
                .build();

        Cliente cliente = Cliente.builder()
                .idCliente(10L)
                .curp("PELJ920520HDFRRN09")
                .activo(true)
                .build();

        UsuarioLogin usuarioLogin = UsuarioLogin.builder()
                .idUsuario(100L)
                .cliente(cliente)
                .biometricoFacialEmbedding(EMBEDDING_VALIDO)
                .activo(true)
                .build();

        Saldo saldo = Saldo.builder()
                .idSaldo(1L)
                .cuenta(cuenta)
                .saldoDisponible(montoSaldo)
                .saldoContable(montoSaldo)
                .build();

        lenient().when(cuentaRepository.findByNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cuenta));
        lenient().when(clienteRepository.findByCuentasNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(cliente));
        lenient().when(usuarioLoginRepository.findByClienteIdCliente(10L)).thenReturn(Optional.of(usuarioLogin));
        lenient().when(saldoRepository.findByCuentaNumeroCuenta(NUM_CUENTA_16)).thenReturn(Optional.of(saldo));

        return saldo;
    }
}
