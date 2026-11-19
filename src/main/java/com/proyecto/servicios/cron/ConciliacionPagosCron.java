package com.proyecto.servicios.cron;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.proyecto.servicios.service.ConciliacionPagosService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dispara periódicamente la conciliación de pagos con resultado desconocido.
 * Se asume una sola instancia de la aplicación; con varias réplicas conviene un lock distribuido (p. ej. ShedLock).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConciliacionPagosCron {

    private final ConciliacionPagosService conciliacionService;

    @Value("${gestopago.conciliacion.habilitada:true}")
    private boolean habilitada;

    @Scheduled(
            fixedDelayString = "${gestopago.conciliacion.intervalo-ms:120000}",
            initialDelayString = "${gestopago.conciliacion.retraso-inicial-ms:60000}")
    public void conciliar() {
        if (!habilitada) {
            return;
        }
        try {
            int procesadas = conciliacionService.conciliarPendientes();
            if (procesadas > 0) {
                log.info("[CRON Conciliacion]: {} operacion(es) sin resolver revisada(s)", procesadas);
            }
        } catch (Exception e) {
            log.error("[CRON Conciliacion]: Error en el ciclo de conciliacion ({})", e.getClass().getSimpleName());
        }
    }
}
