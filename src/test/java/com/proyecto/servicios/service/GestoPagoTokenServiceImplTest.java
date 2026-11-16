package com.proyecto.servicios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.Impl.GestoPagoTokenServiceImpl;

@ExtendWith(MockitoExtension.class)
class GestoPagoTokenServiceImplTest {

    private static final Integer ID_DISTRIBUIDOR = 83;
    private static final String CODIGO_DISPOSITIVO = "DISP_TEST";
    private static final String PASSWORD = "password123";

    @Mock
    private GestoPagoAuthClient gestoPagoAuthClient;

    @Mock
    private GestoPagoTokenRepository tokenRepository;

    @Mock
    private GestoPagoTokenMapper tokenMapper;

    private GestoPagoTokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new GestoPagoTokenServiceImpl(gestoPagoAuthClient, tokenRepository, tokenMapper);
        ReflectionTestUtils.setField(tokenService, "idDistribuidor", ID_DISTRIBUIDOR);
        ReflectionTestUtils.setField(tokenService, "codigoDispositivo", CODIGO_DISPOSITIVO);
        ReflectionTestUtils.setField(tokenService, "password", PASSWORD);
    }

    @Test
    @DisplayName("verificarTokenProgramado: token vigente en BD no detona renovación")
    void testVerificarTokenProgramado_TokenVigenteEnBD_NoRenueva() {
        GestoPagoToken tokenVigente = new GestoPagoToken();
        tokenVigente.setId(1);
        tokenVigente.setToken("token-vigente-123");
        tokenVigente.setActivo(true);
        tokenVigente.setFechaActualizacion(LocalDateTime.now().minusHours(2));

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenVigente));

        tokenService.verificarTokenProgramado();

        verify(gestoPagoAuthClient, never()).authenticate(anyInt(), anyString(), anyString());
        assertEquals("token-vigente-123", tokenService.obtenerTokenValido());
    }

    @Test
    @DisplayName("verificarTokenProgramado: token expirado (>20h) renueva token")
    void testVerificarTokenProgramado_TokenExpirado_RenuevaToken() {
        GestoPagoToken tokenExpirado = new GestoPagoToken();
        tokenExpirado.setId(1);
        tokenExpirado.setToken("token-antiguo");
        tokenExpirado.setActivo(true);
        tokenExpirado.setFechaActualizacion(LocalDateTime.now().minusHours(21));

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenExpirado));

        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setStatus(200);
        authResp.setToken("token-renovado-nuevo");
        authResp.setTokenType("Bearer");
        authResp.setExpiresIn(86400L);

        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(authResp);

        tokenService.verificarTokenProgramado();

        verify(gestoPagoAuthClient).authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD);
        verify(tokenRepository).save(any(GestoPagoToken.class));
    }

    @Test
    @DisplayName("verificarTokenProgramado: token inactivo en BD detona renovación")
    void testVerificarTokenProgramado_TokenInactivo_RenuevaToken() {
        GestoPagoToken tokenInactivo = new GestoPagoToken();
        tokenInactivo.setId(1);
        tokenInactivo.setToken("token-inactivo");
        tokenInactivo.setActivo(false);
        tokenInactivo.setFechaActualizacion(LocalDateTime.now().minusHours(1));

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenInactivo));

        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setStatus(200);
        authResp.setToken("token-reactivado");
        authResp.setTokenType("Bearer");

        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(authResp);

        tokenService.verificarTokenProgramado();

        verify(gestoPagoAuthClient).authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD);
        verify(tokenRepository).save(any(GestoPagoToken.class));
    }

    @Test
    @DisplayName("verificarTokenProgramado: token en blanco en BD detona renovación")
    void testVerificarTokenProgramado_TokenVacio_RenuevaToken() {
        GestoPagoToken tokenVacio = new GestoPagoToken();
        tokenVacio.setId(1);
        tokenVacio.setToken("   ");
        tokenVacio.setActivo(true);
        tokenVacio.setFechaActualizacion(LocalDateTime.now().minusHours(1));

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenVacio));

        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setStatus(200);
        authResp.setToken("token-nuevo");

        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(authResp);

        tokenService.verificarTokenProgramado();

        verify(gestoPagoAuthClient).authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD);
    }

    @Test
    @DisplayName("verificarTokenProgramado: cuando no existe token en BD, detona renovación")
    void testVerificarTokenProgramado_TokenNoExiste_RenuevaToken() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.empty());

        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setStatus(200);
        authResp.setToken("primer-token");

        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(authResp);

        tokenService.verificarTokenProgramado();

        verify(gestoPagoAuthClient).authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD);
        verify(tokenRepository).save(any(GestoPagoToken.class));
    }

    @Test
    @DisplayName("verificarTokenProgramado: fallo de BD es capturado y procede a renovar")
    void testVerificarTokenProgramado_FalloConsultaBD_CapturaExcepcionYRenueva() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenThrow(new RuntimeException("Error temporal PostgreSQL"));

        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setStatus(200);
        authResp.setToken("token-tras-error-bd");

        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(authResp);

        tokenService.verificarTokenProgramado();

        verify(gestoPagoAuthClient).authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD);
    }

    @Test
    @DisplayName("renovarToken: cuando no hay entidad previa en BD, crea nueva entidad y la guarda")
    void testRenovarToken_NuevoToken_CreaYGuardaEntidad() {
        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setStatus(200);
        authResp.setToken("token-xyz");
        authResp.setTokenType("Bearer");
        authResp.setExpiresIn(72000L);

        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(authResp);

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.empty());

        GestoPagoToken entidadNueva = new GestoPagoToken();
        when(tokenMapper.toEntity(authResp)).thenReturn(entidadNueva);

        tokenService.renovarToken();

        ArgumentCaptor<GestoPagoToken> captor = ArgumentCaptor.forClass(GestoPagoToken.class);
        verify(tokenRepository).save(captor.capture());
        GestoPagoToken guardado = captor.getValue();
        assertEquals("token-xyz", guardado.getToken());
        assertEquals("Bearer", guardado.getTokenType());
        assertEquals(72000L, guardado.getExpiresIn());
        assertEquals(ID_DISTRIBUIDOR, guardado.getIdDistribuidor());
        assertEquals(CODIGO_DISPOSITIVO, guardado.getCodigoDispositivo());
        assertTrue(guardado.getActivo());
    }

    @Test
    @DisplayName("renovarToken: cuando hay entidad existente en BD, actualiza entidad y la guarda")
    void testRenovarToken_TokenExistente_ActualizaYGuardaEntidad() {
        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setStatus(200);
        authResp.setToken("token-actualizado");
        authResp.setTokenType("Bearer");
        authResp.setExpiresIn(86400L);

        GestoPagoToken tokenExistente = new GestoPagoToken();
        tokenExistente.setId(5);
        tokenExistente.setToken("antiguo");
        tokenExistente.setIdDistribuidor(ID_DISTRIBUIDOR);
        tokenExistente.setCodigoDispositivo(CODIGO_DISPOSITIVO);

        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(authResp);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenExistente));

        tokenService.renovarToken();

        verify(tokenMapper).updateEntity(authResp, tokenExistente);
        verify(tokenRepository).save(tokenExistente);
        assertEquals("token-actualizado", tokenExistente.getToken());
        assertTrue(tokenExistente.getActivo());
    }

    @Test
    @DisplayName("renovarToken: fallback manual cuando mapper es null o retorna null")
    void testRenovarToken_TokenMapperNulo_CreaEntidadManualmente() {
        GestoPagoTokenServiceImpl serviceSinMapper = new GestoPagoTokenServiceImpl(gestoPagoAuthClient, tokenRepository, null);
        ReflectionTestUtils.setField(serviceSinMapper, "idDistribuidor", ID_DISTRIBUIDOR);
        ReflectionTestUtils.setField(serviceSinMapper, "codigoDispositivo", CODIGO_DISPOSITIVO);
        ReflectionTestUtils.setField(serviceSinMapper, "password", PASSWORD);

        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setStatus(200);
        authResp.setToken("token-fallback");
        authResp.setTokenType("Bearer");
        authResp.setExpiresIn(3600L);

        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(authResp);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.empty());

        serviceSinMapper.renovarToken();

        ArgumentCaptor<GestoPagoToken> captor = ArgumentCaptor.forClass(GestoPagoToken.class);
        verify(tokenRepository).save(captor.capture());
        GestoPagoToken guardado = captor.getValue();
        assertEquals("token-fallback", guardado.getToken());
        assertEquals(ID_DISTRIBUIDOR, guardado.getIdDistribuidor());
    }

    @Test
    @DisplayName("renovarToken: respuesta nula o sin token no persiste nada")
    void testRenovarToken_RespuestaProveedorNulaOInvalida_NoGuarda() {
        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(null);

        tokenService.renovarToken();

        verify(tokenRepository, never()).save(any(GestoPagoToken.class));

        // Prueba con token nulo y token vacío
        GestoPagoAuthResponse respSinToken = new GestoPagoAuthResponse();
        respSinToken.setToken("   ");
        respSinToken.setMessage("Credenciales invalidas");
        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(respSinToken);

        tokenService.renovarToken();
        verify(tokenRepository, never()).save(any(GestoPagoToken.class));
    }

    @Test
    @DisplayName("renovarToken: excepción remota en cliente auth es capturada sin propagarse")
    void testRenovarToken_ExcepcionClienteFeign_CapturaYRegistraError() {
        when(gestoPagoAuthClient.authenticate(anyInt(), anyString(), anyString()))
                .thenThrow(new RuntimeException("503 Service Unavailable"));

        tokenService.renovarToken();

        verify(tokenRepository, never()).save(any(GestoPagoToken.class));
    }

    @Test
    @DisplayName("obtenerTokenActivo: limpia codigoDispositivo cuando es nulo")
    void testObtenerTokenActivo_CodigoDispositivoNulo_LimpiaCadena() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, ""))
                .thenReturn(Optional.empty());

        Optional<GestoPagoToken> opt = tokenService.obtenerTokenActivo(83, null);

        assertNotNull(opt);
        verify(tokenRepository).findByIdDistribuidorAndCodigoDispositivo(83, "");
    }

    @Test
    @DisplayName("obtenerTokenValido: token activo en BD retorna directamente sin renovar")
    void testObtenerTokenValido_TokenActivoEnBD_RetornaDirecto() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token-activo-100");
        token.setActivo(true);

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(token));

        String tokenResult = tokenService.obtenerTokenValido();

        assertEquals("token-activo-100", tokenResult);
        verify(gestoPagoAuthClient, never()).authenticate(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("obtenerTokenValido: sin token activo, renueva y retorna nuevo de BD")
    void testObtenerTokenValido_SinTokenActivo_RenuevaYRetornaNuevoDeBD() {
        GestoPagoToken tokenNuevo = new GestoPagoToken();
        tokenNuevo.setToken("token-guardado-bd");
        tokenNuevo.setActivo(true);

        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setStatus(200);
        authResp.setToken("token-guardado-bd");

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.empty()) // Primer lookup en obtenerTokenValido
                .thenReturn(Optional.empty()) // Lookup dentro de resolverTokenEntity
                .thenReturn(Optional.of(tokenNuevo)); // Segundo lookup tras renovación

        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(authResp);

        String result = tokenService.obtenerTokenValido();

        assertEquals("token-guardado-bd", result);
        verify(tokenRepository).save(any(GestoPagoToken.class));
    }

    @Test
    @DisplayName("obtenerTokenValido: renovación falla en segundo lookup de BD pero retorna desde memoria")
    void testObtenerTokenValido_SinTokenActivo_RenovacionFallaEnBD_RetornaDesdeMemoria() {
        GestoPagoAuthResponse authResp = new GestoPagoAuthResponse();
        authResp.setStatus(200);
        authResp.setToken("token-en-memoria-fallback");

        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(authResp);

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.empty()); // Siempre vacío en BD

        String result = tokenService.obtenerTokenValido();

        assertEquals("token-en-memoria-fallback", result);
    }

    @Test
    @DisplayName("obtenerTokenValido: sin token activo en BD ni memoria, y renovación falla, retorna null")
    void testObtenerTokenValido_SinTokenActivo_SinMemoria_RetornaNull() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.empty());

        when(gestoPagoAuthClient.authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD))
                .thenReturn(null);

        String result = tokenService.obtenerTokenValido();

        assertNull(result);
    }

    @Test
    @DisplayName("verificarTokenProgramado: codigoDispositivo nulo usa cadena vacia")
    void testVerificarTokenProgramado_CodigoDispositivoNulo() {
        ReflectionTestUtils.setField(tokenService, "codigoDispositivo", null);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, ""))
                .thenReturn(Optional.empty());

        tokenService.verificarTokenProgramado();

        verify(gestoPagoAuthClient).authenticate(ID_DISTRIBUIDOR, "", PASSWORD);
    }

    @Test
    @DisplayName("renovarToken: codigoDispositivo y password nulos limpian a cadena vacia")
    void testRenovarToken_CredencialesNulas() {
        ReflectionTestUtils.setField(tokenService, "codigoDispositivo", null);
        ReflectionTestUtils.setField(tokenService, "password", null);

        tokenService.renovarToken();

        verify(gestoPagoAuthClient).authenticate(ID_DISTRIBUIDOR, "", "");
    }

    @Test
    @DisplayName("obtenerTokenValido: token en BD con token nulo o inactivo detona renovacion")
    void testObtenerTokenValido_TokenEnBDConTokenNuloOInactivo() {
        GestoPagoToken tokenSinString = new GestoPagoToken();
        tokenSinString.setToken(null);
        tokenSinString.setActivo(true);

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenSinString));

        tokenService.obtenerTokenValido();

        verify(gestoPagoAuthClient).authenticate(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO, PASSWORD);
    }

    @Test
    @DisplayName("obtenerTokenValido: enMemoria con espacios en blanco retorna null")
    void testObtenerTokenValido_EnMemoriaBlancoRetornaNull() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.empty());

        // Forzar memoria con string en blanco
        java.util.concurrent.atomic.AtomicReference<String> mem =
                (java.util.concurrent.atomic.AtomicReference<String>) ReflectionTestUtils.getField(tokenService, "tokenEnMemoria");
        mem.set("   ");

        assertNull(tokenService.obtenerTokenValido());
    }

    @Test
    @DisplayName("obtenerTokenValido: ramas de token vacio, inactivo o activo nulo detonan renovacion")
    void testObtenerTokenValido_RamasTokenInvalido() {
        // Token vacio con espacios
        GestoPagoToken tokenVacio = new GestoPagoToken();
        tokenVacio.setToken("   ");
        tokenVacio.setActivo(true);

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenVacio));
        tokenService.obtenerTokenValido();

        // Token inactivo (false)
        GestoPagoToken tokenInactivo = new GestoPagoToken();
        tokenInactivo.setToken("valido");
        tokenInactivo.setActivo(false);

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenInactivo));
        tokenService.obtenerTokenValido();

        // Token con activo null
        GestoPagoToken tokenActivoNull = new GestoPagoToken();
        tokenActivoNull.setToken("valido");
        tokenActivoNull.setActivo(null);

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenActivoNull));
        tokenService.obtenerTokenValido();
    }

    @Test
    @DisplayName("verificarTokenProgramado: ramas de token sin fecha, token vacio, inactivo o activo nulo")
    void testVerificarTokenProgramado_RamasExpiracionEInvalidez() {
        // Fecha actualizacion nula
        GestoPagoToken tokenSinFecha = new GestoPagoToken();
        tokenSinFecha.setToken("token-ok");
        tokenSinFecha.setActivo(true);
        tokenSinFecha.setFechaActualizacion(null);

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenSinFecha));
        tokenService.verificarTokenProgramado();

        // Token nulo en entidad
        GestoPagoToken tokenNulo = new GestoPagoToken();
        tokenNulo.setToken(null);
        tokenNulo.setActivo(true);
        tokenNulo.setFechaActualizacion(LocalDateTime.now());

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenNulo));
        tokenService.verificarTokenProgramado();

        // Token inactivo
        GestoPagoToken tokenInactivo = new GestoPagoToken();
        tokenInactivo.setToken("token-ok");
        tokenInactivo.setActivo(false);
        tokenInactivo.setFechaActualizacion(LocalDateTime.now());

        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(tokenInactivo));
        tokenService.verificarTokenProgramado();
    }
}
