package com.proyecto.servicios.model.cliente;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CuentaResponse {
    private Long idCuenta;
    @Schema(hidden = true)
    private Long idCliente;
    private String nombreTitular;
    private String numeroCuenta;
    private String tipoCuenta;
    private String estatus;
    private LocalDateTime fechaApertura;
    private SaldoResponse saldo;
}
