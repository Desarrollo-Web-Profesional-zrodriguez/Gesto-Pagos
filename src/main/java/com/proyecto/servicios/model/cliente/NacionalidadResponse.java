package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Elemento del catalogo de nacionalidades de la base de datos")
public class NacionalidadResponse {

    @Schema(description = "Identificador unico de la nacionalidad", example = "1")
    private Long idNacionalidad;

    @Schema(description = "Codigo ISO de 3 letras", example = "MEX")
    private String claveIso;

    @Schema(description = "Nombre del pais", example = "México")
    private String pais;

    @Schema(description = "Gentilicio oficial", example = "Mexicana")
    private String gentilicio;
}
