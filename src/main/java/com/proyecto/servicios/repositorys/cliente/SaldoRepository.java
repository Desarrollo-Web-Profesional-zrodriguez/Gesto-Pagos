package com.proyecto.servicios.repositorys.cliente;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.proyecto.servicios.entity.cliente.Saldo;

import jakarta.persistence.LockModeType;

@Repository 
public interface SaldoRepository extends JpaRepository<Saldo, Long>{
    // Consultar saldo directamente por número de cuenta
    Optional<Saldo> findByCuentaNumeroCuenta(String numeroCuenta);
    // Consultar saldo porel id de la cuenta
    Optional<Saldo> findByCuentaIdCuenta(Long idCuenta);

    /**
     * Lectura con bloqueo pesimista (SELECT ... FOR UPDATE). Serializa las operaciones
     * monetarias concurrentes sobre una misma cuenta: evita doble gasto y carreras de lectura-modificación-escritura.
     * Solo debe invocarse dentro de una transacción corta (nunca alrededor de llamadas HTTP).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Saldo s WHERE s.cuenta.numeroCuenta = :numeroCuenta")
    Optional<Saldo> findByNumeroCuentaForUpdate(@Param("numeroCuenta") String numeroCuenta);
}
