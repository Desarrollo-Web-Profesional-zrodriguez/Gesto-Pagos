package com.proyecto.servicios.model.cliente;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.proyecto.servicios.config.FlexibleLocalDateDeserializer;

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
@Schema(description = "Criterios para busqueda de clientes (CURP, identificador, correoElectronico, numeroCuenta o RFC). Puede enviarse uno, varios, todos o vacio para traer todos.")
public class ClienteBusquedaAvanzadaRequest {

    @Schema(description = "Identificador general (RFC, CURP, correo electronico o cuenta)", example = "")
    private String identificador;

    @Schema(description = "CURP del cliente (coincidencia exacta o parcial)", example = "")
    private String curp;

    @Schema(description = "RFC del cliente (coincidencia exacta o parcial)", example = "")
    private String rfc;

    @Schema(description = "Correo electronico del cliente (coincidencia exacta o parcial)", example = "")
    private String correoElectronico;

    @Schema(description = "Numero de cuenta bancaria (coincidencia exacta o parcial)", example = "")
    private String numeroCuenta;

    @Schema(hidden = true)
    private Boolean activo;

    @Schema(hidden = true)
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private java.time.LocalDate fechaInicio;

    @Schema(hidden = true)
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private java.time.LocalDate fechaFin;
}
