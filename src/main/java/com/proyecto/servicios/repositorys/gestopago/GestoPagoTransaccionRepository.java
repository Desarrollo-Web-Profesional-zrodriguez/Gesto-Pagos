package com.proyecto.servicios.repositorys.gestopago;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;

@Repository
public interface GestoPagoTransaccionRepository extends JpaRepository<GestoPagoTransaccion, Long> {

    Optional<GestoPagoTransaccion> findByUpc(String upc);

    List<GestoPagoTransaccion> findByNumeroCuentaOrderByFechaCreacionDesc(String numeroCuenta);

    /** Idempotencia explícita: misma cuenta + misma Idempotency-Key. */
    Optional<GestoPagoTransaccion> findByNumeroCuentaAndIdempotencyKey(String numeroCuenta, String idempotencyKey);

    /** Anti-duplicados implícito: operación equivalente reciente en un estado que no permite repetirla. */
    Optional<GestoPagoTransaccion> findFirstByHuellaAndEstatusInAndFechaCreacionAfter(
            String huella, Collection<String> estatus, LocalDateTime desde);

    /** Candidatas a conciliación. */
    List<GestoPagoTransaccion> findByEstatusInAndFechaActualizacionBefore(
            Collection<String> estatus, LocalDateTime limite);
}
