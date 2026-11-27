package com.proyecto.servicios.service.cliente;

import java.util.List;

import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.cliente.SaldoResponse;

public interface CuentaService {

    CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta);

    List<CuentaResponse> obtenerCuentasActivas();

    SaldoResponse obtenerSaldoPorNumeroCuenta(String numeroCuenta);
}
