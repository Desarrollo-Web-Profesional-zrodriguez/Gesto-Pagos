package com.proyecto.servicios.repositorys.cliente;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.proyecto.servicios.entity.cliente.UsuarioLogin;

@Repository 
public interface UsuarioLoginRepository extends JpaRepository<UsuarioLogin, Long>{
    // Buscar usuario por nombre de usuario o correo para el login
    Optional<UsuarioLogin> findByUsername(String username);
    // Buscar usuario de login por cliente
    Optional<UsuarioLogin> findByClienteIdCliente(Long idCliente);
    // Validar si el username ya está ocupado
    boolean existsByUsername(String username);
}
