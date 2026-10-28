package com.proyecto.servicios.repositorys.cliente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.proyecto.servicios.entity.cliente.Cuenta;

@Repository 
public interface  CuentaRepository extends JpaRepository<Cuenta, Long>{
    // Buscar cuenta por número único
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
    // Consultar cuenta con estatus ACTIVA
    List<Cuenta> findByEstatus(String estatus);
    // Validar unicidad del número de cuenta generado
    boolean existsByNumeroCuenta(String numeroCuenta);
    // Cuentas asociadas a un cliente específico
    List<Cuenta> findByClienteIdCliente(Long idCliente);
}
