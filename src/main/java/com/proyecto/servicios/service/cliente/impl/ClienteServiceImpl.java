package com.proyecto.servicios.service.cliente.impl;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.cliente.Saldo;
import com.proyecto.servicios.entity.cliente.UsuarioLogin;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.model.cliente.ClientePatchRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.cliente.DomicilioDto;
import com.proyecto.servicios.model.cliente.SaldoResponse;
import com.proyecto.servicios.entity.cliente.Nacionalidad;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.cliente.NacionalidadRepository;
import com.proyecto.servicios.repositorys.cliente.SaldoRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioLoginRepository;
import com.proyecto.servicios.service.cliente.ClienteService;
import com.proyecto.servicios.service.cliente.util.PasswordEncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final CuentaRepository cuentaRepository;
    private final SaldoRepository saldoRepository;
    private final UsuarioLoginRepository usuarioLoginRepository;
    private final NacionalidadRepository nacionalidadRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public ClienteResponse registrarCliente(ClienteRegistroRequest request) {
        log.info("[Onboarding]: Iniciando registro para CURP: {}", request.getCurp());

        // 1. Validar Mayoría de Edad (18+)
        validarMayoriaDeEdad(request.getFechaNacimiento());

        // 2. Validar Unicidad de CURP, RFC y Correo
        validarUnicidad(request.getCurp(), request.getRfc(), request.getCorreoElectronico(), null);

        // 3. Crear y Persistir Cliente
        Nacionalidad nac = resolverNacionalidad(request.getIdNacionalidad(), request.getNacionalidad());
        Double ingreso = (request.getIngresoMensual() != null) ? request.getIngresoMensual().doubleValue() : 0.0;
        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre().trim())
                .segundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null)
                .apellidoPaterno(request.getApellidoPaterno().trim())
                .apellidoMaterno(request.getApellidoMaterno().trim())
                .fechaNacimiento(request.getFechaNacimiento())
                .curp(request.getCurp().trim().toUpperCase())
                .rfc(request.getRfc().trim().toUpperCase())
                .sexo(request.getSexo().trim().toUpperCase())
                .idNacionalidad(nac.getIdNacionalidad())
                .nacionalidad(nac.getGentilicio())
                .estadoCivil(request.getEstadoCivil().trim())
                .correoElectronico(request.getCorreoElectronico().trim().toLowerCase())
                .telefonoMovil(request.getTelefonoMovil().trim())
                .telefonoAlternativo(request.getTelefonoAlternativo() != null ? request.getTelefonoAlternativo().trim() : null)
                .ocupacion(request.getOcupacion().trim())
                .empresa(request.getEmpresa().trim())
                .ingresoMensual(ingreso)
                .activo(true)
                .build();

        Cliente clienteGuardado = clienteRepository.save(cliente);

        // 4. Crear Domicilio
        DomicilioDto domDto = request.getDomicilio();
        Domicilio domicilio = Domicilio.builder()
                .cliente(clienteGuardado)
                .calle(domDto.getCalle().trim())
                .numeroExterior(domDto.getNumeroExterior().trim())
                .numeroInterior(domDto.getNumeroInterior() != null ? domDto.getNumeroInterior().trim() : null)
                .colonia(domDto.getColonia().trim())
                .municipio(domDto.getMunicipio().trim())
                .estado(domDto.getEstado().trim())
                .codigoPostal(domDto.getCodigoPostal().trim())
                .pais(domDto.getPais().trim())
                .build();
        domicilioRepository.save(domicilio);
        clienteGuardado.getDomicilios().add(domicilio);

        // 5. Creación Automática de Cuenta Bancaria
        String numeroCuenta = generarNumeroCuentaUnico();
        Cuenta cuenta = Cuenta.builder()
                .cliente(clienteGuardado)
                .numeroCuenta(numeroCuenta)
                .tipoCuenta("DEBITO")
                .estatus("ACTIVA")
                .build();
        Cuenta cuentaGuardada = cuentaRepository.save(cuenta);

        // 6. Asignar Saldo Inicial
        Double saldoInicial = (request.getSaldoInicial() != null && request.getSaldoInicial().doubleValue() >= 0.0) 
                ? request.getSaldoInicial().doubleValue() : 0.0;
        Saldo saldo = Saldo.builder()
                .cuenta(cuentaGuardada)
                .saldoDisponible(saldoInicial)
                .saldoContable(saldoInicial)
                .build();
        saldoRepository.save(saldo);
        cuentaGuardada.setSaldo(saldo);
        clienteGuardado.getCuentas().add(cuentaGuardada);

        // 7. Crear Usuario de Acceso (Password cifrado + Embedding MediaPipe)
        String hashPassword = PasswordEncryptionUtil.cifrarPassword(request.getPassword());
        UsuarioLogin usuario = UsuarioLogin.builder()
                .cliente(clienteGuardado)
                .username(request.getCorreoElectronico().trim().toLowerCase())
                .passwordHash(hashPassword)
                .biometricoFacialEmbedding(request.getBiometricoFacialEmbedding())
                .activo(true)
                .build();
        usuarioLoginRepository.save(usuario);
        clienteGuardado.setUsuarioLogin(usuario);

        log.info("[Onboarding]: Exitoso para cliente ID: {}, Cuenta asignada: {}", clienteGuardado.getIdCliente(), numeroCuenta);
        return mapearAClienteResponse(clienteGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerTodos() {
        return clienteRepository.findAll().stream()
                .map(this::mapearAClienteResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontro ningun cliente con el ID: " + id));
        return mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorCurp(String curp) {
        Cliente cliente = clienteRepository.findByCurp(curp.trim().toUpperCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontro cliente con la CURP: " + curp));
        return mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc.trim().toUpperCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontro cliente con el RFC: " + rfc));
        return mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreoElectronico(correo.trim().toLowerCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("El correo electronico no existe: " + correo));
        return mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        Cliente cliente = clienteRepository.findByCuentasNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontro cliente asociado a la cuenta: " + numeroCuenta));
        return mapearAClienteResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerClientesActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(this::mapearAClienteResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerPorRangoFechas(LocalDate inicio, LocalDate fin) {
        LocalDateTime desde = inicio.atStartOfDay();
        LocalDateTime hasta = fin.atTime(LocalTime.MAX);
        return clienteRepository.findByFechaCreacionBetween(desde, hasta).stream()
                .map(this::mapearAClienteResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCompleto(Long id, ClienteRegistroRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + id));

        // Regla: CURP y RFC no se pueden modificar
        if (!cliente.getCurp().equalsIgnoreCase(request.getCurp().trim())) {
            throw new ReglaNegocioException("No esta permitido modificar la CURP del cliente");
        }
        if (!cliente.getRfc().equalsIgnoreCase(request.getRfc().trim())) {
            throw new ReglaNegocioException("No esta permitido modificar el RFC del cliente");
        }

        // Validar unicidad de correo si cambió
        if (!cliente.getCorreoElectronico().equalsIgnoreCase(request.getCorreoElectronico().trim())) {
            validarUnicidad(null, null, request.getCorreoElectronico(), id);
            cliente.setCorreoElectronico(request.getCorreoElectronico().trim().toLowerCase());
        }

        validarMayoriaDeEdad(request.getFechaNacimiento());

        cliente.setNombre(request.getNombre().trim());
        cliente.setSegundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null);
        cliente.setApellidoPaterno(request.getApellidoPaterno().trim());
        cliente.setApellidoMaterno(request.getApellidoMaterno().trim());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setSexo(request.getSexo().trim().toUpperCase());
        Nacionalidad nacActualizada = resolverNacionalidad(request.getIdNacionalidad(), request.getNacionalidad());
        cliente.setIdNacionalidad(nacActualizada.getIdNacionalidad());
        cliente.setNacionalidad(nacActualizada.getGentilicio());
        cliente.setEstadoCivil(request.getEstadoCivil().trim());
        cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo() != null ? request.getTelefonoAlternativo().trim() : null);
        cliente.setOcupacion(request.getOcupacion().trim());
        cliente.setEmpresa(request.getEmpresa().trim());
        cliente.setIngresoMensual(request.getIngresoMensual().doubleValue());

        // Actualizar Domicilio
        if (request.getDomicilio() != null && !cliente.getDomicilios().isEmpty()) {
            Domicilio dom = cliente.getDomicilios().get(0);
            DomicilioDto dDto = request.getDomicilio();
            dom.setCalle(dDto.getCalle().trim());
            dom.setNumeroExterior(dDto.getNumeroExterior().trim());
            dom.setNumeroInterior(dDto.getNumeroInterior() != null ? dDto.getNumeroInterior().trim() : null);
            dom.setColonia(dDto.getColonia().trim());
            dom.setMunicipio(dDto.getMunicipio().trim());
            dom.setEstado(dDto.getEstado().trim());
            dom.setCodigoPostal(dDto.getCodigoPostal().trim());
            dom.setPais(dDto.getPais().trim());
        }

        return mapearAClienteResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteResponse actualizarParcial(Long id, ClientePatchRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + id));

        if (request.getNombre() != null) cliente.setNombre(request.getNombre().trim());
        if (request.getSegundoNombre() != null) cliente.setSegundoNombre(request.getSegundoNombre().trim());
        if (request.getApellidoPaterno() != null) cliente.setApellidoPaterno(request.getApellidoPaterno().trim());
        if (request.getApellidoMaterno() != null) cliente.setApellidoMaterno(request.getApellidoMaterno().trim());
        if (request.getFechaNacimiento() != null) {
            validarMayoriaDeEdad(request.getFechaNacimiento());
            cliente.setFechaNacimiento(request.getFechaNacimiento());
        }
        if (request.getSexo() != null) cliente.setSexo(request.getSexo().trim().toUpperCase());
        if (request.getIdNacionalidad() != null || (request.getNacionalidad() != null && !request.getNacionalidad().trim().isEmpty())) {
            Nacionalidad nacPatch = resolverNacionalidad(request.getIdNacionalidad(), request.getNacionalidad());
            cliente.setIdNacionalidad(nacPatch.getIdNacionalidad());
            cliente.setNacionalidad(nacPatch.getGentilicio());
        }
        if (request.getEstadoCivil() != null) cliente.setEstadoCivil(request.getEstadoCivil().trim());

        if (request.getCorreoElectronico() != null && !request.getCorreoElectronico().equalsIgnoreCase(cliente.getCorreoElectronico())) {
            validarUnicidad(null, null, request.getCorreoElectronico(), id);
            cliente.setCorreoElectronico(request.getCorreoElectronico().trim().toLowerCase());
        }

        if (request.getTelefonoMovil() != null) cliente.setTelefonoMovil(request.getTelefonoMovil().trim());
        if (request.getTelefonoAlternativo() != null) cliente.setTelefonoAlternativo(request.getTelefonoAlternativo().trim());
        if (request.getOcupacion() != null) cliente.setOcupacion(request.getOcupacion().trim());
        if (request.getEmpresa() != null) cliente.setEmpresa(request.getEmpresa().trim());
        if (request.getIngresoMensual() != null) cliente.setIngresoMensual(request.getIngresoMensual().doubleValue());

        if (request.getActivo() != null) {
            cliente.setActivo(request.getActivo());
            // Si se desactiva, suspender cuentas asociadas
            if (Boolean.FALSE.equals(request.getActivo())) {
                cliente.getCuentas().forEach(c -> c.setEstatus("INACTIVA"));
            }
        }

        if (request.getDomicilio() != null && !cliente.getDomicilios().isEmpty()) {
            Domicilio dom = cliente.getDomicilios().get(0);
            DomicilioDto dDto = request.getDomicilio();
            if (dDto.getCalle() != null) dom.setCalle(dDto.getCalle().trim());
            if (dDto.getNumeroExterior() != null) dom.setNumeroExterior(dDto.getNumeroExterior().trim());
            if (dDto.getNumeroInterior() != null) dom.setNumeroInterior(dDto.getNumeroInterior().trim());
            if (dDto.getColonia() != null) dom.setColonia(dDto.getColonia().trim());
            if (dDto.getMunicipio() != null) dom.setMunicipio(dDto.getMunicipio().trim());
            if (dDto.getEstado() != null) dom.setEstado(dDto.getEstado().trim());
            if (dDto.getCodigoPostal() != null) dom.setCodigoPostal(dDto.getCodigoPostal().trim());
            if (dDto.getPais() != null) dom.setPais(dDto.getPais().trim());
        }

        return mapearAClienteResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public void desactivarCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontro el cliente con ID: " + id));

        // Baja lógica
        cliente.setActivo(false);
        // Regla: Solo clientes activos pueden tener cuentas activas
        cliente.getCuentas().forEach(cuenta -> cuenta.setEstatus("INACTIVA"));
        clienteRepository.save(cliente);
        log.info("[BajaLogica]: Cliente con ID {} y sus cuentas han sido desactivados", id);
    }

    @Override
    @Transactional
    public ClienteResponse reactivarCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontro el cliente con ID: " + id));

        cliente.setActivo(true);
        cliente.getCuentas().forEach(cuenta -> cuenta.setEstatus("ACTIVA"));
        return mapearAClienteResponse(clienteRepository.save(cliente));
    }

    // --- Métodos Auxiliares y Validaciones ---

    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ReglaNegocioException("La fecha de nacimiento es obligatoria");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            throw new ReglaNegocioException("El cliente debe ser mayor de edad (18 anos o mas). Edad actual calculada: " + edad);
        }
    }

    private void validarUnicidad(String curp, String rfc, String correo, Long idActual) {
        if (curp != null && clienteRepository.existsByCurp(curp.trim().toUpperCase())) {
            throw new CurpDuplicadaException(curp);
        }
        if (rfc != null && clienteRepository.existsByRfc(rfc.trim().toUpperCase())) {
            throw new RfcDuplicadoException(rfc);
        }
        if (correo != null) {
            clienteRepository.findByCorreoElectronico(correo.trim().toLowerCase()).ifPresent(c -> {
                if (idActual == null || !c.getIdCliente().equals(idActual)) {
                    throw new ReglaNegocioException("Ya existe un cliente registrado con el correo: " + correo);
                }
            });
        }
    }

    private String generarNumeroCuentaUnico() {
        String numCuenta;
        do {
            // Genera número de 10 dígitos (ej. 1000000000 a 9999999999)
            long numero = 1000000000L + (long) (RANDOM.nextDouble() * 8999999999L);
            numCuenta = String.valueOf(numero);
        } while (cuentaRepository.existsByNumeroCuenta(numCuenta));
        return numCuenta;
    }

    private ClienteResponse mapearAClienteResponse(Cliente c) {
        String nombreCompleto = c.getNombre() 
                + (c.getSegundoNombre() != null ? " " + c.getSegundoNombre() : "")
                + " " + c.getApellidoPaterno() + " " + c.getApellidoMaterno();

        int edad = (c.getFechaNacimiento() != null) ? Period.between(c.getFechaNacimiento(), LocalDate.now()).getYears() : 0;

        List<DomicilioDto> domDtos = c.getDomicilios().stream().map(d -> DomicilioDto.builder()
                .calle(d.getCalle())
                .numeroExterior(d.getNumeroExterior())
                .numeroInterior(d.getNumeroInterior())
                .colonia(d.getColonia())
                .municipio(d.getMunicipio())
                .estado(d.getEstado())
                .codigoPostal(d.getCodigoPostal())
                .pais(d.getPais())
                .build()).collect(Collectors.toList());

        List<CuentaResponse> cuentaResponses = c.getCuentas().stream().map(cta -> {
            SaldoResponse sResp = null;
            if (cta.getSaldo() != null) {
                sResp = SaldoResponse.builder()
                        .idSaldo(cta.getSaldo().getIdSaldo())
                        .numeroCuenta(cta.getNumeroCuenta())
                        .saldoDisponible(cta.getSaldo().getSaldoDisponible())
                        .saldoContable(cta.getSaldo().getSaldoContable())
                        .fechaUltimaActualizacion(cta.getSaldo().getFechaUltimaActualizacion())
                        .build();
            }
            return CuentaResponse.builder()
                    .idCuenta(cta.getIdCuenta())
                    .idCliente(c.getIdCliente())
                    .nombreTitular(nombreCompleto)
                    .numeroCuenta(cta.getNumeroCuenta())
                    .tipoCuenta(cta.getTipoCuenta())
                    .estatus(cta.getEstatus())
                    .fechaApertura(cta.getFechaApertura())
                    .saldo(sResp)
                    .build();
        }).collect(Collectors.toList());

        return ClienteResponse.builder()
                .idCliente(c.getIdCliente())
                .nombre(c.getNombre())
                .segundoNombre(c.getSegundoNombre())
                .apellidoPaterno(c.getApellidoPaterno())
                .apellidoMaterno(c.getApellidoMaterno())
                .nombreCompleto(nombreCompleto)
                .fechaNacimiento(c.getFechaNacimiento())
                .edad(edad)
                .curp(c.getCurp())
                .rfc(c.getRfc())
                .sexo(c.getSexo())
                .idNacionalidad(c.getIdNacionalidad())
                .nacionalidad(c.getNacionalidad())
                .estadoCivil(c.getEstadoCivil())
                .correoElectronico(c.getCorreoElectronico())
                .telefonoMovil(c.getTelefonoMovil())
                .telefonoAlternativo(c.getTelefonoAlternativo())
                .ocupacion(c.getOcupacion())
                .empresa(c.getEmpresa())
                .ingresoMensual(c.getIngresoMensual())
                .activo(c.getActivo())
                .fechaCreacion(c.getFechaCreacion())
                .fechaModificacion(c.getFechaModificacion())
                .domicilios(domDtos)
                .cuentas(cuentaResponses)
                .build();
    }

    private Nacionalidad resolverNacionalidad(Long idNacionalidad, String nombreNacionalidad) {
        if (idNacionalidad != null) {
            return nacionalidadRepository.findById(idNacionalidad)
                    .orElseThrow(() -> new ReglaNegocioException("La nacionalidad con ID " + idNacionalidad + " no existe en el catalogo de la base de datos"));
        }
        if (nombreNacionalidad != null && !nombreNacionalidad.trim().isEmpty()) {
            String nombreLimpio = nombreNacionalidad.trim();
            if (nombreLimpio.matches("^\\d+$")) {
                Long id = Long.parseLong(nombreLimpio);
                return nacionalidadRepository.findById(id)
                        .orElseThrow(() -> new ReglaNegocioException("La nacionalidad con ID " + id + " no existe en el catalogo de la base de datos"));
            }
            return nacionalidadRepository.findByGentilicioIgnoreCase(nombreLimpio)
                    .or(() -> nacionalidadRepository.findByPaisIgnoreCase(nombreLimpio))
                    .or(() -> nacionalidadRepository.findByClaveIsoIgnoreCase(nombreLimpio))
                    .orElseThrow(() -> new ReglaNegocioException("La nacionalidad '" + nombreLimpio + "' no existe en el catalogo de la base de datos"));
        }
        throw new ReglaNegocioException("La nacionalidad es obligatoria. Debe proporcionar idNacionalidad o nombre de la nacionalidad del catalogo");
    }
}
