package com.proyecto.servicios.repositorys.cliente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.proyecto.servicios.entity.cliente.Nacionalidad;

@Repository
public interface NacionalidadRepository extends JpaRepository<Nacionalidad, Long> {

    List<Nacionalidad> findByActivoTrue();

    Optional<Nacionalidad> findByGentilicioIgnoreCase(String gentilicio);

    Optional<Nacionalidad> findByClaveIsoIgnoreCase(String claveIso);

    Optional<Nacionalidad> findByPaisIgnoreCase(String pais);
}
