package com.proyecto.servicios.repositorys.cliente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.proyecto.servicios.entity.cliente.SesionLogin;

@Repository 
public interface SesionLoginRepository extends JpaRepository<SesionLogin, Long>{
    // Buscar sesión por su token único
    Optional<SesionLogin> findByTokenSesion(String tokenSesion);
    // Buscar sesión activa por token
    Optional<SesionLogin> findByTokenSesionAndActivaTrue(String tokenSesion);
    // Obtener tofas las sesiones activas de un usuario
    List<SesionLogin> findByUsuarioIdUsuarioAndActivaTrue(Long idUsuario);
}
