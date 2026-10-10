package com.proyecto.servicios.controller.cliente;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.model.cliente.NacionalidadResponse;
import com.proyecto.servicios.repositorys.cliente.NacionalidadRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/catalogos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Catalogos", description = "Endpoints para consulta de catalogos del sistema almacenados en base de datos")
public class CatalogoController {

    private final NacionalidadRepository nacionalidadRepository;

    @GetMapping("/nacionalidades")
    @Operation(summary = "Listar catalogo de nacionalidades", description = "Obtiene todas las nacionalidades activas registradas en la base de datos.")
    public ResponseEntity<List<NacionalidadResponse>> listarNacionalidades() {
        List<NacionalidadResponse> lista = nacionalidadRepository.findByActivoTrue().stream()
                .map(n -> NacionalidadResponse.builder()
                        .idNacionalidad(n.getIdNacionalidad())
                        .claveIso(n.getClaveIso())
                        .pais(n.getPais())
                        .gentilicio(n.getGentilicio())
                        .build())
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/nacionalidades/{id}")
    @Operation(summary = "Consultar nacionalidad por ID", description = "Obtiene los detalles de una nacionalidad especifica por su ID de catalogo.")
    public ResponseEntity<NacionalidadResponse> obtenerNacionalidadPorId(
            @Parameter(description = "ID de la nacionalidad (mayor a cero)", example = "1") 
            @PathVariable @Positive(message = "El ID de la nacionalidad debe ser un numero mayor a cero") Long id) {
        return nacionalidadRepository.findById(id)
                .map(n -> ResponseEntity.ok(NacionalidadResponse.builder()
                        .idNacionalidad(n.getIdNacionalidad())
                        .claveIso(n.getClaveIso())
                        .pais(n.getPais())
                        .gentilicio(n.getGentilicio())
                        .build()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Nacionalidad no encontrada con ID: " + id));
    }
}
