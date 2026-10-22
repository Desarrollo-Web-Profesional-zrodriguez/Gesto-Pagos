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
@Schema(description = "Criterios para busqueda avanzada de clientes por coincidencias (CURP, RFC, correo electronico o numero de cuenta)")
public class ClienteBusquedaAvanzadaRequest {

    @Schema(description = "Identificador general (RFC, CURP, correo electronico o cuenta)", example = "PELJ920520HDFRRN09")
    private String identificador;

    @Schema(description = "CURP del cliente (coincidencia exacta o parcial)", example = "PELJ920520HDFRRN09")
    private String curp;

    @Schema(description = "RFC del cliente (coincidencia exacta o parcial)", example = "PELJ9205201A0")
    private String rfc;

    @Schema(description = "Correo electronico del cliente (coincidencia exacta o parcial)", example = "juan.perez@example.com")
    private String correoElectronico;

    @Schema(description = "Numero de cuenta bancaria (coincidencia exacta o parcial)", example = "1000000001")
    private String numeroCuenta;

    @Schema(description = "Filtro por estatus activo (true o false)", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha inicial de registro (YYYY-MM-DD)", example = "2026-01-01")
    private java.time.LocalDate fechaInicio;

    @Schema(description = "Fecha final de registro (YYYY-MM-DD)", example = "2026-12-31")
    private java.time.LocalDate fechaFin;
}
