package com.proyecto.servicios.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.proyecto.servicios.entity.gestopago.EstatusTransaccion;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Conciliación de operaciones cuyo resultado en el proveedor se desconoce (timeout, 5xx, respuesta ilegible,
 * códigos 07/08) o que quedaron a medias por una caída local.
 *
 * <p>Principio: NUNCA se asume que un timeout significa "no procesado". Mientras no haya certeza, el dinero
 * permanece retenido. La reconsulta reutiliza el MISMO UPC y el MISMO payload cifrado (idempotencia del lado
 * del proveedor): si ya lo aplicó, no puede cobrarse dos veces.
 *
 * <ul>
 *   <li>01 -> se confirma el cargo (EXITOSA).</li>
 *   <li>Rechazo explícito del proveedor distinto de 06 -> se libera la retención (RECHAZADA).</li>
 *   <li>06 ("aplicada anteriormente") -> el intento original probablemente sí se aplicó: REVISION_MANUAL, sin mover saldo.</li>
 *   <li>Timeout / indeterminado / falla técnica -> permanece pendiente y se reintenta en el siguiente ciclo.</li>
 *   <li>Intentos agotados o reintento deshabilitado -> REVISION_MANUAL (conciliar contra el reporte del proveedor).</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConciliacionPagosService {

    private static final String CODIGO_DUPLICADA = "06";

    private final GestoPagoTransaccionRepository transaccionRepository;
    private final PagoLedgerService ledger;
    private final GestoPagoSendTxGateway sendTxGateway;

    /** Tiempo mínimo que debe llevar sin resolverse una operación antes de conciliarla (evita pisar el flujo en línea). */
    @Value("${gestopago.conciliacion.espera-minutos:2}")
    private long esperaMinutos = 2;

    @Value("${gestopago.conciliacion.max-intentos:5}")
    private int maxIntentos = 5;

    /** Permite desactivar la reconsulta automática si el proveedor no garantiza idempotencia por UPC. */
    @Value("${gestopago.conciliacion.reintento-habilitado:true}")
    private boolean reintentoHabilitado = true;

    /** @return cantidad de operaciones procesadas en este ciclo */
    public int conciliarPendientes() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(esperaMinutos);
        List<GestoPagoTransaccion> pendientes = transaccionRepository
                .findByEstatusInAndFechaActualizacionBefore(EstatusTransaccion.SIN_RESOLVER, limite);

        for (GestoPagoTransaccion tx : pendientes) {
            try {
                conciliar(tx);
            } catch (Exception e) {
                log.error("[Conciliacion]: Error conciliando UPC={} ({})", tx.getUpc(), e.getClass().getSimpleName());
            }
        }
        return pendientes.size();
    }

    void conciliar(GestoPagoTransaccion tx) {
        String upc = tx.getUpc();
        int intentos = tx.getIntentosConciliacion() == null ? 0 : tx.getIntentosConciliacion();

        if (!reintentoHabilitado || intentos >= maxIntentos
                || tx.getPayloadCifrado() == null || tx.getPayloadCifrado().isBlank()) {
            ledger.marcarRevisionManual(upc, "No fue posible determinar el resultado en el proveedor tras "
                    + intentos + " intento(s) de conciliacion");
            return;
        }

        ledger.registrarIntentoConciliacion(upc);
        RespuestaSendTx r = sendTxGateway.ejecutar(tx.getPayloadCifrado(), upc);

        switch (r.veredicto()) {
            case EXITO -> {
                ledger.confirmarExito(tx.getNumeroCuenta(), upc, r);
                log.info("[Conciliacion]: UPC={} confirmada como EXITOSA", upc);
            }
            case RECHAZADO -> {
                if (!r.esRechazoExplicitoDelProveedor()) {
                    // La reconsulta ni siquiera llegó al proveedor (token/HTTP 4xx): el intento ORIGINAL sigue sin certeza.
                    log.warn("[Conciliacion]: UPC={} la reconsulta no llego al proveedor; se reintentara", upc);
                } else if (CODIGO_DUPLICADA.equals(r.codigo())) {
                    ledger.marcarRevisionManual(upc, "El proveedor respondio 06 (aplicada anteriormente) al reconsultar: "
                            + "el intento original probablemente fue aplicado. Verificar contra el reporte del proveedor");
                } else {
                    ledger.liberarRetencion(tx.getNumeroCuenta(), upc, r);
                    log.info("[Conciliacion]: UPC={} rechazada por el proveedor (CODIGO={}); retencion liberada", upc, r.codigo());
                }
            }
            case INDETERMINADO -> log.warn("[Conciliacion]: UPC={} sigue sin resultado definitivo (intento {})", upc, intentos + 1);
        }
    }
}
