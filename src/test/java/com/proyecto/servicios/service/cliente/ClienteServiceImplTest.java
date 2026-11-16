package com.proyecto.servicios.service.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.cliente.Nacionalidad;
import com.proyecto.servicios.entity.cliente.Saldo;
import com.proyecto.servicios.entity.cliente.UsuarioLogin;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.model.cliente.ClienteBusquedaAvanzadaRequest;
import com.proyecto.servicios.model.cliente.ClientePatchRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.model.cliente.DomicilioDto;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.cliente.NacionalidadRepository;
import com.proyecto.servicios.repositorys.cliente.SaldoRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioLoginRepository;
import com.proyecto.servicios.service.cliente.impl.ClienteServiceImpl;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private DomicilioRepository domicilioRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private SaldoRepository saldoRepository;
    @Mock
    private UsuarioLoginRepository usuarioLoginRepository;
    @Mock
    private NacionalidadRepository nacionalidadRepository;

    private ClienteServiceImpl clienteService;

    @BeforeEach
    void setUp() {
        clienteService = new ClienteServiceImpl(
                clienteRepository,
                domicilioRepository,
                cuentaRepository,
                saldoRepository,
                usuarioLoginRepository,
                nacionalidadRepository
        );
        org.mockito.Mockito.lenient().when(cuentaRepository.save(any(Cuenta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(domicilioRepository.save(any(Domicilio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(saldoRepository.save(any(Saldo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(usuarioLoginRepository.save(any(UsuarioLogin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private DomicilioDto crearDomicilioDto() {
        return DomicilioDto.builder()
                .calle("Insurgentes Sur")
                .numeroExterior("123")
                .numeroInterior("4B")
                .colonia("Del Valle")
                .municipio("Benito Juarez")
                .estado("CDMX")
                .codigoPostal("03100")
                .pais("Mexico")
                .build();
    }

    private ClienteRegistroRequest crearRegistroRequest() {
        ClienteRegistroRequest req = new ClienteRegistroRequest();
        req.setNombre("Carlos");
        req.setSegundoNombre("Eduardo");
        req.setApellidoPaterno("Sanchez");
        req.setApellidoMaterno("Ramirez");
        req.setFechaNacimiento(LocalDate.of(1995, 5, 20));
        req.setCurp("SARC950520HDFRMN01");
        req.setRfc("SARC9505201A0");
        req.setSexo("H");
        req.setIdNacionalidad(1L);
        req.setEstadoCivil("SOLTERO");
        req.setCorreoElectronico("carlos.sanchez@test.com");
        req.setTelefonoMovil("5512345678");
        req.setTelefonoAlternativo("5587654321");
        req.setOcupacion("Ingeniero");
        req.setEmpresa("Tech Corp");
        req.setIngresoMensual(BigDecimal.valueOf(35000.0));
        req.setSaldoInicial(BigDecimal.valueOf(1000.0));
        req.setPassword("PasswordSeguro123*");
        req.setBiometricoFacialEmbedding("[0.12, 0.34, 0.56]");
        req.setDomicilio(crearDomicilioDto());
        return req;
    }

    private Cliente crearClienteEntidad() {
        Cliente c = Cliente.builder()
                .idCliente(100L)
                .nombre("Carlos")
                .segundoNombre("Eduardo")
                .apellidoPaterno("Sanchez")
                .apellidoMaterno("Ramirez")
                .fechaNacimiento(LocalDate.of(1995, 5, 20))
                .curp("SARC950520HDFRMN01")
                .rfc("SARC9505201A0")
                .sexo("H")
                .idNacionalidad(1L)
                .nacionalidad("Mexicana")
                .estadoCivil("SOLTERO")
                .correoElectronico("carlos.sanchez@test.com")
                .telefonoMovil("5512345678")
                .telefonoAlternativo("5587654321")
                .ocupacion("Ingeniero")
                .empresa("Tech Corp")
                .ingresoMensual(35000.0)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .fechaModificacion(LocalDateTime.now())
                .domicilios(new ArrayList<>())
                .cuentas(new ArrayList<>())
                .build();

        Domicilio d = Domicilio.builder()
                .idDomicilio(1L)
                .cliente(c)
                .calle("Insurgentes Sur")
                .numeroExterior("123")
                .numeroInterior("4B")
                .colonia("Del Valle")
                .municipio("Benito Juarez")
                .estado("CDMX")
                .codigoPostal("03100")
                .pais("Mexico")
                .build();
        c.getDomicilios().add(d);

        Cuenta cta = Cuenta.builder()
                .idCuenta(50L)
                .cliente(c)
                .numeroCuenta("1234567890123456")
                .tipoCuenta("DEBITO")
                .estatus("ACTIVA")
                .fechaApertura(LocalDateTime.now())
                .build();
        Saldo s = Saldo.builder()
                .idSaldo(500L)
                .cuenta(cta)
                .saldoDisponible(1000.0)
                .saldoContable(1000.0)
                .fechaUltimaActualizacion(LocalDateTime.now())
                .build();
        cta.setSaldo(s);
        c.getCuentas().add(cta);

        return c;
    }

    // ==========================================
    // 1. Pruebas de Registro de Cliente
    // ==========================================

    @Test
    @DisplayName("registrarCliente: registro exitoso de cliente con domicilio, cuenta, saldo y usuario")
    void testRegistrarCliente_Exitoso() {
        ClienteRegistroRequest req = crearRegistroRequest();
        Nacionalidad nac = new Nacionalidad();
        nac.setIdNacionalidad(1L);
        nac.setGentilicio("Mexicana");

        when(nacionalidadRepository.findById(1L)).thenReturn(Optional.of(nac));
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.findByCorreoElectronico(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setIdCliente(100L);
            c.setDomicilios(new ArrayList<>());
            c.setCuentas(new ArrayList<>());
            return c;
        });
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteResponse resp = clienteService.registrarCliente(req);

        assertNotNull(resp);
        assertNull(resp.getIdCliente()); // Enmascarado en la respuesta final
        assertEquals("Carlos", resp.getNombre());
        assertEquals("SARC950520HDFRMN01", resp.getCurp());
        assertEquals("carlos.sanchez@test.com", resp.getCorreoElectronico());
        assertFalse(resp.getCuentas().isEmpty());
        assertNull(resp.getCuentas().get(0).getIdCliente());

        verify(clienteRepository).save(any(Cliente.class));
        verify(domicilioRepository).save(any(Domicilio.class));
        verify(cuentaRepository).save(any(Cuenta.class));
        verify(saldoRepository).save(any(Saldo.class));
        verify(usuarioLoginRepository).save(any(UsuarioLogin.class));
    }

    @Test
    @DisplayName("registrarCliente: fecha de nacimiento nula lanza ReglaNegocioException")
    void testRegistrarCliente_FechaNacimientoNula_LanzaReglaNegocio() {
        ClienteRegistroRequest req = crearRegistroRequest();
        req.setFechaNacimiento(null);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));
        assertTrue(ex.getMessage().contains("La fecha de nacimiento es obligatoria"));
    }

    @Test
    @DisplayName("registrarCliente: menor de edad (<18) lanza ReglaNegocioException")
    void testRegistrarCliente_MenorDeEdad_LanzaReglaNegocio() {
        ClienteRegistroRequest req = crearRegistroRequest();
        req.setFechaNacimiento(LocalDate.now().minusYears(16));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));
        assertTrue(ex.getMessage().contains("El cliente debe ser mayor de edad"));
    }

    @Test
    @DisplayName("registrarCliente: CURP duplicada lanza CurpDuplicadaException")
    void testRegistrarCliente_CurpDuplicada_LanzaCurpDuplicadaException() {
        ClienteRegistroRequest req = crearRegistroRequest();
        when(clienteRepository.existsByCurp(req.getCurp())).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () -> clienteService.registrarCliente(req));
    }

    @Test
    @DisplayName("registrarCliente: RFC duplicado lanza RfcDuplicadoException")
    void testRegistrarCliente_RfcDuplicado_LanzaRfcDuplicadoException() {
        ClienteRegistroRequest req = crearRegistroRequest();
        when(clienteRepository.existsByCurp(req.getCurp())).thenReturn(false);
        when(clienteRepository.existsByRfc(req.getRfc())).thenReturn(true);

        assertThrows(RfcDuplicadoException.class, () -> clienteService.registrarCliente(req));
    }

    @Test
    @DisplayName("registrarCliente: correo electronico duplicado lanza ReglaNegocioException")
    void testRegistrarCliente_CorreoDuplicado_LanzaReglaNegocio() {
        ClienteRegistroRequest req = crearRegistroRequest();
        when(clienteRepository.existsByCurp(req.getCurp())).thenReturn(false);
        when(clienteRepository.existsByRfc(req.getRfc())).thenReturn(false);

        Cliente existente = Cliente.builder().idCliente(999L).build();
        when(clienteRepository.findByCorreoElectronico(req.getCorreoElectronico())).thenReturn(Optional.of(existente));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));
        assertTrue(ex.getMessage().contains("Ya existe un cliente registrado con el correo"));
    }

    @Test
    @DisplayName("registrarCliente: resolucion de nacionalidad por cadena numerica '1'")
    void testRegistrarCliente_NacionalidadPorNumeroId_ResuelveExitoso() {
        ClienteRegistroRequest req = crearRegistroRequest();
        req.setIdNacionalidad(null);
        req.setNacionalidad("1");

        Nacionalidad nac = new Nacionalidad();
        nac.setIdNacionalidad(1L);
        nac.setGentilicio("Mexicana");

        when(nacionalidadRepository.findById(1L)).thenReturn(Optional.of(nac));
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.findByCorreoElectronico(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setDomicilios(new ArrayList<>());
            c.setCuentas(new ArrayList<>());
            return c;
        });

        ClienteResponse resp = clienteService.registrarCliente(req);
        assertNotNull(resp);
    }

    @Test
    @DisplayName("registrarCliente: resolucion de nacionalidad por gentilicio, pais o clave ISO")
    void testRegistrarCliente_NacionalidadPorNombre_ResuelveExitoso() {
        ClienteRegistroRequest req = crearRegistroRequest();
        req.setIdNacionalidad(null);
        req.setNacionalidad("Mexicana");

        Nacionalidad nac = new Nacionalidad();
        nac.setIdNacionalidad(1L);
        nac.setGentilicio("Mexicana");

        // 1. Por gentilicio
        when(nacionalidadRepository.findByGentilicioIgnoreCase("Mexicana")).thenReturn(Optional.of(nac));
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.findByCorreoElectronico(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setDomicilios(new ArrayList<>());
            c.setCuentas(new ArrayList<>());
            return c;
        });

        ClienteResponse resp = clienteService.registrarCliente(req);
        assertNotNull(resp);

        // 2. Por pais (fallback)
        req.setNacionalidad("Mexico");
        when(nacionalidadRepository.findByGentilicioIgnoreCase("Mexico")).thenReturn(Optional.empty());
        when(nacionalidadRepository.findByPaisIgnoreCase("Mexico")).thenReturn(Optional.of(nac));
        assertNotNull(clienteService.registrarCliente(req));

        // 3. Por clave ISO (segundo fallback)
        req.setNacionalidad("MEX");
        when(nacionalidadRepository.findByGentilicioIgnoreCase("MEX")).thenReturn(Optional.empty());
        when(nacionalidadRepository.findByPaisIgnoreCase("MEX")).thenReturn(Optional.empty());
        when(nacionalidadRepository.findByClaveIsoIgnoreCase("MEX")).thenReturn(Optional.of(nac));
        assertNotNull(clienteService.registrarCliente(req));
    }

    @Test
    @DisplayName("registrarCliente: nacionalidad no encontrada o nula lanza ReglaNegocioException")
    void testRegistrarCliente_NacionalidadInvalida_LanzaReglaNegocio() {
        ClienteRegistroRequest req = crearRegistroRequest();
        req.setIdNacionalidad(999L);
        when(nacionalidadRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setIdNacionalidad(null);
        req.setNacionalidad(null);
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));

        req.setNacionalidad("Atlantis");
        when(nacionalidadRepository.findByGentilicioIgnoreCase("Atlantis")).thenReturn(Optional.empty());
        when(nacionalidadRepository.findByPaisIgnoreCase("Atlantis")).thenReturn(Optional.empty());
        when(nacionalidadRepository.findByClaveIsoIgnoreCase("Atlantis")).thenReturn(Optional.empty());
        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(req));
    }

    @Test
    @DisplayName("registrarCliente: saldo inicial nulo o negativo e ingreso mensual nulo se asignan en cero")
    void testRegistrarCliente_SaldosEIngresosNulos_AsignaCero() {
        ClienteRegistroRequest req = crearRegistroRequest();
        req.setSaldoInicial(null);
        req.setIngresoMensual(null);

        Nacionalidad nac = new Nacionalidad();
        nac.setIdNacionalidad(1L);
        nac.setGentilicio("Mexicana");

        when(nacionalidadRepository.findById(1L)).thenReturn(Optional.of(nac));
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.findByCorreoElectronico(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setDomicilios(new ArrayList<>());
            c.setCuentas(new ArrayList<>());
            return c;
        });

        ClienteResponse resp = clienteService.registrarCliente(req);
        assertNotNull(resp);

        ArgumentCaptor<Saldo> saldoCaptor = ArgumentCaptor.forClass(Saldo.class);
        verify(saldoRepository).save(saldoCaptor.capture());
        assertEquals(0.0, saldoCaptor.getValue().getSaldoDisponible());

        ArgumentCaptor<Cliente> clienteCaptor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(clienteCaptor.capture());
        assertEquals(0.0, clienteCaptor.getValue().getIngresoMensual());
    }

    // ==========================================
    // 2. Pruebas de Consultas Básicas
    // ==========================================

    @Test
    @DisplayName("consultas: obtener todos y obtener activos")
    void testConsultas_ObtenerTodosYActivos() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findAll()).thenReturn(List.of(c));
        when(clienteRepository.findByActivoTrue()).thenReturn(List.of(c));

        List<ClienteResponse> todos = clienteService.obtenerTodos();
        assertFalse(todos.isEmpty());

        List<ClienteResponse> activos = clienteService.obtenerClientesActivos();
        assertFalse(activos.isEmpty());
    }

    @Test
    @DisplayName("consultas: obtener por ID existente y no existente")
    void testConsultas_ObtenerPorId() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertNotNull(clienteService.obtenerPorId(100L));
        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.obtenerPorId(999L));
    }

    @Test
    @DisplayName("consultas: obtener por CURP, RFC, Correo y Numero de Cuenta")
    void testConsultas_PorCamposUnicos() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findByCurp("SARC950520HDFRMN01")).thenReturn(Optional.of(c));
        when(clienteRepository.findByCurp("NOEXISTE")).thenReturn(Optional.empty());
        assertNotNull(clienteService.obtenerPorCurp("SARC950520HDFRMN01"));
        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.obtenerPorCurp("NOEXISTE"));

        when(clienteRepository.findByRfc("SARC9505201A0")).thenReturn(Optional.of(c));
        when(clienteRepository.findByRfc("NOEXISTE")).thenReturn(Optional.empty());
        assertNotNull(clienteService.obtenerPorRfc("SARC9505201A0"));
        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.obtenerPorRfc("NOEXISTE"));

        when(clienteRepository.findByCorreoElectronico("carlos.sanchez@test.com")).thenReturn(Optional.of(c));
        when(clienteRepository.findByCorreoElectronico("no@test.com")).thenReturn(Optional.empty());
        assertNotNull(clienteService.obtenerPorCorreo("carlos.sanchez@test.com"));
        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.obtenerPorCorreo("no@test.com"));

        when(clienteRepository.findByCuentasNumeroCuenta("1234567890123456")).thenReturn(Optional.of(c));
        when(clienteRepository.findByCuentasNumeroCuenta("0000")).thenReturn(Optional.empty());
        assertNotNull(clienteService.obtenerPorNumeroCuenta("1234567890123456"));
        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.obtenerPorNumeroCuenta("0000"));
    }

    // ==========================================
    // 3. Pruebas de Rango de Fechas
    // ==========================================

    @Test
    @DisplayName("obtenerPorRangoFechas: consulta correcta y validaciones de fechas obligatorias y orden")
    void testObtenerPorRangoFechas() {
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);
        Cliente c = crearClienteEntidad();

        when(clienteRepository.findByFechaCreacionBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(c));

        List<ClienteResponse> resp = clienteService.obtenerPorRangoFechas(ayer, hoy);
        assertFalse(resp.isEmpty());

        assertThrows(ReglaNegocioException.class, () -> clienteService.obtenerPorRangoFechas(null, hoy));
        assertThrows(ReglaNegocioException.class, () -> clienteService.obtenerPorRangoFechas(hoy, null));
        assertThrows(ReglaNegocioException.class, () -> clienteService.obtenerPorRangoFechas(hoy, ayer)); // inicio > fin
    }

    // ==========================================
    // 4. Pruebas de Búsqueda por Identificador
    // ==========================================

    @Test
    @DisplayName("obtenerPorIdentificador: validacion de cadena vacia y busquedas en cascada")
    void testObtenerPorIdentificador() {
        assertThrows(ReglaNegocioException.class, () -> clienteService.obtenerPorIdentificador("   "));

        Cliente c = crearClienteEntidad();

        // 1. Por CURP
        when(clienteRepository.findByCurpIgnoreCase("CURPTEST")).thenReturn(Optional.of(c));
        assertNotNull(clienteService.obtenerPorIdentificador("CURPTEST"));

        // 2. Por RFC
        when(clienteRepository.findByCurpIgnoreCase("RFCTEST")).thenReturn(Optional.empty());
        when(clienteRepository.findByRfcIgnoreCase("RFCTEST")).thenReturn(Optional.of(c));
        assertNotNull(clienteService.obtenerPorIdentificador("RFCTEST"));

        // 3. Por correo con '@'
        when(clienteRepository.findByCurpIgnoreCase("test@mail.com")).thenReturn(Optional.empty());
        when(clienteRepository.findByRfcIgnoreCase("test@mail.com")).thenReturn(Optional.empty());
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("test@mail.com")).thenReturn(Optional.of(c));
        assertNotNull(clienteService.obtenerPorIdentificador("test@mail.com"));

        // 4. Por numero de cuenta
        when(clienteRepository.findByCurpIgnoreCase("1234567890123456")).thenReturn(Optional.empty());
        when(clienteRepository.findByRfcIgnoreCase("1234567890123456")).thenReturn(Optional.empty());
        when(clienteRepository.findByCuentasNumeroCuenta("1234567890123456")).thenReturn(Optional.of(c));
        assertNotNull(clienteService.obtenerPorIdentificador("1234567890123456"));

        // 5. Fallback por correo sin '@'
        when(clienteRepository.findByCurpIgnoreCase("correoSinArroba")).thenReturn(Optional.empty());
        when(clienteRepository.findByRfcIgnoreCase("correoSinArroba")).thenReturn(Optional.empty());
        when(clienteRepository.findByCuentasNumeroCuenta("correoSinArroba")).thenReturn(Optional.empty());
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("correoSinArroba")).thenReturn(Optional.of(c));
        assertNotNull(clienteService.obtenerPorIdentificador("correoSinArroba"));

        // 6. Por nombre o apellidos
        when(clienteRepository.findByCurpIgnoreCase("Carlos Sanchez")).thenReturn(Optional.empty());
        when(clienteRepository.findByRfcIgnoreCase("Carlos Sanchez")).thenReturn(Optional.empty());
        when(clienteRepository.findByCuentasNumeroCuenta("Carlos Sanchez")).thenReturn(Optional.empty());
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("Carlos Sanchez")).thenReturn(Optional.empty());
        when(clienteRepository.buscarPorNombreOCoincidencia("Carlos Sanchez")).thenReturn(List.of(c));
        assertNotNull(clienteService.obtenerPorIdentificador("Carlos Sanchez"));

        // 7. No encontrado
        when(clienteRepository.buscarPorNombreOCoincidencia("Carlos Sanchez")).thenReturn(List.of());
        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.obtenerPorIdentificador("Carlos Sanchez"));
    }

    // ==========================================
    // 5. Pruebas de Búsqueda Avanzada
    // ==========================================

    @Test
    @DisplayName("busquedaAvanzada: vacia retorna obtenerTodos, con criterios filtra y sin resultados lanza excepcion")
    void testBusquedaAvanzada() {
        Cliente c = crearClienteEntidad();

        // Sin criterios -> obtenerTodos
        when(clienteRepository.findAll()).thenReturn(List.of(c));
        List<ClienteResponse> respVacia = clienteService.busquedaAvanzada(null);
        assertFalse(respVacia.isEmpty());

        ClienteBusquedaAvanzadaRequest reqVacio = new ClienteBusquedaAvanzadaRequest();
        assertFalse(clienteService.busquedaAvanzada(reqVacio).isEmpty());

        // Con criterios
        ClienteBusquedaAvanzadaRequest req = new ClienteBusquedaAvanzadaRequest();
        req.setIdentificador("SARC950520HDFRMN01");
        req.setCurp("SARC");
        req.setRfc("SARC");
        req.setCorreoElectronico("carlos");
        req.setNumeroCuenta("1234");
        req.setActivo(true);
        req.setFechaInicio(LocalDate.now().minusDays(5));
        req.setFechaFin(LocalDate.now().plusDays(1));

        when(clienteRepository.findByCurpIgnoreCase("SARC950520HDFRMN01")).thenReturn(Optional.of(c));
        when(clienteRepository.findByCurpContainingIgnoreCase(anyString())).thenReturn(List.of(c));
        when(clienteRepository.findByRfcContainingIgnoreCase(anyString())).thenReturn(List.of(c));
        when(clienteRepository.findByCorreoElectronicoContainingIgnoreCase(anyString())).thenReturn(List.of(c));
        when(clienteRepository.buscarPorNumeroCuentaContaining(anyString())).thenReturn(List.of(c));
        when(clienteRepository.buscarPorNombreOCoincidencia(anyString())).thenReturn(List.of(c));

        List<ClienteResponse> resultados = clienteService.busquedaAvanzada(req);
        assertFalse(resultados.isEmpty());

        // Solo activo o fechas sin candidatos de texto
        ClienteBusquedaAvanzadaRequest reqSoloActivo = new ClienteBusquedaAvanzadaRequest();
        reqSoloActivo.setActivo(true);
        assertFalse(clienteService.busquedaAvanzada(reqSoloActivo).isEmpty());

        // Sin resultados lanza excepcion
        req.setActivo(false); // c.getActivo() es true
        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.busquedaAvanzada(req));
    }

    // ==========================================
    // 6. Pruebas de Actualización Completa
    // ==========================================

    @Test
    @DisplayName("actualizarCompleto: actualizacion exitosa respetando inmutabilidad de CURP y RFC")
    void testActualizarCompleto_Exitoso() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Nacionalidad nac = new Nacionalidad();
        nac.setIdNacionalidad(1L);
        nac.setGentilicio("Mexicana");
        when(nacionalidadRepository.findById(1L)).thenReturn(Optional.of(nac));

        ClienteRegistroRequest req = crearRegistroRequest();
        req.setNombre("Carlos Modificado");
        req.setCorreoElectronico("nuevo.correo@test.com");
        when(clienteRepository.findByCorreoElectronico("nuevo.correo@test.com")).thenReturn(Optional.empty());

        ClienteResponse resp = clienteService.actualizarCompleto(100L, req);

        assertNotNull(resp);
        assertEquals("Carlos Modificado", resp.getNombre());
        assertEquals("nuevo.correo@test.com", resp.getCorreoElectronico());
        verify(clienteRepository).save(c);
    }

    @Test
    @DisplayName("actualizarCompleto: modificacion de CURP o RFC lanza ReglaNegocioException")
    void testActualizarCompleto_ModificaCurpORfc_LanzaReglaNegocio() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));

        ClienteRegistroRequest reqCurp = crearRegistroRequest();
        reqCurp.setCurp("OTRACURP9999999999");
        ReglaNegocioException exCurp = assertThrows(ReglaNegocioException.class,
                () -> clienteService.actualizarCompleto(100L, reqCurp));
        assertTrue(exCurp.getMessage().contains("No esta permitido modificar la CURP"));

        ClienteRegistroRequest reqRfc = crearRegistroRequest();
        reqRfc.setRfc("OTRORFC999999");
        ReglaNegocioException exRfc = assertThrows(ReglaNegocioException.class,
                () -> clienteService.actualizarCompleto(100L, reqRfc));
        assertTrue(exRfc.getMessage().contains("No esta permitido modificar el RFC"));
    }

    @Test
    @DisplayName("actualizarCompleto: sobrecargas con request e identificador")
    void testActualizarCompleto_Sobrecargas() {
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCompleto((ClienteRegistroRequest) null));

        ClienteRegistroRequest sinId = new ClienteRegistroRequest();
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCompleto(sinId));

        Cliente c = crearClienteEntidad();
        when(clienteRepository.findByCurpIgnoreCase("SARC950520HDFRMN01")).thenReturn(Optional.of(c));
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Nacionalidad nac = new Nacionalidad();
        nac.setIdNacionalidad(1L);
        nac.setGentilicio("Mexicana");
        when(nacionalidadRepository.findById(1L)).thenReturn(Optional.of(nac));

        ClienteRegistroRequest req = crearRegistroRequest();
        req.setIdentificador("SARC950520HDFRMN01");
        assertNotNull(clienteService.actualizarCompleto(req));

        req.setIdentificador(null);
        req.setCurp("SARC950520HDFRMN01");
        assertNotNull(clienteService.actualizarCompleto(req));
    }

    // ==========================================
    // 7. Pruebas de Actualización Parcial (Patch)
    // ==========================================

    @Test
    @DisplayName("actualizarParcial: parche de campos individuales y cascada de estatus a cuentas")
    void testActualizarParcial_Exitoso() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Nacionalidad nac = new Nacionalidad();
        nac.setIdNacionalidad(2L);
        nac.setGentilicio("Espanola");
        when(nacionalidadRepository.findById(2L)).thenReturn(Optional.of(nac));

        ClientePatchRequest patch = new ClientePatchRequest();
        patch.setNombre("Carlos Parche");
        patch.setSegundoNombre("Manuel");
        patch.setApellidoPaterno("SanchezP");
        patch.setApellidoMaterno("RamirezP");
        patch.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        patch.setSexo("M");
        patch.setIdNacionalidad(2L);
        patch.setEstadoCivil("CASADO");
        patch.setTelefonoMovil("5599998888");
        patch.setTelefonoAlternativo("5577776666");
        patch.setOcupacion("Director");
        patch.setEmpresa("Global Corp");
        patch.setIngresoMensual(BigDecimal.valueOf(80000.0));
        patch.setActivo(false); // Desactiva cuentas
        patch.setDomicilio(crearDomicilioDto());

        ClienteResponse resp = clienteService.actualizarParcial(100L, patch);

        assertNotNull(resp);
        assertEquals("Carlos Parche", resp.getNombre());
        assertFalse(resp.getActivo());
        assertEquals("INACTIVA", c.getCuentas().get(0).getEstatus());

        // Reactivar cuentas via patch activo=true
        patch.setActivo(true);
        clienteService.actualizarParcial(100L, patch);
        assertTrue(c.getActivo());
        assertEquals("ACTIVA", c.getCuentas().get(0).getEstatus());
    }

    @Test
    @DisplayName("actualizarParcial: sobrecargas con request e identificador")
    void testActualizarParcial_Sobrecargas() {
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarParcial((ClientePatchRequest) null));

        ClientePatchRequest sinId = new ClientePatchRequest();
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarParcial(sinId));

        Cliente c = crearClienteEntidad();
        when(clienteRepository.findByCurpIgnoreCase("SARC950520HDFRMN01")).thenReturn(Optional.of(c));
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientePatchRequest req = new ClientePatchRequest();
        req.setIdentificador("SARC950520HDFRMN01");
        req.setNombre("Nuevo Nombre");
        assertNotNull(clienteService.actualizarParcial(req));

        req.setIdentificador(null);
        req.setCorreoElectronico("carlos.sanchez@test.com");
        when(clienteRepository.findByCorreoElectronicoIgnoreCase("carlos.sanchez@test.com")).thenReturn(Optional.of(c));
        assertNotNull(clienteService.actualizarParcial(req));
    }

    // ==========================================
    // 8. Pruebas de Desactivación y Reactivación
    // ==========================================

    @Test
    @DisplayName("desactivarCliente y reactivarCliente: actualiza estado de cliente y cuentas")
    void testDesactivarYReactivarCliente() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Desactivar
        clienteService.desactivarCliente(100L);
        assertFalse(c.getActivo());
        assertEquals("INACTIVA", c.getCuentas().get(0).getEstatus());

        // 2. Reactivar
        ClienteResponse respReactivado = clienteService.reactivarCliente(100L);
        assertNotNull(respReactivado);
        assertTrue(c.getActivo());
        assertEquals("ACTIVA", c.getCuentas().get(0).getEstatus());

        // 3. No encontrado
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.desactivarCliente(999L));
        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.reactivarCliente(999L));
    }

    @Test
    @DisplayName("desactivarPorIdentificador y reactivarPorIdentificador: delegacion exitosa")
    void testDesactivarYReactivarPorIdentificador() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findByCurpIgnoreCase("SARC950520HDFRMN01")).thenReturn(Optional.of(c));
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteResponse respDesactivado = clienteService.desactivarClientePorIdentificador("SARC950520HDFRMN01");
        assertNotNull(respDesactivado);
        assertNull(respDesactivado.getIdCliente());

        ClienteResponse respReactivado = clienteService.reactivarClientePorIdentificador("SARC950520HDFRMN01");
        assertNotNull(respReactivado);
        assertNull(respReactivado.getIdCliente());
    }

    // ==========================================
    // 9. Pruebas de Ramas Adicionales (Branch Hardening)
    // ==========================================

    @Test
    @DisplayName("tieneCriteriosBusqueda y poblarCandidatosBusqueda: ramas booleanas individuales")
    void testTieneCriteriosBusqueda_RamasIndividuales() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findAll()).thenReturn(List.of(c));

        // Request con identificador vacio/blanco
        ClienteBusquedaAvanzadaRequest rIdVacio = new ClienteBusquedaAvanzadaRequest();
        rIdVacio.setIdentificador("   ");
        assertFalse(clienteService.busquedaAvanzada(rIdVacio).isEmpty());

        // Request con solo curp
        ClienteBusquedaAvanzadaRequest rCurp = new ClienteBusquedaAvanzadaRequest();
        rCurp.setCurp("SARC");
        when(clienteRepository.findByCurpContainingIgnoreCase("SARC")).thenReturn(List.of(c));
        assertFalse(clienteService.busquedaAvanzada(rCurp).isEmpty());

        // Request con curp vacia
        ClienteBusquedaAvanzadaRequest rCurpVacia = new ClienteBusquedaAvanzadaRequest();
        rCurpVacia.setCurp("   ");
        assertFalse(clienteService.busquedaAvanzada(rCurpVacia).isEmpty());

        // Request con solo rfc
        ClienteBusquedaAvanzadaRequest rRfc = new ClienteBusquedaAvanzadaRequest();
        rRfc.setRfc("SARC");
        when(clienteRepository.findByRfcContainingIgnoreCase("SARC")).thenReturn(List.of(c));
        assertFalse(clienteService.busquedaAvanzada(rRfc).isEmpty());

        // Request con rfc vacio
        ClienteBusquedaAvanzadaRequest rRfcVacio = new ClienteBusquedaAvanzadaRequest();
        rRfcVacio.setRfc("   ");
        assertFalse(clienteService.busquedaAvanzada(rRfcVacio).isEmpty());

        // Request con solo correo
        ClienteBusquedaAvanzadaRequest rCorreo = new ClienteBusquedaAvanzadaRequest();
        rCorreo.setCorreoElectronico("carlos");
        when(clienteRepository.findByCorreoElectronicoContainingIgnoreCase("carlos")).thenReturn(List.of(c));
        assertFalse(clienteService.busquedaAvanzada(rCorreo).isEmpty());

        // Request con correo vacio
        ClienteBusquedaAvanzadaRequest rCorreoVacio = new ClienteBusquedaAvanzadaRequest();
        rCorreoVacio.setCorreoElectronico("   ");
        assertFalse(clienteService.busquedaAvanzada(rCorreoVacio).isEmpty());

        // Request con solo numeroCuenta
        ClienteBusquedaAvanzadaRequest rCta = new ClienteBusquedaAvanzadaRequest();
        rCta.setNumeroCuenta("1234");
        when(clienteRepository.buscarPorNumeroCuentaContaining("1234")).thenReturn(List.of(c));
        assertFalse(clienteService.busquedaAvanzada(rCta).isEmpty());

        // Request con numeroCuenta vacio
        ClienteBusquedaAvanzadaRequest rCtaVacia = new ClienteBusquedaAvanzadaRequest();
        rCtaVacia.setNumeroCuenta("   ");
        assertFalse(clienteService.busquedaAvanzada(rCtaVacia).isEmpty());

        // Request con fechaInicio presente pero fechaFin nula (y viceversa)
        ClienteBusquedaAvanzadaRequest rFecha1 = new ClienteBusquedaAvanzadaRequest();
        rFecha1.setFechaInicio(LocalDate.now());
        assertFalse(clienteService.busquedaAvanzada(rFecha1).isEmpty());

        ClienteBusquedaAvanzadaRequest rFecha2 = new ClienteBusquedaAvanzadaRequest();
        rFecha2.setFechaFin(LocalDate.now());
        assertFalse(clienteService.busquedaAvanzada(rFecha2).isEmpty());

        // Request con fechas donde cliente tiene fechaCreacion nula
        Cliente cSinFecha = crearClienteEntidad();
        cSinFecha.setFechaCreacion(null);
        when(clienteRepository.findAll()).thenReturn(List.of(cSinFecha));
        ClienteBusquedaAvanzadaRequest rFechas = new ClienteBusquedaAvanzadaRequest();
        rFechas.setFechaInicio(LocalDate.now().minusDays(1));
        rFechas.setFechaFin(LocalDate.now().plusDays(1));
        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.busquedaAvanzada(rFechas));
    }

    @Test
    @DisplayName("actualizarParcial: ramas de calles y ubicacion en domicilio con campos nulos y no nulos")
    void testActualizarParcial_RamasDomicilio() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DomicilioDto dDto = DomicilioDto.builder()
                .calle("Calle Nueva")
                .numeroExterior("999")
                .numeroInterior("1A")
                .colonia("Colonia Nueva")
                .municipio("Alcaldia")
                .estado("Estado")
                .codigoPostal("12345")
                .pais("Mexico")
                .build();
        ClientePatchRequest patch = new ClientePatchRequest();
        patch.setDomicilio(dDto);
        assertNotNull(clienteService.actualizarParcial(100L, patch));

        patch.setDomicilio(null);
        assertNotNull(clienteService.actualizarParcial(100L, patch));

        c.getDomicilios().clear();
        patch.setDomicilio(dDto);
        assertNotNull(clienteService.actualizarParcial(100L, patch));
    }

    @Test
    @DisplayName("actualizarContactoYEmpleoPatch: ramas de correo igual vs modificado con unicidad")
    void testActualizarParcial_ContactoYEmpleoRamas() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientePatchRequest patch = new ClientePatchRequest();
        patch.setCorreoElectronico(c.getCorreoElectronico());
        patch.setTelefonoMovil("5511223344");
        patch.setTelefonoAlternativo("5599887766");
        patch.setOcupacion("Profesor");
        patch.setEmpresa("Universidad");
        patch.setIngresoMensual(BigDecimal.valueOf(40000.0));
        assertNotNull(clienteService.actualizarParcial(100L, patch));

        patch.setCorreoElectronico("otro@test.com");
        when(clienteRepository.findByCorreoElectronico("otro@test.com")).thenReturn(Optional.of(c));
        assertNotNull(clienteService.actualizarParcial(100L, patch));
    }

    @Test
    @DisplayName("actualizarNombresPatch e IdentidadPatch: ramas individuales")
    void testActualizarParcial_NombresEIdentidadRamas() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Nacionalidad nac = new Nacionalidad();
        nac.setIdNacionalidad(1L);
        nac.setGentilicio("Mexicana");
        when(nacionalidadRepository.findByGentilicioIgnoreCase("Mexicana")).thenReturn(Optional.of(nac));

        ClientePatchRequest patch = new ClientePatchRequest();
        patch.setNombre("SoloNombre");
        patch.setSegundoNombre("SoloSegundo");
        patch.setApellidoPaterno("SoloPaterno");
        patch.setApellidoMaterno("SoloMaterno");
        patch.setSexo("H");
        patch.setEstadoCivil("CASADO");
        patch.setIdNacionalidad(null);
        patch.setNacionalidad("Mexicana");
        assertNotNull(clienteService.actualizarParcial(100L, patch));
    }

    @Test
    @DisplayName("actualizarCompleto: ramas con domicilio nulo y cliente sin domicilios")
    void testActualizarCompleto_RamasDomicilio() {
        Cliente c = crearClienteEntidad();
        when(clienteRepository.findById(100L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Nacionalidad nac = new Nacionalidad();
        nac.setIdNacionalidad(1L);
        nac.setGentilicio("Mexicana");
        when(nacionalidadRepository.findById(1L)).thenReturn(Optional.of(nac));

        ClienteRegistroRequest req = crearRegistroRequest();
        req.setDomicilio(null);
        req.setSegundoNombre(null);
        req.setTelefonoAlternativo(null);
        assertNotNull(clienteService.actualizarCompleto(100L, req));

        c.getDomicilios().clear();
        req.setDomicilio(crearDomicilioDto());
        assertNotNull(clienteService.actualizarCompleto(100L, req));

        req.setIdentificador("SARC950520HDFRMN01");
        req.setCurp("SARC950520HDFRMN01");
        req.setRfc("SARC9505201A0");
        when(clienteRepository.findByCurpIgnoreCase("SARC950520HDFRMN01")).thenReturn(Optional.of(c));
        assertNotNull(clienteService.actualizarCompleto(req));

        req.setIdentificador(null);
        req.setCurp(null);
        req.setRfc(null);
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCompleto(req));

        req.setCurp("");
        req.setRfc("SARC9505201A0");
        when(clienteRepository.findByRfcIgnoreCase("SARC9505201A0")).thenReturn(Optional.of(c));
        assertThrows(ReglaNegocioException.class, () -> clienteService.actualizarCompleto(req));
    }

    @Test
    @DisplayName("registrarCliente: ramas de segundoNombre y telefonoAlternativo nulos")
    void testRegistrarCliente_CamposOpcionalesNulos() {
        ClienteRegistroRequest req = crearRegistroRequest();
        req.setSegundoNombre(null);
        req.setTelefonoAlternativo(null);
        req.getDomicilio().setNumeroInterior(null);

        Nacionalidad nac = new Nacionalidad();
        nac.setIdNacionalidad(1L);
        nac.setGentilicio("Mexicana");

        when(nacionalidadRepository.findById(1L)).thenReturn(Optional.of(nac));
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.findByCorreoElectronico(anyString())).thenReturn(Optional.empty());
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setDomicilios(new ArrayList<>());
            c.setCuentas(new ArrayList<>());
            return c;
        });

        ClienteResponse resp = clienteService.registrarCliente(req);
        assertNotNull(resp);
        assertNull(resp.getSegundoNombre());
        assertNull(resp.getTelefonoAlternativo());
    }
}
