package com.proyecto.servicios.repositorys.cliente;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.proyecto.servicios.entity.cliente.Domicilio;

@Repository 
public interface DomicilioRepository extends JpaRepository<Domicilio, Long>{
    // Consultar domicilios asociados a un cliente
    List<Domicilio> findByClienteIdCliente(Long idCliente);
}
