package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para busqueda de nacionalidad por clave ISO, pais o gentilicio (sin usar ID)")
public class NacionalidadBusquedaRequest {

    @NotBlank(message = "El criterio de busqueda es obligatorio")
    @Schema(description = "Clave ISO (ej. MEX), Pais (ej. Mexico) o Gentilicio (ej. Mexicana)", example = "Mexicana")
    private String criterio;
}
