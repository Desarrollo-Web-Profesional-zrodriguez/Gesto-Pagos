package com.proyecto.servicios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx.Veredicto;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse.MensajeTxXml;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;

@ExtendWith(MockitoExtension.class)
class ConciliacionPagosServiceTest {

    private static final String CUENTA = "1000000001000000";

    @Mock
    private GestoPagoTransaccionRepository repository;
    @Mock
    private PagoLedgerService ledger;
    @Mock
    private GestoPagoSendTxGateway gateway;

    private ConciliacionPagosService service;

    @BeforeEach
    void setUp() {
        service = new ConciliacionPagosService(repository, ledger, gateway);
    }

    private GestoPagoTransaccion pendiente(int intentos) {
        return GestoPagoTransaccion.builder().upc("UPC0000001").numeroCuenta(CUENTA).monto(new BigDecimal("150.00"))
                .payloadCifrado("cifrado").estatus("PENDIENTE_CONCILIACION").intentosConciliacion(intentos).build();
    }

    private void hayPendiente(GestoPagoTransaccion tx) {
        when(repository.findByEstatusInAndFechaActualizacionBefore(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(List.of(tx));
    }

    private RespuestaSendTx explicita(Veredicto v, String codigo) {
        SendTxXmlResponse xml = SendTxXmlResponse.builder().mensaje(new MensajeTxXml(codigo, "t", null, null, null, null)).build();
        return new RespuestaSendTx(v, codigo, "t", "<r/>", xml, null);
    }

    @Test
    @DisplayName("Reconsulta con el MISMO UPC y payload; 01 confirma el cargo")
    void exito_confirma() {
        hayPendiente(pendiente(0));
        when(gateway.ejecutar("cifrado", "UPC0000001")).thenReturn(explicita(Veredicto.EXITO, "01"));

        service.conciliarPendientes();

        verify(ledger).registrarIntentoConciliacion("UPC0000001");
        verify(ledger).confirmarExito(eq(CUENTA), eq("UPC0000001"), any());
        verify(ledger, never()).liberarRetencion(any(), any(), any());
    }

    @Test
    @DisplayName("Rechazo explicito distinto de 06 libera la retencion")
    void rechazo_liberaSaldo() {
        hayPendiente(pendiente(1));
        when(gateway.ejecutar(anyString(), anyString())).thenReturn(explicita(Veredicto.RECHAZADO, "02"));

        service.conciliarPendientes();

        verify(ledger).liberarRetencion(eq(CUENTA), eq("UPC0000001"), any());
    }

    @Test
    @DisplayName("06 (aplicada anteriormente) NO mueve saldo: pasa a revision manual")
    void codigo06_revisionManual() {
        hayPendiente(pendiente(1));
        when(gateway.ejecutar(anyString(), anyString())).thenReturn(explicita(Veredicto.RECHAZADO, "06"));

        service.conciliarPendientes();

        verify(ledger).marcarRevisionManual(eq("UPC0000001"), contains("06"));
        verify(ledger, never()).liberarRetencion(any(), any(), any());
        verify(ledger, never()).confirmarExito(any(), any(), any());
    }

    @Test
    @DisplayName("Si la reconsulta tampoco llega al proveedor, la operacion sigue pendiente (no se libera)")
    void reconsultaNoLlega_siguePendiente() {
        hayPendiente(pendiente(1));
        when(gateway.ejecutar(anyString(), anyString())).thenReturn(
                new RespuestaSendTx(Veredicto.RECHAZADO, null, null, null, null, CatalogoCodigoError.TOKEN_GESTOPAGO_INVALIDO));

        service.conciliarPendientes();

        verify(ledger, never()).liberarRetencion(any(), any(), any());
        verify(ledger, never()).marcarRevisionManual(any(), any());
    }

    @Test
    @DisplayName("Indeterminado de nuevo: permanece pendiente para el siguiente ciclo")
    void indeterminado_siguePendiente() {
        hayPendiente(pendiente(2));
        when(gateway.ejecutar(anyString(), anyString())).thenReturn(
                new RespuestaSendTx(Veredicto.INDETERMINADO, null, null, null, null, null));

        service.conciliarPendientes();

        verify(ledger, never()).liberarRetencion(any(), any(), any());
        verify(ledger, never()).confirmarExito(any(), any(), any());
    }

    @Test
    @DisplayName("Intentos agotados => revision manual sin volver a llamar al proveedor")
    void intentosAgotados_revisionManual() {
        hayPendiente(pendiente(5));

        service.conciliarPendientes();

        verify(ledger).marcarRevisionManual(eq("UPC0000001"), anyString());
        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("Con la reconsulta deshabilitada nunca se vuelve a invocar sendTx")
    void reintentoDeshabilitado() {
        ReflectionTestUtils.setField(service, "reintentoHabilitado", false);
        hayPendiente(pendiente(0));

        service.conciliarPendientes();

        verify(ledger).marcarRevisionManual(eq("UPC0000001"), anyString());
        verifyNoInteractions(gateway);
        assertEquals(1, service.conciliarPendientes());
    }
}
