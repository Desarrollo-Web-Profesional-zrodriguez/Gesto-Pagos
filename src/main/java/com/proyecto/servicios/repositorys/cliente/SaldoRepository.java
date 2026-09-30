package com.proyecto.servicios.repositorys.cliente;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.proyecto.servicios.entity.cliente.Saldo;

@Repository 
public interface SaldoRepository extends JpaRepository<Saldo, Long>{
    // Consultar saldo directamente por número de cuenta
    Optional<Saldo> findByCuentaNumeroCuenta(String numeroCuenta);
    // Consultar saldo porel id de la cuenta
    Optional<Saldo> findByCuentaIdCuenta(Long idCuenta);
}
