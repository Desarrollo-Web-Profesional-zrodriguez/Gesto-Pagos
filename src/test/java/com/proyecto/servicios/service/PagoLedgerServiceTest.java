package com.proyecto.servicios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.proyecto.servicios.entity.cliente.Saldo;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.SolicitudDuplicadaException;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx.Veredicto;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse.MensajeTxXml;
import com.proyecto.servicios.repositorys.cliente.SaldoRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.service.PagoLedgerService.Reserva;
import com.proyecto.servicios.service.PagoLedgerService.SaldosFinales;

@ExtendWith(MockitoExtension.class)
class PagoLedgerServiceTest {

    private static final String CUENTA = "1000000001000000";

    @Mock
    private SaldoRepository saldoRepository;
    @Mock
    private GestoPagoTransaccionRepository transaccionRepository;

    private PagoLedgerService ledger;
    private Saldo saldo;

    @BeforeEach
    void setUp() {
        ledger = new PagoLedgerService(saldoRepository, transaccionRepository);
        saldo = Saldo.builder().idSaldo(1L).saldoDisponible(1000.00).saldoContable(1000.00).build();
    }

    private GestoPagoTransaccion nuevaTx(String estatus) {
        return GestoPagoTransaccion.builder()
                .upc("UPC0000001").numeroCuenta(CUENTA).monto(new BigDecimal("150.00"))
                .huella("huella-1").idempotencyKey("clave-0001").estatus(estatus).build();
    }

    private RespuestaSendTx respuestaExito() {
        SendTxXmlResponse xml = SendTxXmlResponse.builder().idTx("291620941").numAutorizacion("100074211").comision("6.0")
                .mensaje(new MensajeTxXml("01", "OK", null, null, null, null)).build();
        return new RespuestaSendTx(Veredicto.EXITO, "01", "OK", "<RESPONSE/>", xml, null);
    }

    @Test
    @DisplayName("reservar: retiene el monto en saldo disponible y registra la transaccion en PROCESANDO")
    void reservar_ok() {
        when(saldoRepository.findByNumeroCuentaForUpdate(CUENTA)).thenReturn(Optional.of(saldo));
        when(transaccionRepository.findByNumeroCuentaAndIdempotencyKey(CUENTA, "clave-0001")).thenReturn(Optional.empty());
        when(transaccionRepository.findFirstByHuellaAndEstatusInAndFechaCreacionAfter(eq("huella-1"), anyCollection(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        GestoPagoTransaccion tx = nuevaTx(null);
        Reserva reserva = ledger.reservar(tx);

        assertEquals(new BigDecimal("1000.00"), reserva.saldoAnterior());
        assertEquals(new BigDecimal("850.00"), reserva.saldoDisponibleRetenido());
        assertEquals(850.00, saldo.getSaldoDisponible());
        assertEquals(1000.00, saldo.getSaldoContable());
        assertEquals("PROCESANDO", tx.getEstatus());
        verify(transaccionRepository).saveAndFlush(tx);
    }

    @Test
    @DisplayName("reservar: saldo insuficiente no modifica saldo ni registra la transaccion")
    void reservar_saldoInsuficiente() {
        saldo.setSaldoDisponible(100.00);
        when(saldoRepository.findByNumeroCuentaForUpdate(CUENTA)).thenReturn(Optional.of(saldo));
        when(transaccionRepository.findByNumeroCuentaAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(transaccionRepository.findFirstByHuellaAndEstatusInAndFechaCreacionAfter(any(), anyCollection(), any()))
                .thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> ledger.reservar(nuevaTx(null)));

        assertEquals(CatalogoCodigoError.SALDO_INSUFICIENTE, ex.getCatalogoError());
        assertEquals(100.00, saldo.getSaldoDisponible());
        verify(saldoRepository, never()).save(any());
        verify(transaccionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("reservar: la misma Idempotency-Key no genera un segundo cargo")
    void reservar_idempotencyKeyRepetida() {
        GestoPagoTransaccion existente = nuevaTx("PENDIENTE_CONCILIACION");
        when(saldoRepository.findByNumeroCuentaForUpdate(CUENTA)).thenReturn(Optional.of(saldo));
        when(transaccionRepository.findByNumeroCuentaAndIdempotencyKey(CUENTA, "clave-0001")).thenReturn(Optional.of(existente));

        SolicitudDuplicadaException ex = assertThrows(SolicitudDuplicadaException.class, () -> ledger.reservar(nuevaTx(null)));

        assertEquals("UPC0000001", ex.getUpcExistente());
        assertEquals(1000.00, saldo.getSaldoDisponible());
        verify(transaccionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("reservar: una operacion identica dentro de la ventana se rechaza como duplicada")
    void reservar_huellaDuplicadaEnVentana() {
        when(saldoRepository.findByNumeroCuentaForUpdate(CUENTA)).thenReturn(Optional.of(saldo));
        when(transaccionRepository.findByNumeroCuentaAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(transaccionRepository.findFirstByHuellaAndEstatusInAndFechaCreacionAfter(eq("huella-1"), anyCollection(), any()))
                .thenReturn(Optional.of(nuevaTx("EXITOSA")));

        assertThrows(SolicitudDuplicadaException.class, () -> ledger.reservar(nuevaTx(null)));

        assertEquals(1000.00, saldo.getSaldoDisponible());
        verify(saldoRepository, never()).save(any());
    }

    @Test
    @DisplayName("reservar: cuenta sin saldo registrado")
    void reservar_cuentaInexistente() {
        when(saldoRepository.findByNumeroCuentaForUpdate(CUENTA)).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> ledger.reservar(nuevaTx(null)));
    }

    @Test
    @DisplayName("confirmarExito: descuenta saldo contable y marca EXITOSA; es idempotente")
    void confirmarExito_idempotente() {
        saldo.setSaldoDisponible(850.00);
        GestoPagoTransaccion tx = nuevaTx("PROCESANDO");
        when(saldoRepository.findByNumeroCuentaForUpdate(CUENTA)).thenReturn(Optional.of(saldo));
        when(transaccionRepository.findByUpc("UPC0000001")).thenReturn(Optional.of(tx));

        SaldosFinales finales = ledger.confirmarExito(CUENTA, "UPC0000001", respuestaExito());

        assertEquals(new BigDecimal("850.00"), finales.saldoContable());
        assertEquals("EXITOSA", tx.getEstatus());
        assertEquals("100074211", tx.getNumAutorizacion());
        assertEquals("291620941", tx.getIdTxGestoPago());

        // Segundo cierre: no vuelve a descontar
        ledger.confirmarExito(CUENTA, "UPC0000001", respuestaExito());
        assertEquals(850.00, saldo.getSaldoContable());
    }

    @Test
    @DisplayName("liberarRetencion: restituye el saldo disponible una sola vez")
    void liberarRetencion_idempotente() {
        saldo.setSaldoDisponible(850.00);
        GestoPagoTransaccion tx = nuevaTx("PROCESANDO");
        when(saldoRepository.findByNumeroCuentaForUpdate(CUENTA)).thenReturn(Optional.of(saldo));
        when(transaccionRepository.findByUpc("UPC0000001")).thenReturn(Optional.of(tx));

        ledger.liberarRetencion(CUENTA, "UPC0000001", null);
        ledger.liberarRetencion(CUENTA, "UPC0000001", null);

        assertEquals(1000.00, saldo.getSaldoDisponible());
        assertEquals("RECHAZADA", tx.getEstatus());
    }

    @Test
    @DisplayName("marcarPendiente: conserva la retencion (no toca el saldo)")
    void marcarPendiente_noTocaSaldo() {
        GestoPagoTransaccion tx = nuevaTx("PROCESANDO");
        when(transaccionRepository.findByUpc("UPC0000001")).thenReturn(Optional.of(tx));

        ledger.marcarPendiente("UPC0000001", null);

        assertEquals("PENDIENTE_CONCILIACION", tx.getEstatus());
        verifyNoInteractions(saldoRepository);
    }

    @Test
    @DisplayName("marcarPendiente: no degrada una transaccion ya resuelta")
    void marcarPendiente_noDegradaResuelta() {
        GestoPagoTransaccion tx = nuevaTx("EXITOSA");
        when(transaccionRepository.findByUpc("UPC0000001")).thenReturn(Optional.of(tx));

        ledger.marcarPendiente("UPC0000001", null);

        assertEquals("EXITOSA", tx.getEstatus());
    }
}
