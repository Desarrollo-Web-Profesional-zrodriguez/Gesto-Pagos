package com.proyecto.servicios.repositorys.cliente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.proyecto.servicios.entity.cliente.Cliente;

@Repository 
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Consultas directas
    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByCurpIgnoreCase(String curp);
    Optional<Cliente> findByRfc(String rfc);
    Optional<Cliente> findByRfcIgnoreCase(String rfc);
    Optional<Cliente> findByCorreoElectronico(String correoElectronico);
    Optional<Cliente> findByCorreoElectronicoIgnoreCase(String correoElectronico);
    List<Cliente> findByActivoTrue();
    List<Cliente> findByFechaCreacionBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    // Consulta para obtener cliente a partir del numero de cuenta asociada
    Optional<Cliente> findByCuentasNumeroCuenta(String numeroCuenta);

    // Búsquedas por coincidencias parciales (Containing / LIKE)
    @Query("SELECT c FROM Cliente c WHERE " +
           "LOWER(CONCAT(c.nombre, ' ', COALESCE(c.segundoNombre, ''), ' ', c.apellidoPaterno, ' ', c.apellidoMaterno)) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(CONCAT(c.nombre, ' ', c.apellidoPaterno, ' ', c.apellidoMaterno)) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "(c.segundoNombre IS NOT NULL AND LOWER(c.segundoNombre) LIKE LOWER(CONCAT('%', :texto, '%'))) OR " +
           "LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(c.apellidoMaterno) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<Cliente> buscarPorNombreOCoincidencia(@Param("texto") String texto);

    List<Cliente> findByCurpContainingIgnoreCase(String curp);

    List<Cliente> findByRfcContainingIgnoreCase(String rfc);

    List<Cliente> findByCorreoElectronicoContainingIgnoreCase(String correoElectronico);

    @Query("SELECT DISTINCT c FROM Cliente c JOIN c.cuentas cu WHERE cu.numeroCuenta LIKE CONCAT('%', :numeroCuenta, '%')")
    List<Cliente> buscarPorNumeroCuentaContaining(@Param("numeroCuenta") String numeroCuenta);

    // Validaciones de reglas de negocio (no duplicados)
    boolean existsByCurp(String curp);
    boolean existsByRfc(String rfc);
    boolean existsByCorreoElectronico(String correoElectronico);
}
