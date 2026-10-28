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
public class SaldoResponse {
    private Long idSaldo;
    private String numeroCuenta;
    private Double saldoDisponible;
    private Double saldoContable;
    private LocalDateTime fechaUltimaActualizacion;
}
