package com.proyecto.servicios.repositorys.cliente;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.proyecto.servicios.entity.cliente.Cliente;

@Repository 
public interface  ClienteRepository extends JpaRepository<Cliente, Long> {
    // consultas solicitadas
    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByRfc(String rfc);
    Optional<Cliente> findByCorreoElectronico(String correoElectronico);
    List<Cliente> findByActivoTrue();
    List<Cliente> findByFechaCreacionBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);
    // Consulta para obtener cliente a partir del número de cuenta asociada
    Optional<Cliente> findByCuentasNumeroCuenta(String numeroCuenta);
    // Validaciones de reglas de negocio (no duplicados)
    boolean existsByCurp(String curp);
    boolean existsByRfc(String rfc);
    boolean existsByCorreoElectronico(String correoElectronico);
    
}
