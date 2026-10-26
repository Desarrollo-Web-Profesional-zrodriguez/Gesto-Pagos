package com.proyecto.servicios.repositorys.gestopago;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;

@Repository
public interface GestoPagoTransaccionRepository extends JpaRepository<GestoPagoTransaccion, Long> {

    Optional<GestoPagoTransaccion> findByUpc(String upc);

    List<GestoPagoTransaccion> findByNumeroCuentaOrderByFechaCreacionDesc(String numeroCuenta);
}
