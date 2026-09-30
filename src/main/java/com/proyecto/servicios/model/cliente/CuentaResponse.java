package com.proyecto.servicios.model.cliente;

import java.time.LocalDateTime;

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
public class CuentaResponse {
    private Long idCuenta;
    private Long idCliente;
    private String nombreTitular;
    private String numeroCuenta;
    private String tipoCuenta;
    private String estatus;
    private LocalDateTime fechaApertura;
    private SaldoResponse saldo;
}
