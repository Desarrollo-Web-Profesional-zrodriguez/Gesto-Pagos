package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.gestopago.catalogo.ProductCategorizedResponse;
import com.proyecto.servicios.model.gestopago.catalogo.ProductItemDto;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para la clase {@link ProductoServiceImpl}.
 * 
 * Se utiliza Mockito para aislar las pruebas de llamadas de red reales o servidores externos,
 * garantizando la cobertura de los flujos exitosos y de manejo de excepciones.
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    /**
     * Mock del cliente Feign que se comunica con el API externo de GestoPago.
     */
    @Mock
    private GestoPagoProductClient productClient;

    /**
     * Mock del servicio de gestión de tokens dinámicos de GestoPago.
     */
    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private GestoPagoProductoRepository productoRepository;

    @Mock
    private GestoPagoProductoMapper productoMapper;

    /**
     * Instancia del servicio a probar en la que Mockito inyecta automáticamente los mocks declarados.
     */
    @InjectMocks
    private ProductoServiceImpl productoService;

    private static final String DUMMY_TOKEN = "mi_token_secreto_123";
    private static final String REDIS_KEY = "gestopago:catalogo:productos_por_tipo_front";

    /**
     * Configuración previa a la ejecución de cada prueba.
     * Simula la provisión del token dinámico válido y la clave de Redis.
     */
    @BeforeEach
    void setUp() {
        lenient().when(tokenService.obtenerTokenValido()).thenReturn(DUMMY_TOKEN);
        ReflectionTestUtils.setField(productoService, "redisKey", REDIS_KEY);
    }

    /**
     * Prueba el flujo exitoso:
     * 1. El cliente externo responde con un XML válido (status 200).
     * 2. JAXB deserializa correctamente los atributos y etiquetas en los objetos DTO.
     * 3. El servicio devuelve un {@link ProductListResponse} con status 200 y la lista de productos.
     */
    @Test
    @DisplayName("Debe retornar lista de productos exitosamente cuando el cliente externo responde XML 200 OK")
    void testObtenerListaProductos_Exitoso() {
        // Given (Preparación): XML de ejemplo que simula la respuesta de GestoPago
        String xmlMock = """
            <?xml version="1.0" encoding="UTF-8"?>
            <RESPONSE>
                <MENSAJE>
                    <CODIGO>01</CODIGO>
                    <TEXTO>Operacion realizada con exito</TEXTO>
                </MENSAJE>
                <PRODUCTOS>
                    <producto servicio="ABIB" producto="ABIB 100" idServicio="2284" idProducto="14302" idCatTipoServicio="13" tipoFront="1" hasDigitoVerificador="false" precio="100.0" showAyuda="false" tipoReferencia="a">
                        <legend><![CDATA[Recibe soporte las 24h marcando al *787]]></legend>
                    </producto>
                </PRODUCTOS>
            </RESPONSE>
            """;

        when(productClient.getProductList("Bearer " + DUMMY_TOKEN)).thenReturn(xmlMock);

        // When (Ejecución): Llamamos al método real a probar
        ProductListResponse result = productoService.obtenerListaProductos();

        // Then (Validaciones):
        assertNotNull(result, "La respuesta no debe ser nula");
        assertEquals(200, result.getStatus(), "El código de estado debe ser 200");
        assertEquals("Operacion realizada con exito", result.getMessage(), "El mensaje debe coincidir");
        assertNotNull(result.getProductos(), "La lista de productos no debe ser nula");
        assertEquals(1, result.getProductos().size(), "Debe contener exactamente 1 producto");

        // Validamos que JAXB haya mapeado los atributos XML al DTO
        var producto = result.getProductos().get(0);
        assertEquals(14302L, producto.getIdProducto());
        assertEquals("ABIB 100", producto.getProducto());
        assertEquals("ABIB", producto.getServicio());
        assertEquals(new BigDecimal("100.0"), producto.getPrecio());
        assertEquals("Recibe soporte las 24h marcando al *787", producto.getLegend());

        // Verificamos que se llamó al cliente con el header Bearer correcto
        verify(productClient).getProductList("Bearer " + DUMMY_TOKEN);
    }

    /**
     * Prueba el flujo de validación del token de autenticación:
     * Si el servicio de tokens no retorna un token disponible o este es nulo/vacío,
     * el servicio debe retornar status 401 inmediatamente sin llamar al cliente externo.
     */
    @Test
    @DisplayName("Debe retornar status 401 cuando el token dinámico no está disponible")
    void testObtenerListaProductos_TokenNoDisponible() {
        // Given: El servicio de tokens no retorna un token disponible
        when(tokenService.obtenerTokenValido()).thenReturn(null);

        // When
        ProductListResponse result = productoService.obtenerListaProductos();

        // Then
        assertNotNull(result);
        assertEquals(401, result.getStatus(), "Debe responder con 401 si falta el token");
        assertEquals("Token de autenticacion no disponible", result.getMessage());
        assertTrue(result.getProductos().isEmpty(), "La lista de productos debe estar vacía");
    }

    /**
     * Prueba la recuperación automática ante expiración del token (HTTP 403 / 401):
     * Simula que la primera llamada falla con 403 Forbidden (token expirado),
     * el servicio fuerza la renovación del token y reintenta con éxito.
     */
    @Test
    @DisplayName("Debe renovar token y reintentar exitosamente cuando el servicio responde 403 por token expirado")
    void testObtenerListaProductos_TokenExpirado403_RenuevaYReintentaExitoso() {
        String tokenNuevo = "nuevo_token_dinamico_renovado_789";
        String xmlMock = """
            <?xml version="1.0" encoding="UTF-8"?>
            <RESPONSE>
                <MENSAJE>
                    <CODIGO>01</CODIGO>
                    <TEXTO>Operacion realizada con exito</TEXTO>
                </MENSAJE>
                <PRODUCTOS>
                    <producto servicio="ABIB" producto="ABIB 100" idServicio="2284" idProducto="14302" idCatTipoServicio="13" tipoFront="1" hasDigitoVerificador="false" precio="100.0" showAyuda="false" tipoReferencia="a">
                        <legend><![CDATA[Recibe soporte las 24h marcando al *787]]></legend>
                    </producto>
                </PRODUCTOS>
            </RESPONSE>
            """;

        Request mockRequest = Request.create(
                Request.HttpMethod.GET,
                "/sistema/service/getProductList.do",
                new HashMap<>(),
                Request.Body.empty(),
                new RequestTemplate()
        );

        FeignException forbiddenException = new FeignException.Forbidden(
                "Forbidden",
                mockRequest,
                null,
                new HashMap<>()
        );

        // Primer llamada con token viejo falla con 403, tras renovar retorna tokenNuevo y la segunda llamada es exitosa
        when(productClient.getProductList("Bearer " + DUMMY_TOKEN)).thenThrow(forbiddenException);
        when(tokenService.obtenerTokenValido()).thenReturn(DUMMY_TOKEN, tokenNuevo);
        when(productClient.getProductList("Bearer " + tokenNuevo)).thenReturn(xmlMock);

        // When
        ProductListResponse result = productoService.obtenerListaProductos();

        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertEquals("Operacion realizada con exito", result.getMessage());
        assertEquals(1, result.getProductos().size());

        verify(tokenService).renovarToken();
        verify(productClient).getProductList("Bearer " + tokenNuevo);
    }

    /**
     * Prueba el manejo de excepción por tiempo de espera (Timeout):
     * Simula que Feign lanza {@link RetryableException} al agotarse el tiempo de conexión,
     * verificando que el servicio lo capture y devuelva status 504.
     */
    @Test
    @DisplayName("Debe manejar RetryableException (Timeout) y retornar respuesta de error controlada")
    void testObtenerListaProductos_Timeout() {
        // Given: Construimos un objeto Request simulado, requerido por el constructor de RetryableException
        Request mockRequest = Request.create(
                Request.HttpMethod.GET,
                "/sistema/service/getProductList.do",
                new HashMap<>(),
                Request.Body.empty(),
                new RequestTemplate()
        );

        // Instanciamos la excepción simulando que la petición falló por timeout
        RetryableException timeoutException = new RetryableException(
                504,
                "Read timed out",
                Request.HttpMethod.GET,
                new Date(),
                mockRequest
        );

        // Simulamos que al llamar al cliente se dispara la excepción de timeout
        when(productClient.getProductList(anyString())).thenThrow(timeoutException);

        // When
        ProductListResponse result = productoService.obtenerListaProductos();

        // Then
        assertNotNull(result);
        assertEquals(504, result.getStatus(), "El status debe ser 504 ante un timeout");
        assertEquals("Tiempo de espera agotado al conectar con el servicio externo", result.getMessage());
        assertTrue(result.getProductos().isEmpty(), "No debe retornar productos");
    }

    /**
     * Prueba el manejo de error de autenticación con el proveedor (401 Unauthorized):
     * Simula que el servidor externo rechazó las credenciales o el token expiró.
     */
    @Test
    @DisplayName("Debe manejar FeignException.Unauthorized (401) cuando el token es rechazado")
    void testObtenerListaProductos_Unauthorized() {
        // Given: Petición HTTP simulada requerida por FeignException
        Request mockRequest = Request.create(
                Request.HttpMethod.GET,
                "/sistema/service/getProductList.do",
                new HashMap<>(),
                Request.Body.empty(),
                new RequestTemplate()
        );

        FeignException.Unauthorized unauthorizedException = new FeignException.Unauthorized(
                "Unauthorized",
                mockRequest,
                null,
                new HashMap<>()
        );

        when(productClient.getProductList(anyString())).thenThrow(unauthorizedException);

        // When
        ProductListResponse result = productoService.obtenerListaProductos();

        // Then
        assertNotNull(result);
        assertEquals(401, result.getStatus(), "Debe responder con 401");
        assertEquals("Error de autenticacion con el proveedor externo", result.getMessage());
        assertTrue(result.getProductos().isEmpty());
    }

    /**
     * Prueba el manejo de errores HTTP genéricos del servidor externo (ej. HTTP 500, 502, 503):
     * Verifica que el servicio capture la excepción y propague el código de error correspondiente.
     */
    @Test
    @DisplayName("Debe manejar FeignException genérica (ej. 500, 502) del proveedor")
    void testObtenerListaProductos_FeignExceptionGenerica() {
        // Given: Error 500 simulado proveniente del servidor de GestoPago
        Request mockRequest = Request.create(
                Request.HttpMethod.GET,
                "/sistema/service/getProductList.do",
                new HashMap<>(),
                Request.Body.empty(),
                new RequestTemplate()
        );

        FeignException serverErrorException = new FeignException.InternalServerError(
                "Internal Server Error",
                mockRequest,
                null,
                new HashMap<>()
        );

        when(productClient.getProductList(anyString())).thenThrow(serverErrorException);

        // When
        ProductListResponse result = productoService.obtenerListaProductos();

        // Then
        assertNotNull(result);
        assertEquals(500, result.getStatus(), "El status debe coincidir con el error HTTP recibido");
        assertTrue(result.getMessage().contains("500"));
        assertTrue(result.getProductos().isEmpty());
    }

    /**
     * Prueba el manejo de errores por XML malformado o corrupto:
     * Si el proveedor responde texto que no es XML válido, la deserialización de JAXB falla.
     * El servicio debe capturar la {@link Exception} genérica y retornar status 500 controlado.
     */
    @Test
    @DisplayName("Debe manejar Exception genérica cuando el XML devuelto está corrupto o malformado")
    void testObtenerListaProductos_XmlInvalido() {
        // Given: Respuesta simulada con HTML o texto inválido que no se puede parsear a XML
        String xmlInvalido = "<html><body>502 Bad Gateway</body></html>";

        when(productClient.getProductList(anyString())).thenReturn(xmlInvalido);

        // When
        ProductListResponse result = productoService.obtenerListaProductos();

        // Then
        assertNotNull(result);
        assertEquals(500, result.getStatus(), "Debe retornar 500 por error interno de parseo");
        assertEquals("Error interno al procesar la lista de productos", result.getMessage());
        assertTrue(result.getProductos().isEmpty());
    }

    @Test
    @DisplayName("Debe retornar productos categorizados desde Redis cuando la cache está disponible")
    void testObtenerProductosCategorizados_ExitosoDesdeRedis() {
        ProductItemDto item = ProductItemDto.builder()
                .idProducto(101L)
                .producto("Recarga 100")
                .tipoFront(1)
                .build();
        Map<Integer, List<ProductItemDto>> mockCategorias = Map.of(1, List.of(item));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(mockCategorias);

        ProductCategorizedResponse result = productoService.obtenerProductosCategorizados();

        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertEquals("REDIS", result.getOrigen());
        assertEquals(1, result.getTotalProductos());
        assertTrue(result.getCategorias().containsKey(1));
    }

    @Test
    @DisplayName("Debe hacer fallback a PostgreSQL cuando Redis falla o no tiene datos")
    void testObtenerProductosCategorizados_FallbackPostgresql() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenThrow(new RuntimeException("Redis connection refused"));

        GestoPagoProducto entidad = GestoPagoProducto.builder()
                .idProducto(202L)
                .producto("Servicio Luz")
                .tipoFront(2)
                .build();
        ProductItemDto dto = ProductItemDto.builder()
                .idProducto(202L)
                .producto("Servicio Luz")
                .tipoFront(2)
                .build();

        when(productoRepository.findAllByOrderByTipoFrontAscIdProductoAsc()).thenReturn(List.of(entidad));
        when(productoMapper.toDtoList(List.of(entidad))).thenReturn(List.of(dto));

        ProductCategorizedResponse result = productoService.obtenerProductosCategorizados();

        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertEquals("POSTGRESQL", result.getOrigen());
        assertEquals(1, result.getTotalProductos());
        assertTrue(result.getCategorias().containsKey(2));
    }

    @Test
    @DisplayName("Debe retornar 404 cuando no hay datos en Redis ni en PostgreSQL")
    void testObtenerProductosCategorizados_VacioRetorna404() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);
        when(productoRepository.findAllByOrderByTipoFrontAscIdProductoAsc()).thenReturn(List.of());

        ProductCategorizedResponse result = productoService.obtenerProductosCategorizados();

        assertNotNull(result);
        assertEquals(404, result.getStatus());
        assertEquals("NINGUNO", result.getOrigen());
        assertEquals(0, result.getTotalProductos());
    }
}
