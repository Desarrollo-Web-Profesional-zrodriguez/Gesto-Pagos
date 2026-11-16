package com.proyecto.servicios.service.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.SesionLogin;
import com.proyecto.servicios.entity.cliente.UsuarioLogin;
import com.proyecto.servicios.exception.AutenticacionException;
import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;
import com.proyecto.servicios.repositorys.cliente.SesionLoginRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioLoginRepository;
import com.proyecto.servicios.service.cliente.impl.AuthServiceImpl;
import com.proyecto.servicios.service.cliente.util.PasswordEncryptionUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String PASSWORD_VALIDO = "Contrasena123*";
    private static final String PASSWORD_HASH = PasswordEncryptionUtil.cifrarPassword(PASSWORD_VALIDO);
    private static final String EMBEDDING_VALIDO = "[1.0, 0.0, 0.0]";
    private static final String EMBEDDING_DISTINTO = "[0.0, 1.0, 0.0]";

    @Mock
    private UsuarioLoginRepository usuarioLoginRepository;

    @Mock
    private SesionLoginRepository sesionLoginRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(usuarioLoginRepository, sesionLoginRepository);
    }

    @Test
    @DisplayName("login: usuario no registrado lanza AutenticacionException")
    void testLogin_UsuarioNoExiste_LanzaExcepcion() {
        LoginRequest req = new LoginRequest();
        req.setUsername("noexiste@banco.com");
        req.setPassword(PASSWORD_VALIDO);

        when(usuarioLoginRepository.findByUsername("noexiste@banco.com")).thenReturn(Optional.empty());

        AutenticacionException ex = assertThrows(AutenticacionException.class, () -> authService.login(req));
        assertEquals("El correo electronico no existe", ex.getMessage());
    }

    @Test
    @DisplayName("login: usuario inactivo lanza AutenticacionException")
    void testLogin_UsuarioInactivo_LanzaExcepcion() {
        LoginRequest req = new LoginRequest();
        req.setUsername("inactivo@banco.com");
        req.setPassword(PASSWORD_VALIDO);

        UsuarioLogin usuario = UsuarioLogin.builder()
                .idUsuario(1L)
                .username("inactivo@banco.com")
                .activo(false)
                .build();

        when(usuarioLoginRepository.findByUsername("inactivo@banco.com")).thenReturn(Optional.of(usuario));

        AutenticacionException ex = assertThrows(AutenticacionException.class, () -> authService.login(req));
        assertEquals("El usuario se encuentra inactivo", ex.getMessage());
    }

    @Test
    @DisplayName("login: password exitoso con cliente asociado inactiva sesiones previas y crea nueva")
    void testLogin_PasswordExitoso_ConCliente() {
        LoginRequest req = new LoginRequest();
        req.setUsername("cliente@banco.com");
        req.setPassword(PASSWORD_VALIDO);

        Cliente cliente = Cliente.builder()
                .idCliente(10L)
                .nombre("Juan")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Gomez")
                .build();

        UsuarioLogin usuario = UsuarioLogin.builder()
                .idUsuario(1L)
                .username("cliente@banco.com")
                .passwordHash(PASSWORD_HASH)
                .activo(true)
                .cliente(cliente)
                .build();

        SesionLogin sesionPrevia = SesionLogin.builder()
                .idSesion(100L)
                .tokenSesion("token-viejo")
                .activa(true)
                .build();
        List<SesionLogin> sesionesPrevias = new ArrayList<>(List.of(sesionPrevia));

        when(usuarioLoginRepository.findByUsername("cliente@banco.com")).thenReturn(Optional.of(usuario));
        when(sesionLoginRepository.findByUsuarioIdUsuarioAndActivaTrue(1L)).thenReturn(sesionesPrevias);

        LoginResponse resp = authService.login(req);

        assertNotNull(resp);
        assertNotNull(resp.getTokenSesion());
        assertTrue(resp.getActiva());
        assertEquals("Bearer", resp.getTipoToken());
        assertEquals(5, resp.getMinutosInactividadMaximos());
        assertEquals(1L, resp.getIdUsuario());
        assertEquals(10L, resp.getIdCliente());
        assertEquals("cliente@banco.com", resp.getUsername());
        assertEquals("Juan Perez", resp.getNombreCliente());

        // Verificamos inactivación de sesión previa
        assertFalse(sesionPrevia.getActiva());
        verify(sesionLoginRepository).saveAll(sesionesPrevias);
        verify(sesionLoginRepository).save(any(SesionLogin.class));
    }

    @Test
    @DisplayName("login: password exitoso sin cliente asociado usa username como nombre")
    void testLogin_PasswordExitoso_SinCliente() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin@banco.com");
        req.setPassword(PASSWORD_VALIDO);

        UsuarioLogin usuario = UsuarioLogin.builder()
                .idUsuario(2L)
                .username("admin@banco.com")
                .passwordHash(PASSWORD_HASH)
                .activo(true)
                .cliente(null)
                .build();

        when(usuarioLoginRepository.findByUsername("admin@banco.com")).thenReturn(Optional.of(usuario));
        when(sesionLoginRepository.findByUsuarioIdUsuarioAndActivaTrue(2L)).thenReturn(List.of());

        LoginResponse resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("admin@banco.com", resp.getNombreCliente());
        assertNull(resp.getIdCliente());
    }

    @Test
    @DisplayName("login: password incorrecto lanza AutenticacionException")
    void testLogin_PasswordDistinta_LanzaExcepcion() {
        LoginRequest req = new LoginRequest();
        req.setUsername("cliente@banco.com");
        req.setPassword("passwordErroneo123");

        UsuarioLogin usuario = UsuarioLogin.builder()
                .idUsuario(1L)
                .username("cliente@banco.com")
                .passwordHash(PASSWORD_HASH)
                .activo(true)
                .build();

        when(usuarioLoginRepository.findByUsername("cliente@banco.com")).thenReturn(Optional.of(usuario));

        AutenticacionException ex = assertThrows(AutenticacionException.class, () -> authService.login(req));
        assertEquals("La contrasena es distinta a la registrada", ex.getMessage());
    }

    @Test
    @DisplayName("login: biometria facial exitosa cuando password es nulo o vacio")
    void testLogin_BiometriaFacial_Exitoso() {
        LoginRequest req = new LoginRequest();
        req.setUsername("biometria@banco.com");
        req.setPassword(null);
        req.setBiometricoFacialEmbedding(EMBEDDING_VALIDO);

        UsuarioLogin usuario = UsuarioLogin.builder()
                .idUsuario(3L)
                .username("biometria@banco.com")
                .biometricoFacialEmbedding(EMBEDDING_VALIDO)
                .activo(true)
                .build();

        when(usuarioLoginRepository.findByUsername("biometria@banco.com")).thenReturn(Optional.of(usuario));
        when(sesionLoginRepository.findByUsuarioIdUsuarioAndActivaTrue(3L)).thenReturn(List.of());

        LoginResponse resp = authService.login(req);

        assertNotNull(resp);
        assertTrue(resp.getActiva());
        verify(sesionLoginRepository).save(any(SesionLogin.class));
    }

    @Test
    @DisplayName("login: biometria facial cuando el usuario no tiene perfil registrado lanza excepcion")
    void testLogin_BiometriaFacial_UsuarioSinPerfil_LanzaExcepcion() {
        LoginRequest req = new LoginRequest();
        req.setUsername("sinperfil@banco.com");
        req.setPassword("   ");
        req.setBiometricoFacialEmbedding(EMBEDDING_VALIDO);

        UsuarioLogin usuario = UsuarioLogin.builder()
                .idUsuario(4L)
                .username("sinperfil@banco.com")
                .biometricoFacialEmbedding(null)
                .activo(true)
                .build();

        when(usuarioLoginRepository.findByUsername("sinperfil@banco.com")).thenReturn(Optional.of(usuario));

        AutenticacionException ex = assertThrows(AutenticacionException.class, () -> authService.login(req));
        assertEquals("El usuario no tiene registrado perfil biometrico facial", ex.getMessage());
    }

    @Test
    @DisplayName("login: biometria facial con vector distinto lanza excepcion")
    void testLogin_BiometriaFacial_VectorDistinto_LanzaExcepcion() {
        LoginRequest req = new LoginRequest();
        req.setUsername("biometria@banco.com");
        req.setPassword(null);
        req.setBiometricoFacialEmbedding(EMBEDDING_VALIDO);

        UsuarioLogin usuario = UsuarioLogin.builder()
                .idUsuario(5L)
                .username("biometria@banco.com")
                .biometricoFacialEmbedding(EMBEDDING_DISTINTO)
                .activo(true)
                .build();

        when(usuarioLoginRepository.findByUsername("biometria@banco.com")).thenReturn(Optional.of(usuario));

        AutenticacionException ex = assertThrows(AutenticacionException.class, () -> authService.login(req));
        assertEquals("El vector biometrico facial es distinto al registrado", ex.getMessage());
    }

    @Test
    @DisplayName("login: sin password ni biometria lanza excepcion de credenciales requeridas")
    void testLogin_SinCredenciales_LanzaExcepcion() {
        LoginRequest req = new LoginRequest();
        req.setUsername("cliente@banco.com");
        req.setPassword(null);
        req.setBiometricoFacialEmbedding(null);

        UsuarioLogin usuario = UsuarioLogin.builder()
                .idUsuario(1L)
                .username("cliente@banco.com")
                .activo(true)
                .build();

        when(usuarioLoginRepository.findByUsername("cliente@banco.com")).thenReturn(Optional.of(usuario));

        AutenticacionException ex = assertThrows(AutenticacionException.class, () -> authService.login(req));
        assertEquals("Debe proporcionar contrasena o vector biometrico facial", ex.getMessage());
    }

    @Test
    @DisplayName("login: embedding facial en blanco lanza excepcion de credenciales requeridas")
    void testLogin_BiometriaFacial_EspaciosEnBlanco_LanzaExcepcion() {
        LoginRequest req = new LoginRequest();
        req.setUsername("cliente@banco.com");
        req.setPassword(null);
        req.setBiometricoFacialEmbedding("   ");

        UsuarioLogin usuario = UsuarioLogin.builder()
                .idUsuario(1L)
                .username("cliente@banco.com")
                .activo(true)
                .build();

        when(usuarioLoginRepository.findByUsername("cliente@banco.com")).thenReturn(Optional.of(usuario));

        AutenticacionException ex = assertThrows(AutenticacionException.class, () -> authService.login(req));
        assertEquals("Debe proporcionar contrasena o vector biometrico facial", ex.getMessage());
    }

    @Test
    @DisplayName("validarSesion: token nulo o en blanco lanza AutenticacionException")
    void testValidarSesion_TokenNuloOVacio_LanzaExcepcion() {
        AutenticacionException ex1 = assertThrows(AutenticacionException.class, () -> authService.validarSesion(null));
        assertEquals("Token de sesion no proporcionado", ex1.getMessage());

        AutenticacionException ex2 = assertThrows(AutenticacionException.class, () -> authService.validarSesion("   "));
        assertEquals("Token de sesion no proporcionado", ex2.getMessage());
    }

    @Test
    @DisplayName("validarSesion: token no encontrado lanza AutenticacionException")
    void testValidarSesion_SesionNoExiste_LanzaExcepcion() {
        when(sesionLoginRepository.findByTokenSesion("token-inexistente")).thenReturn(Optional.empty());

        AutenticacionException ex = assertThrows(AutenticacionException.class,
                () -> authService.validarSesion("token-inexistente"));
        assertEquals("Sesion no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("validarSesion: sesion inactiva lanza AutenticacionException")
    void testValidarSesion_SesionInactiva_LanzaExcepcion() {
        SesionLogin sesion = SesionLogin.builder()
                .tokenSesion("token-cerrado")
                .activa(false)
                .build();

        when(sesionLoginRepository.findByTokenSesion("token-cerrado")).thenReturn(Optional.of(sesion));

        AutenticacionException ex = assertThrows(AutenticacionException.class,
                () -> authService.validarSesion("token-cerrado"));
        assertEquals("La sesion se encuentra inactiva o fue cerrada", ex.getMessage());
    }

    @Test
    @DisplayName("validarSesion: sesion expirada por >5 min de inactividad la desactiva y lanza excepcion")
    void testValidarSesion_SesionExpiradaPorInactividad_LanzaExcepcion() {
        LocalDateTime diezMinutosAtras = LocalDateTime.now().minusMinutes(10);
        SesionLogin sesion = SesionLogin.builder()
                .tokenSesion("token-expirado")
                .activa(true)
                .ultimoAcceso(diezMinutosAtras)
                .fechaInicio(diezMinutosAtras.minusMinutes(10))
                .fechaExpiracion(diezMinutosAtras.plusMinutes(5))
                .build();

        when(sesionLoginRepository.findByTokenSesion("token-expirado")).thenReturn(Optional.of(sesion));

        AutenticacionException ex = assertThrows(AutenticacionException.class,
                () -> authService.validarSesion("token-expirado"));

        assertEquals("La sesion ha expirado por inactividad (limite de 5 minutos excedido)", ex.getMessage());
        assertFalse(sesion.getActiva());
        verify(sesionLoginRepository).save(sesion);
    }

    @Test
    @DisplayName("validarSesion: sesion activa dentro de los 5 min renueva ultimoAcceso y retorna true")
    void testValidarSesion_SesionActivaYValida_Exitoso() {
        LocalDateTime unMinutoAtras = LocalDateTime.now().minusMinutes(1);
        SesionLogin sesion = SesionLogin.builder()
                .tokenSesion("token-activo")
                .activa(true)
                .ultimoAcceso(unMinutoAtras)
                .fechaInicio(unMinutoAtras)
                .fechaExpiracion(unMinutoAtras.plusMinutes(5))
                .build();

        when(sesionLoginRepository.findByTokenSesion("token-activo")).thenReturn(Optional.of(sesion));

        boolean resultado = authService.validarSesion("token-activo");

        assertTrue(resultado);
        assertTrue(sesion.getUltimoAcceso().isAfter(unMinutoAtras));
        assertTrue(sesion.getFechaExpiracion().isAfter(LocalDateTime.now()));
        verify(sesionLoginRepository).save(sesion);
    }

    @Test
    @DisplayName("logout: sesion existente la desactiva y guarda")
    void testLogout_SesionExiste_InactivaSesion() {
        SesionLogin sesion = SesionLogin.builder()
                .tokenSesion("token-logout")
                .activa(true)
                .build();

        when(sesionLoginRepository.findByTokenSesion("token-logout")).thenReturn(Optional.of(sesion));

        authService.logout("token-logout");

        assertFalse(sesion.getActiva());
        verify(sesionLoginRepository).save(sesion);
    }

    @Test
    @DisplayName("logout: sesion no existente no ejecuta guardado ni lanza error")
    void testLogout_SesionNoExiste_NoRealizaAccion() {
        when(sesionLoginRepository.findByTokenSesion("token-fantasma")).thenReturn(Optional.empty());

        authService.logout("token-fantasma");

        verify(sesionLoginRepository, never()).save(any(SesionLogin.class));
    }
}
