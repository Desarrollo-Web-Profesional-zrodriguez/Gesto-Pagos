package com.proyecto.servicios.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyecto.servicios.entity.cliente.Saldo;
import com.proyecto.servicios.entity.gestopago.EstatusTransaccion;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.SolicitudDuplicadaException;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse;
import com.proyecto.servicios.repositorys.cliente.SaldoRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.util.DatosSensibles;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Libro mayor del flujo de pagos: cada método es UNA transacción de BD corta y atómica.
 *
 * <p>Estrategia de dinero (patrón de dos fases, "reservar -> confirmar | liberar"):
 * <ol>
 *   <li>{@link #reservar}: bajo bloqueo pesimista del saldo, valida duplicados y saldo, retiene el monto
 *       en {@code saldoDisponible} e inserta la transacción en PROCESANDO — todo en un solo commit.</li>
 *   <li>La llamada HTTP al proveedor ocurre FUERA de cualquier transacción (no se retienen conexiones ni locks).</li>
 *   <li>{@link #confirmarExito} descuenta {@code saldoContable}; {@link #liberarRetencion} devuelve el disponible;
 *       {@link #marcarPendiente} deja la retención intacta cuando el resultado es desconocido.</li>
 * </ol>
 *
 * Las operaciones de cierre son idempotentes: si la transacción ya no está sin resolver, no vuelven a mover dinero
 * (evita doble reverso / doble cargo si el job de conciliación y el flujo en línea coinciden).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PagoLedgerService {

    private final SaldoRepository saldoRepository;
    private final GestoPagoTransaccionRepository transaccionRepository;

    @Value("${gestopago.pago.ventana-duplicados-segundos:120}")
    private long ventanaDuplicadosSegundos = 120;

    /** Resultado de la reserva: saldos antes y después de la retención. */
    public record Reserva(BigDecimal saldoAnterior, BigDecimal saldoDisponibleRetenido) { }

    /** Saldos finales tras confirmar el cargo. */
    public record SaldosFinales(BigDecimal saldoDisponible, BigDecimal saldoContable) { }

    @Transactional
    public Reserva reservar(GestoPagoTransaccion tx) {
        String numCuenta = tx.getNumeroCuenta();
        Saldo saldo = saldoRepository.findByNumeroCuentaForUpdate(numCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(DatosSensibles.enmascararCuenta(numCuenta)));

        // 1) Idempotencia explícita: la misma clave jamás genera un segundo cargo
        if (tx.getIdempotencyKey() != null) {
            transaccionRepository.findByNumeroCuentaAndIdempotencyKey(numCuenta, tx.getIdempotencyKey())
                    .ifPresent(existente -> {
                        throw new SolicitudDuplicadaException(existente.getUpc(), existente.getEstatus());
                    });
        }

        // 2) Anti-duplicados implícito (doble clic, reintento ciego del cliente): misma operación en la ventana
        LocalDateTime desde = LocalDateTime.now().minusSeconds(ventanaDuplicadosSegundos);
        transaccionRepository.findFirstByHuellaAndEstatusInAndFechaCreacionAfter(
                        tx.getHuella(), EstatusTransaccion.BLOQUEANTES, desde)
                .ifPresent(existente -> {
                    throw new SolicitudDuplicadaException(existente.getUpc(), existente.getEstatus());
                });

        // 3) Saldo suficiente (con el registro bloqueado: nadie más puede gastarlo entre la lectura y la escritura)
        BigDecimal monto = tx.getMonto().setScale(2, RoundingMode.HALF_UP);
        BigDecimal disponible = aDecimal(saldo.getSaldoDisponible());
        if (disponible.compareTo(monto) < 0) {
            throw new ReglaNegocioException(CatalogoCodigoError.SALDO_INSUFICIENTE, String.format(
                    "Saldo insuficiente en la cuenta. Saldo disponible: %.2f, monto requerido: %.2f", disponible, monto));
        }

        BigDecimal retenido = disponible.subtract(monto).setScale(2, RoundingMode.HALF_UP);
        saldo.setSaldoDisponible(retenido.doubleValue());
        saldoRepository.save(saldo);

        tx.setEstatus(EstatusTransaccion.PROCESANDO.name());
        transaccionRepository.saveAndFlush(tx);

        log.info("[Ledger]: Reserva aplicada cuenta={} UPC={} monto={} disponible {} -> {}",
                DatosSensibles.enmascararCuenta(numCuenta), tx.getUpc(), monto, disponible, retenido);
        return new Reserva(disponible, retenido);
    }

    @Transactional
    public SaldosFinales confirmarExito(String numCuenta, String upc, RespuestaSendTx respuesta) {
        Saldo saldo = bloquearSaldo(numCuenta);
        GestoPagoTransaccion tx = obtenerTransaccion(upc);

        if (!estaSinResolver(tx)) {
            log.warn("[Ledger]: confirmarExito ignorado, UPC={} ya esta {}", upc, tx.getEstatus());
            return new SaldosFinales(aDecimal(saldo.getSaldoDisponible()), aDecimal(saldo.getSaldoContable()));
        }

        BigDecimal monto = tx.getMonto().setScale(2, RoundingMode.HALF_UP);
        BigDecimal contable = aDecimal(saldo.getSaldoContable()).subtract(monto).setScale(2, RoundingMode.HALF_UP);
        saldo.setSaldoContable(contable.doubleValue());
        saldoRepository.save(saldo);

        aplicarRespuesta(tx, respuesta);
        tx.setEstatus(EstatusTransaccion.EXITOSA.name());
        transaccionRepository.save(tx);

        log.info("[Ledger]: Cargo confirmado cuenta={} UPC={} contable={}", DatosSensibles.enmascararCuenta(numCuenta), upc, contable);
        return new SaldosFinales(aDecimal(saldo.getSaldoDisponible()), contable);
    }

    @Transactional
    public void liberarRetencion(String numCuenta, String upc, RespuestaSendTx respuesta) {
        Saldo saldo = bloquearSaldo(numCuenta);
        GestoPagoTransaccion tx = obtenerTransaccion(upc);

        if (!estaSinResolver(tx)) {
            log.warn("[Ledger]: liberarRetencion ignorado, UPC={} ya esta {}", upc, tx.getEstatus());
            return;
        }

        BigDecimal monto = tx.getMonto().setScale(2, RoundingMode.HALF_UP);
        BigDecimal restaurado = aDecimal(saldo.getSaldoDisponible()).add(monto).setScale(2, RoundingMode.HALF_UP);
        saldo.setSaldoDisponible(restaurado.doubleValue());
        saldoRepository.save(saldo);

        aplicarRespuesta(tx, respuesta);
        tx.setEstatus(EstatusTransaccion.RECHAZADA.name());
        transaccionRepository.save(tx);

        log.info("[Ledger]: Retencion liberada cuenta={} UPC={} disponible={}", DatosSensibles.enmascararCuenta(numCuenta), upc, restaurado);
    }

    /** Resultado desconocido: se conserva la retención (el dinero sigue apartado) y se encola para conciliar. */
    @Transactional
    public void marcarPendiente(String upc, RespuestaSendTx respuesta) {
        GestoPagoTransaccion tx = obtenerTransaccion(upc);
        if (!estaSinResolver(tx)) {
            return;
        }
        aplicarRespuesta(tx, respuesta);
        tx.setEstatus(EstatusTransaccion.PENDIENTE_CONCILIACION.name());
        transaccionRepository.save(tx);
        log.warn("[Ledger]: UPC={} queda PENDIENTE_CONCILIACION con la retencion intacta", upc);
    }

    /** Requiere intervención humana; el saldo permanece retenido hasta que se determine el resultado real. */
    @Transactional
    public void marcarRevisionManual(String upc, String motivo) {
        GestoPagoTransaccion tx = obtenerTransaccion(upc);
        if (!estaSinResolver(tx)) {
            return;
        }
        tx.setEstatus(EstatusTransaccion.REVISION_MANUAL.name());
        tx.setMensajeRespuesta(limitar(motivo, 2000));
        transaccionRepository.save(tx);
        log.error("[Ledger CRITICO]: UPC={} requiere REVISION MANUAL: {}", upc, motivo);
    }

    @Transactional
    public void registrarIntentoConciliacion(String upc) {
        GestoPagoTransaccion tx = obtenerTransaccion(upc);
        tx.setIntentosConciliacion((tx.getIntentosConciliacion() == null ? 0 : tx.getIntentosConciliacion()) + 1);
        transaccionRepository.save(tx);
    }

    // ------------------------------------------------------------------ helpers

    private Saldo bloquearSaldo(String numCuenta) {
        return saldoRepository.findByNumeroCuentaForUpdate(numCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(DatosSensibles.enmascararCuenta(numCuenta)));
    }

    private GestoPagoTransaccion obtenerTransaccion(String upc) {
        return transaccionRepository.findByUpc(upc)
                .orElseThrow(() -> new IllegalStateException("Transaccion inexistente para UPC " + upc));
    }

    private boolean estaSinResolver(GestoPagoTransaccion tx) {
        return EstatusTransaccion.SIN_RESOLVER.contains(tx.getEstatus());
    }

    private BigDecimal aDecimal(Double valor) {
        return BigDecimal.valueOf(valor != null ? valor : 0.0).setScale(2, RoundingMode.HALF_UP);
    }

    private void aplicarRespuesta(GestoPagoTransaccion tx, RespuestaSendTx r) {
        if (r == null) {
            return;
        }
        tx.setResponseXml(r.respuestaRaw());
        tx.setCodigoRespuesta(limitar(r.codigo(), 10));
        tx.setMensajeRespuesta(DatosSensibles.sanitizarTextoProveedor(r.texto()));

        SendTxXmlResponse xml = r.parsed();
        if (xml == null) {
            return;
        }
        String idTx = xml.getIdTx() != null ? xml.getIdTx()
                : (xml.getMensaje() != null ? xml.getMensaje().getIdTx() : null);
        tx.setIdTxGestoPago(limitar(idTx, 50));
        tx.setNumAutorizacion(limitar(xml.getNumAutorizacion(), 60));
        tx.setComision(parsearDecimal(xml.getComision()));
        if (xml.getMensaje() != null) {
            tx.setPin(limitar(xml.getMensaje().getPin(), 250));
            tx.setLegend(xml.getMensaje().getLegend());
        }
    }

    private BigDecimal parsearDecimal(String valor) {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(valor.trim().replace(",", "")).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private String limitar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        String t = valor.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
