package com.proyecto.servicios.service.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Saldo;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.cliente.SaldoResponse;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.SaldoRepository;
import com.proyecto.servicios.service.cliente.impl.CuentaServiceImpl;

@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private SaldoRepository saldoRepository;

    private CuentaServiceImpl cuentaService;

    @BeforeEach
    void setUp() {
        cuentaService = new CuentaServiceImpl(cuentaRepository, saldoRepository);
    }

    @Test
    @DisplayName("obtenerPorNumeroCuenta: cuenta encontrada con titular y saldo completo")
    void testObtenerPorNumeroCuenta_ExitosoConClienteYSaldo() {
        String numCuenta = "1000000001000000";
        LocalDateTime ahora = LocalDateTime.now();

        Cliente cliente = Cliente.builder()
                .idCliente(10L)
                .nombre("Juan")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Gomez")
                .build();

        Cuenta cuenta = Cuenta.builder()
                .idCuenta(1L)
                .numeroCuenta(numCuenta)
                .tipoCuenta("DEBITO")
                .estatus("ACTIVA")
                .fechaApertura(ahora.minusDays(30))
                .cliente(cliente)
                .build();

        Saldo saldo = Saldo.builder()
                .idSaldo(5L)
                .cuenta(cuenta)
                .saldoDisponible(1500.50)
                .saldoContable(1500.50)
                .fechaUltimaActualizacion(ahora)
                .build();
        cuenta.setSaldo(saldo);

        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));

        CuentaResponse resp = cuentaService.obtenerPorNumeroCuenta(numCuenta);

        assertNotNull(resp);
        assertEquals(1L, resp.getIdCuenta());
        assertEquals(10L, resp.getIdCliente());
        assertEquals("Juan Perez Gomez", resp.getNombreTitular());
        assertEquals(numCuenta, resp.getNumeroCuenta());
        assertEquals("DEBITO", resp.getTipoCuenta());
        assertEquals("ACTIVA", resp.getEstatus());
        assertNotNull(resp.getSaldo());
        assertEquals(5L, resp.getSaldo().getIdSaldo());
        assertEquals(1500.50, resp.getSaldo().getSaldoDisponible());
        assertEquals(1500.50, resp.getSaldo().getSaldoContable());
        verify(cuentaRepository).findByNumeroCuenta(numCuenta);
    }

    @Test
    @DisplayName("obtenerPorNumeroCuenta: cuenta encontrada sin cliente ni saldo mapea campos nulos o vacios")
    void testObtenerPorNumeroCuenta_ExitosoSinClienteNiSaldo() {
        String numCuenta = "2000000002000000";
        Cuenta cuenta = Cuenta.builder()
                .idCuenta(2L)
                .numeroCuenta(numCuenta)
                .tipoCuenta("AHORRO")
                .estatus("ACTIVA")
                .cliente(null)
                .saldo(null)
                .build();

        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));

        CuentaResponse resp = cuentaService.obtenerPorNumeroCuenta(numCuenta);

        assertNotNull(resp);
        assertNull(resp.getIdCliente());
        assertEquals("", resp.getNombreTitular());
        assertNull(resp.getSaldo());
    }

    @Test
    @DisplayName("obtenerPorNumeroCuenta: cuenta encontrada con cliente pero sin saldo")
    void testObtenerPorNumeroCuenta_ExitosoConClienteSinSaldo() {
        String numCuenta = "3000000003000000";
        Cliente cliente = Cliente.builder()
                .idCliente(20L)
                .nombre("Maria")
                .apellidoPaterno("Lopez")
                .apellidoMaterno("Sanchez")
                .build();

        Cuenta cuenta = Cuenta.builder()
                .idCuenta(3L)
                .numeroCuenta(numCuenta)
                .tipoCuenta("DEBITO")
                .estatus("ACTIVA")
                .cliente(cliente)
                .saldo(null)
                .build();

        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));

        CuentaResponse resp = cuentaService.obtenerPorNumeroCuenta(numCuenta);

        assertNotNull(resp);
        assertEquals(20L, resp.getIdCliente());
        assertEquals("Maria Lopez Sanchez", resp.getNombreTitular());
        assertNull(resp.getSaldo());
    }

    @Test
    @DisplayName("obtenerPorNumeroCuenta: cuenta con espacios en blanco es trimeada correctamente")
    void testObtenerPorNumeroCuenta_TrimeaNumero() {
        String numCuenta = "1000000001000000";
        Cuenta cuenta = Cuenta.builder().idCuenta(1L).numeroCuenta(numCuenta).estatus("ACTIVA").build();

        when(cuentaRepository.findByNumeroCuenta(numCuenta)).thenReturn(Optional.of(cuenta));

        CuentaResponse resp = cuentaService.obtenerPorNumeroCuenta("  " + numCuenta + "  ");

        assertNotNull(resp);
        verify(cuentaRepository).findByNumeroCuenta(numCuenta);
    }

    @Test
    @DisplayName("obtenerPorNumeroCuenta: cuenta no encontrada lanza CuentaNoEncontradaException")
    void testObtenerPorNumeroCuenta_NoEncontrada_LanzaExcepcion() {
        String numInexistente = "9999999999999999";
        when(cuentaRepository.findByNumeroCuenta(numInexistente)).thenReturn(Optional.empty());

        CuentaNoEncontradaException ex = assertThrows(CuentaNoEncontradaException.class,
                () -> cuentaService.obtenerPorNumeroCuenta(numInexistente));

        assertTrue(ex.getMessage().contains(numInexistente));
    }

    @Test
    @DisplayName("obtenerCuentasActivas: retorna lista de cuentas mapeadas")
    void testObtenerCuentasActivas_ConRegistros() {
        Cuenta c1 = Cuenta.builder().idCuenta(1L).numeroCuenta("1111").estatus("ACTIVA").build();
        Cuenta c2 = Cuenta.builder().idCuenta(2L).numeroCuenta("2222").estatus("ACTIVA").build();

        when(cuentaRepository.findByEstatus("ACTIVA")).thenReturn(List.of(c1, c2));

        List<CuentaResponse> activas = cuentaService.obtenerCuentasActivas();

        assertNotNull(activas);
        assertEquals(2, activas.size());
        assertEquals("1111", activas.get(0).getNumeroCuenta());
        assertEquals("2222", activas.get(1).getNumeroCuenta());
    }

    @Test
    @DisplayName("obtenerCuentasActivas: cuando no existen cuentas activas retorna lista vacia")
    void testObtenerCuentasActivas_ListaVacia() {
        when(cuentaRepository.findByEstatus("ACTIVA")).thenReturn(Collections.emptyList());

        List<CuentaResponse> activas = cuentaService.obtenerCuentasActivas();

        assertNotNull(activas);
        assertTrue(activas.isEmpty());
    }

    @Test
    @DisplayName("obtenerSaldoPorNumeroCuenta: saldo encontrado retorna SaldoResponse con campos correctos")
    void testObtenerSaldoPorNumeroCuenta_Exitoso() {
        String numCuenta = "1000000001000000";
        LocalDateTime ahora = LocalDateTime.now();

        Saldo saldo = Saldo.builder()
                .idSaldo(99L)
                .saldoDisponible(500.0)
                .saldoContable(500.0)
                .fechaUltimaActualizacion(ahora)
                .build();

        when(saldoRepository.findByCuentaNumeroCuenta(numCuenta)).thenReturn(Optional.of(saldo));

        SaldoResponse resp = cuentaService.obtenerSaldoPorNumeroCuenta(numCuenta);

        assertNotNull(resp);
        assertEquals(99L, resp.getIdSaldo());
        assertEquals(numCuenta, resp.getNumeroCuenta());
        assertEquals(500.0, resp.getSaldoDisponible());
        assertEquals(500.0, resp.getSaldoContable());
        assertEquals(ahora, resp.getFechaUltimaActualizacion());
        verify(saldoRepository).findByCuentaNumeroCuenta(numCuenta);
    }

    @Test
    @DisplayName("obtenerSaldoPorNumeroCuenta: saldo no encontrado lanza CuentaNoEncontradaException")
    void testObtenerSaldoPorNumeroCuenta_NoEncontrado_LanzaExcepcion() {
        String numInexistente = "8888888888888888";
        when(saldoRepository.findByCuentaNumeroCuenta(numInexistente)).thenReturn(Optional.empty());

        CuentaNoEncontradaException ex = assertThrows(CuentaNoEncontradaException.class,
                () -> cuentaService.obtenerSaldoPorNumeroCuenta(numInexistente));

        assertTrue(ex.getMessage().contains(numInexistente));
    }
}
