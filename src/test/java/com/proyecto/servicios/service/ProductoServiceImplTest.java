package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.model.gestopago.catalogo.ProductItemDto;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private GestoPagoProductClient productClient;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private static final String DUMMY_TOKEN = "mi_token_secreto_123";

    @BeforeEach
    void setUp() {
        // Inyectamos el valor de la propiedad @Value tokenConfigurado
        ReflectionTestUtils.setField(productoService, "tokenConfigurado", DUMMY_TOKEN);
    }

    @Test
    @DisplayName("Debe retornar lista de productos exitosamente cuando el cliente externo responde 200 OK")
    void testObtenerListaProductos_Exitoso() {
        // 1. Given: Datos simulados de prueba
        ProductItemDto item = ProductItemDto.builder()
                .idProducto(1L)
                .nombre("Recarga Telefónica")
                .descripcion("Recarga de saldo")
                .categoria("Telefonía")
                .precio(new BigDecimal("100.00"))
                .activo(true)
                .build();

        ProductListResponse mockResponse = ProductListResponse.builder()
                .status(200)
                .message("Operación exitosa")
                .productos(List.of(item))
                .build();

        when(productClient.getProductList("Bearer " + DUMMY_TOKEN)).thenReturn(mockResponse);

        // 2. When: Ejecución del método a probar
        ProductListResponse result = productoService.obtenerListaProductos();

        // 3. Then: Aserciones / Validaciones
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertEquals("Operación exitosa", result.getMessage());
        assertNotNull(result.getProductos());
        assertFalse(result.getProductos().isEmpty());
        assertEquals(1, result.getProductos().size());
        assertEquals("Recarga Telefónica", result.getProductos().get(0).getNombre());

        verify(productClient).getProductList("Bearer " + DUMMY_TOKEN);
    }

    @Test
    @DisplayName("Debe retornar status 401 cuando el token no está configurado")
    void testObtenerListaProductos_TokenNoConfigurado() {
        // Given: Simulamos que la propiedad está vacía
        ReflectionTestUtils.setField(productoService, "tokenConfigurado", "");

        // When
        ProductListResponse result = productoService.obtenerListaProductos();

        // Then
        assertNotNull(result);
        assertEquals(401, result.getStatus());
        assertTrue(result.getProductos().isEmpty());
    }

    @Test
    @DisplayName("Debe manejar RetryableException (Timeout) y retornar respuesta de error controlada")
    void testObtenerListaProductos_Timeout() {
        // Given: Simulamos un timeout del cliente Feign
        Request mockRequest = Request.create(
                Request.HttpMethod.GET,
                "/sistema/service/getProductList.do",
                new HashMap<>(),
                Request.Body.empty(),
                new RequestTemplate()
        );

        RetryableException timeoutException = new RetryableException(
                504,
                "Read timed out",
                Request.HttpMethod.GET,
                new Date(),
                mockRequest
        );

        when(productClient.getProductList(anyString())).thenThrow(timeoutException);

        // When
        ProductListResponse result = productoService.obtenerListaProductos();

        // Then
        assertNotNull(result);
        assertTrue(result.getStatus() == 504 || result.getStatus() == 501);
        assertTrue(result.getProductos().isEmpty());
    }
}
