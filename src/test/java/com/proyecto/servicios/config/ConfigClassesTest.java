package com.proyecto.servicios.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.zaxxer.hikari.HikariDataSource;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.persistence.EntityManagerFactory;

@ExtendWith(MockitoExtension.class)
class ConfigClassesTest {

    // ==========================================
    // 1. ConfigDB Tests
    // ==========================================

    @Test
    @DisplayName("ConfigDB: sfDatasource configura pool Hikari correctamente con properties validos")
    void testSfDatasource_Exitoso() {
        ConfigDB configDB = new ConfigDB();
        Environment env = mock(Environment.class);
        when(env.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:configdb_test;DB_CLOSE_DELAY=-1");
        when(env.getProperty("spring.datasource.username")).thenReturn("sa");
        when(env.getProperty("spring.datasource.password")).thenReturn("");

        DataSource ds = configDB.sfDatasource(env);

        assertNotNull(ds);
        assertTrue(ds instanceof HikariDataSource);
        HikariDataSource hds = (HikariDataSource) ds;
        assertEquals("sfDatasource", hds.getPoolName());
        assertEquals(10, hds.getMaximumPoolSize());
        assertEquals(2, hds.getMinimumIdle());
        hds.close();
    }

    @Test
    @DisplayName("ConfigDB: sfDatasource captura excepcion y retorna null")
    void testSfDatasource_ExcepcionRetornaNull() {
        ConfigDB configDB = new ConfigDB();
        Environment env = mock(Environment.class);
        when(env.getProperty("spring.datasource.url")).thenThrow(new RuntimeException("Error simulado"));

        DataSource ds = configDB.sfDatasource(env);

        assertNull(ds);
    }

    @Test
    @DisplayName("ConfigDB: sfEntityManagerFactory configura LocalContainerEntityManagerFactoryBean")
    void testSfEntityManagerFactory_Exitoso() {
        ConfigDB configDB = new ConfigDB();
        DataSource dataSource = mock(DataSource.class);

        LocalContainerEntityManagerFactoryBean factoryBean = configDB.sfEntityManagerFactory(dataSource);

        assertNotNull(factoryBean);
        assertEquals("sfDatasource", factoryBean.getPersistenceUnitName());
        assertEquals(dataSource, factoryBean.getDataSource());
    }

    @Test
    @DisplayName("ConfigDB: sfTransactionManager crea JpaTransactionManager")
    void testSfTransactionManager_Exitoso() {
        ConfigDB configDB = new ConfigDB();
        EntityManagerFactory emf = mock(EntityManagerFactory.class);

        PlatformTransactionManager txManager = configDB.sfTransactionManager(emf);

        assertNotNull(txManager);
        assertTrue(txManager instanceof JpaTransactionManager);
        assertEquals(emf, ((JpaTransactionManager) txManager).getEntityManagerFactory());
    }

    // ==========================================
    // 2. FlywayConfig Tests
    // ==========================================

    @Test
    @DisplayName("FlywayConfig: configura y ejecuta migraciones via flyway bean")
    void testFlywayConfig_MigrateEjecutado() {
        FlywayConfig flywayConfig = new FlywayConfig();
        ReflectionTestUtils.setField(flywayConfig, "locations", new String[]{"classpath:db/migration"});
        ReflectionTestUtils.setField(flywayConfig, "historyTable", "flyway_schema_history");
        ReflectionTestUtils.setField(flywayConfig, "schema", "public");

        DataSource dataSource = mock(DataSource.class);
        Flyway mockFlyway = mock(Flyway.class);
        FluentConfiguration fluentMock = mock(FluentConfiguration.class);

        when(fluentMock.dataSource(any(DataSource.class))).thenReturn(fluentMock);
        when(fluentMock.locations(any(String[].class))).thenReturn(fluentMock);
        when(fluentMock.table(anyString())).thenReturn(fluentMock);
        when(fluentMock.schemas(anyString())).thenReturn(fluentMock);
        when(fluentMock.baselineOnMigrate(anyBoolean())).thenReturn(fluentMock);
        when(fluentMock.baselineVersion(anyString())).thenReturn(fluentMock);
        when(fluentMock.load()).thenReturn(mockFlyway);
        when(mockFlyway.migrate()).thenReturn(mock(MigrateResult.class));

        try (MockedStatic<Flyway> flywayStatic = mockStatic(Flyway.class)) {
            flywayStatic.when(Flyway::configure).thenReturn(fluentMock);

            Flyway result = flywayConfig.flyway(dataSource);

            assertNotNull(result);
            verify(mockFlyway).migrate();
        }
    }

    // ==========================================
    // 3. RedisConfig Tests
    // ==========================================

    @Test
    @DisplayName("RedisConfig: redisTemplate configura serializadores String y GenericJackson2Json")
    void testRedisTemplate_ConfiguracionSerializadores() {
        RedisConfig redisConfig = new RedisConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);

        assertNotNull(template);
        assertEquals(connectionFactory, template.getConnectionFactory());
        assertTrue(template.getKeySerializer() instanceof StringRedisSerializer);
        assertTrue(template.getHashKeySerializer() instanceof StringRedisSerializer);
        assertTrue(template.getValueSerializer() instanceof GenericJackson2JsonRedisSerializer);
        assertTrue(template.getHashValueSerializer() instanceof GenericJackson2JsonRedisSerializer);
    }

    // ==========================================
    // 4. OpenApi Tests
    // ==========================================

    @Test
    @DisplayName("OpenApi: openAPI bean expone titulo, version y server correcto")
    void testOpenApi_Configuracion() {
        OpenApi openApi = new OpenApi();
        OpenAPI api = openApi.openAPI();

        assertNotNull(api);
        assertNotNull(api.getInfo());
        assertEquals("API Onboarding de Clientes y Gestion Financiera", api.getInfo().getTitle());
        assertEquals("1.0.0", api.getInfo().getVersion());
        assertEquals("Equipo de Desarrollo", api.getInfo().getContact().getName());
        assertNotNull(api.getServers());
        assertFalse(api.getServers().isEmpty());
        assertEquals("/", api.getServers().get(0).getUrl());
    }

    // ==========================================
    // 5. GestoPagoFeignConfig Tests
    // ==========================================

    @Test
    @DisplayName("GestoPagoFeignConfig: inyecta X-API-Key cuando targetName inicia con gestoPago")
    @SuppressWarnings("unchecked")
    void testFeignInterceptor_TargetNameMatch_InyectaHeader() {
        GestoPagoFeignConfig feignConfig = new GestoPagoFeignConfig();
        ReflectionTestUtils.setField(feignConfig, "gestoPagoApiKey", "CLAVE_TEST_API_KEY");

        RequestInterceptor interceptor = feignConfig.gestoPagoApiKeyInterceptor();
        RequestTemplate template = new RequestTemplate();

        Target<Object> mockTarget = mock(Target.class);
        when(mockTarget.name()).thenReturn("gestoPagoAuthClient");
        when(mockTarget.url()).thenReturn("https://proveedor.com/api");
        template.feignTarget(mockTarget);

        interceptor.apply(template);

        Collection<String> headerValues = template.headers().get("X-API-Key");
        assertNotNull(headerValues);
        assertTrue(headerValues.contains("CLAVE_TEST_API_KEY"));
    }

    @Test
    @DisplayName("GestoPagoFeignConfig: inyecta X-API-Key cuando targetUrl contiene gestopago")
    @SuppressWarnings("unchecked")
    void testFeignInterceptor_TargetUrlMatch_InyectaHeader() {
        GestoPagoFeignConfig feignConfig = new GestoPagoFeignConfig();
        ReflectionTestUtils.setField(feignConfig, "gestoPagoApiKey", "CLAVE_TEST_API_KEY");

        RequestInterceptor interceptor = feignConfig.gestoPagoApiKeyInterceptor();
        RequestTemplate template = new RequestTemplate();

        Target<Object> mockTarget = mock(Target.class);
        when(mockTarget.name()).thenReturn("otroCliente");
        when(mockTarget.url()).thenReturn("https://api.gestopago.com/tx");
        template.feignTarget(mockTarget);

        interceptor.apply(template);

        Collection<String> headerValues = template.headers().get("X-API-Key");
        assertNotNull(headerValues);
        assertTrue(headerValues.contains("CLAVE_TEST_API_KEY"));
    }

    @Test
    @DisplayName("GestoPagoFeignConfig: no inyecta X-API-Key cuando el target no coincide")
    @SuppressWarnings("unchecked")
    void testFeignInterceptor_NoMatch_NoInyectaHeader() {
        GestoPagoFeignConfig feignConfig = new GestoPagoFeignConfig();
        ReflectionTestUtils.setField(feignConfig, "gestoPagoApiKey", "CLAVE_TEST_API_KEY");

        RequestInterceptor interceptor = feignConfig.gestoPagoApiKeyInterceptor();
        RequestTemplate template = new RequestTemplate();

        Target<Object> mockTarget = mock(Target.class);
        when(mockTarget.name()).thenReturn("brevoClient");
        when(mockTarget.url()).thenReturn("https://api.brevo.com");
        template.feignTarget(mockTarget);

        interceptor.apply(template);

        assertNull(template.headers().get("X-API-Key"));
    }

    @Test
    @DisplayName("GestoPagoFeignConfig: no inyecta X-API-Key cuando target es nulo")
    void testFeignInterceptor_NullTarget_NoInyectaHeader() {
        GestoPagoFeignConfig feignConfig = new GestoPagoFeignConfig();
        ReflectionTestUtils.setField(feignConfig, "gestoPagoApiKey", "CLAVE_TEST_API_KEY");

        RequestInterceptor interceptor = feignConfig.gestoPagoApiKeyInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertNull(template.headers().get("X-API-Key"));
    }

    @Test
    @DisplayName("GestoPagoFeignConfig: no inyecta X-API-Key cuando api-key es nula o blanca")
    @SuppressWarnings("unchecked")
    void testFeignInterceptor_ApiKeyNulaOBlanca_NoInyectaHeader() {
        GestoPagoFeignConfig feignConfig = new GestoPagoFeignConfig();
        ReflectionTestUtils.setField(feignConfig, "gestoPagoApiKey", null);

        RequestInterceptor interceptor = feignConfig.gestoPagoApiKeyInterceptor();
        RequestTemplate template = new RequestTemplate();

        Target<Object> mockTarget = mock(Target.class);
        when(mockTarget.name()).thenReturn("gestoPagoClient");
        template.feignTarget(mockTarget);

        interceptor.apply(template);
        assertNull(template.headers().get("X-API-Key"));

        // Caso con apiKey en blanco
        ReflectionTestUtils.setField(feignConfig, "gestoPagoApiKey", "   ");
        RequestTemplate template2 = new RequestTemplate();
        template2.feignTarget(mockTarget);
        interceptor.apply(template2);
        assertNull(template2.headers().get("X-API-Key"));
    }

    // ==========================================
    // 6. FlexibleLocalDateDeserializer Tests
    // ==========================================

    @Test
    @DisplayName("FlexibleLocalDateDeserializer: texto nulo, vacio o en blanco retorna null")
    void testLocalDateDeserializer_NuloOVacio() throws IOException {
        FlexibleLocalDateDeserializer deserializer = new FlexibleLocalDateDeserializer();
        JsonParser p = mock(JsonParser.class);
        DeserializationContext ctxt = mock(DeserializationContext.class);

        when(p.getText()).thenReturn(null);
        assertNull(deserializer.deserialize(p, ctxt));

        when(p.getText()).thenReturn("");
        assertNull(deserializer.deserialize(p, ctxt));

        when(p.getText()).thenReturn("    ");
        assertNull(deserializer.deserialize(p, ctxt));
    }

    @Test
    @DisplayName("FlexibleLocalDateDeserializer: fecha valida en formato AAAA-MM-DD es parseada")
    void testLocalDateDeserializer_FechasValidas() throws IOException {
        FlexibleLocalDateDeserializer deserializer = new FlexibleLocalDateDeserializer();
        JsonParser p = mock(JsonParser.class);
        DeserializationContext ctxt = mock(DeserializationContext.class);

        when(p.getText()).thenReturn("1992-05-20");
        assertEquals(LocalDate.of(1992, 5, 20), deserializer.deserialize(p, ctxt));

        when(p.getText()).thenReturn("  1985-12-31  ");
        assertEquals(LocalDate.of(1985, 12, 31), deserializer.deserialize(p, ctxt));

        // Año bisiesto
        when(p.getText()).thenReturn("2024-02-29");
        assertEquals(LocalDate.of(2024, 2, 29), deserializer.deserialize(p, ctxt));
    }

    @Test
    @DisplayName("FlexibleLocalDateDeserializer: formato incorrecto lanza IllegalArgumentException")
    void testLocalDateDeserializer_FormatoInvalido_LanzaExcepcion() throws IOException {
        FlexibleLocalDateDeserializer deserializer = new FlexibleLocalDateDeserializer();
        JsonParser p = mock(JsonParser.class);
        DeserializationContext ctxt = mock(DeserializationContext.class);

        String[] formatosInvalidos = {"1992/05/20", "20-05-1992", "1992-5-20", "texto", "1672531199000", "1992-05-20T10:00:00"};
        for (String f : formatosInvalidos) {
            when(p.getText()).thenReturn(f);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> deserializer.deserialize(p, ctxt));
            assertTrue(ex.getMessage().contains("formato de fecha invalido"));
        }
    }

    @Test
    @DisplayName("FlexibleLocalDateDeserializer: fecha inexistente lanza IllegalArgumentException")
    void testLocalDateDeserializer_FechaInexistente_LanzaExcepcion() throws IOException {
        FlexibleLocalDateDeserializer deserializer = new FlexibleLocalDateDeserializer();
        JsonParser p = mock(JsonParser.class);
        DeserializationContext ctxt = mock(DeserializationContext.class);

        String[] fechasInexistentes = {"1992-13-01", "1992-00-10", "1992-05-00", "1992-05-32"};
        for (String f : fechasInexistentes) {
            when(p.getText()).thenReturn(f);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> deserializer.deserialize(p, ctxt));
            assertTrue(ex.getMessage().contains("fecha invalida o inexistente"));
        }
    }

    // ==========================================
    // 7. StrictTwoDecimalNumberDeserializer Tests
    // ==========================================

    @Test
    @DisplayName("StrictTwoDecimalNumberDeserializer: token String entre comillas es rechazado")
    void testStrictTwoDecimal_ComillasRechazado() throws IOException {
        StrictTwoDecimalNumberDeserializer deserializer = new StrictTwoDecimalNumberDeserializer();
        JsonParser p = mock(JsonParser.class);
        DeserializationContext ctxt = mock(DeserializationContext.class);

        when(p.currentToken()).thenReturn(JsonToken.VALUE_STRING);
        when(p.getText()).thenReturn("200.00");
        when(p.currentName()).thenReturn("monto");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> deserializer.deserialize(p, ctxt));
        assertTrue(ex.getMessage().contains("no debe enviarse entre comillas"));

        // Caso con currentName nulo (usa DEFAULT_FIELD_NAME)
        when(p.currentName()).thenReturn(null);
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> deserializer.deserialize(p, ctxt));
        assertTrue(ex2.getMessage().contains("El campo 'monto' no debe enviarse entre comillas"));
    }

    @Test
    @DisplayName("StrictTwoDecimalNumberDeserializer: token no numerico es rechazado")
    void testStrictTwoDecimal_NoNumericoRechazado() throws IOException {
        StrictTwoDecimalNumberDeserializer deserializer = new StrictTwoDecimalNumberDeserializer();
        JsonParser p = mock(JsonParser.class);
        DeserializationContext ctxt = mock(DeserializationContext.class);

        when(p.currentToken()).thenReturn(JsonToken.VALUE_NULL);
        when(p.currentName()).thenReturn("monto");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> deserializer.deserialize(p, ctxt));
        assertTrue(ex.getMessage().contains("debe ser un valor numerico directo con dos decimales"));

        when(p.currentToken()).thenReturn(JsonToken.VALUE_TRUE);
        when(p.currentName()).thenReturn(null);
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> deserializer.deserialize(p, ctxt));
        assertTrue(ex2.getMessage().contains("debe ser un valor numerico directo con dos decimales"));
    }

    @Test
    @DisplayName("StrictTwoDecimalNumberDeserializer: numero sin dos decimales exactos es rechazado")
    void testStrictTwoDecimal_DecimalesInvalidosRechazado() throws IOException {
        StrictTwoDecimalNumberDeserializer deserializer = new StrictTwoDecimalNumberDeserializer();
        JsonParser p = mock(JsonParser.class);
        DeserializationContext ctxt = mock(DeserializationContext.class);

        String[] invalidos = {"200", "200.5", "200.555", "abc", ""};
        for (String inv : invalidos) {
            when(p.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_FLOAT);
            when(p.getText()).thenReturn(inv);
            when(p.currentName()).thenReturn("monto");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> deserializer.deserialize(p, ctxt));
            assertTrue(ex.getMessage().contains("debe ser un numero con exactamente dos decimales"));
        }

        // Test con currentName null y texto nulo
        when(p.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_FLOAT);
        when(p.getText()).thenReturn(null);
        when(p.currentName()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> deserializer.deserialize(p, ctxt));
    }

    @Test
    @DisplayName("StrictTwoDecimalNumberDeserializer: numero con dos decimales exactos es deserializado")
    void testStrictTwoDecimal_DosDecimalesExitoso() throws IOException {
        StrictTwoDecimalNumberDeserializer deserializer = new StrictTwoDecimalNumberDeserializer();
        JsonParser p = mock(JsonParser.class);
        DeserializationContext ctxt = mock(DeserializationContext.class);

        when(p.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_FLOAT);
        when(p.getText()).thenReturn("200.00");
        assertEquals(new BigDecimal("200.00"), deserializer.deserialize(p, ctxt));

        when(p.getText()).thenReturn("0.50");
        assertEquals(new BigDecimal("0.50"), deserializer.deserialize(p, ctxt));

        when(p.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_INT);
        when(p.getText()).thenReturn("12345.67");
        assertEquals(new BigDecimal("12345.67"), deserializer.deserialize(p, ctxt));
    }

    @Test
    @DisplayName("CorsConfig: construye configuracion con origenes personalizados y por defecto")
    void testCorsConfig() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "https://app.test, https://admin.test");
        org.springframework.web.cors.CorsConfiguration config = corsConfig.buildCorsConfiguration();
        assertNotNull(config);
        assertEquals(2, config.getAllowedOriginPatterns().size());
        assertTrue(config.getAllowedOriginPatterns().contains("https://app.test"));
        assertTrue(config.getAllowCredentials());

        org.springframework.web.filter.CorsFilter filter = corsConfig.corsFilter();
        assertNotNull(filter);

        // Rama con origenes vacios -> cae al fallback "*"
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "  ,  ");
        org.springframework.web.cors.CorsConfiguration defaultConfig = corsConfig.buildCorsConfiguration();
        assertNotNull(defaultConfig);
        assertEquals(1, defaultConfig.getAllowedOriginPatterns().size());
        assertEquals("*", defaultConfig.getAllowedOriginPatterns().get(0));
    }
}
