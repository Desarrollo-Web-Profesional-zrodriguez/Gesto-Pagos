package com.proyecto.servicios.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LogoServicioResolverTest {

    private LogoServicioResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new LogoServicioResolver();
    }

    @Test
    @DisplayName("Debe resolver el logotipo oficial para IZZI Telecom")
    void testResolverLogo_Izzi() {
        String logo = resolver.resolverUrlLogo("IZZI Telecom", 76L, 205L);
        assertNotNull(logo);
        assertTrue(logo.contains("Logo_Izzi_Telecom"));
    }

    @Test
    @DisplayName("Debe resolver el logotipo oficial para CFE")
    void testResolverLogo_CFE() {
        String logo = resolver.resolverUrlLogo("CFE Suministrador de Servicios Basicos", 10L, 100L);
        assertNotNull(logo);
        assertTrue(logo.contains("CFE_logo"));
    }

    @Test
    @DisplayName("Debe resolver el logotipo oficial para Telcel")
    void testResolverLogo_Telcel() {
        String logo = resolver.resolverUrlLogo("Telcel Tiempo Aire", 1L, 2L);
        assertNotNull(logo);
        assertTrue(logo.contains("Telcel_logo"));
    }

    @Test
    @DisplayName("Debe resolver el logotipo por coincidencia en el nombre de producto cuando el servicio es generico")
    void testResolverLogo_PorProducto() {
        String logo = resolver.resolverUrlLogo("Pago de Servicios", "Recarga Movistar 100", 99L, 999L);
        assertNotNull(logo);
        assertTrue(logo.contains("Movistar_logo"));
    }

    @Test
    @DisplayName("Debe retornar el logo default cuando el servicio no esta en el catalogo de marcas conocidas")
    void testResolverLogo_DefaultFallback() {
        String logo = resolver.resolverUrlLogo("Servicio Desconocido XYZ", 9999L, 8888L);
        assertNotNull(logo);
        assertEquals(LogoServicioResolver.LOGO_DEFAULT_SERVICIO, logo);
    }

    @Test
    @DisplayName("Debe resolver la URL oficial del logo de PuntoRed usando idServicio")
    void testResolverUrlLogoPuntoRed() {
        String urlLogoPuntoRed = resolver.resolverUrlLogoPuntoRed(76L);
        assertNotNull(urlLogoPuntoRed);
        assertEquals("https://gestopago.portalventas.net/sistema/images/gestopago/servicios/76.png", urlLogoPuntoRed);
    }

    @Test
    @DisplayName("Debe resolver la URL oficial de la imagen de referencia de PuntoRed usando idServicio")
    void testResolverUrlReferenciaPuntoRed() {
        String urlReferencia = resolver.resolverUrlReferenciaPuntoRed(76L);
        assertNotNull(urlReferencia);
        assertEquals("https://gestopago.portalventas.net/sistema/images/gestopago/referencias/76.jpg", urlReferencia);
    }
}
