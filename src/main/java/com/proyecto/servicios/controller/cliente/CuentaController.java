package com.proyecto.servicios.controller.cliente;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.cliente.SaldoResponse;
import com.proyecto.servicios.service.cliente.CuentaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
@Validated
@Tag(name = "Cuentas y Saldos", description = "Consultas de cuentas bancarias y saldos disponibles")
public class CuentaController {

    private final CuentaService cuentaService;

    // Consultar cuenta por numero de cuenta
    @GetMapping("/{numeroCuenta}")
    @Operation(summary = "Consultar cuenta por su numero unico")
    public ResponseEntity<CuentaResponse> obtenerPorNumeroCuenta(
            @Parameter(description = "Numero de cuenta bancaria (10 digitos)", example = "1000000001") 
            @PathVariable 
            @Pattern(regexp = "^\\d{10}$", message = "El numero de cuenta debe tener exactamente 10 digitos numericos") 
            String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerPorNumeroCuenta(numeroCuenta));
    }

    // Consultar cuentas activas
    @GetMapping("/activas")
    @Operation(summary = "Consultar todas las cuentas activas")
    public ResponseEntity<List<CuentaResponse>> obtenerCuentasActivas() {
        return ResponseEntity.ok(cuentaService.obtenerCuentasActivas());
    }

    // Consultar saldo de una cuenta
    @GetMapping("/{numeroCuenta}/saldo")
    @Operation(summary = "Consultar saldo disponible y contable de una cuenta")
    public ResponseEntity<SaldoResponse> obtenerSaldoPorNumeroCuenta(
            @Parameter(description = "Numero de cuenta bancaria (10 digitos)", example = "1000000001") 
            @PathVariable 
            @Pattern(regexp = "^\\d{10}$", message = "El numero de cuenta debe tener exactamente 10 digitos numericos") 
            String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerSaldoPorNumeroCuenta(numeroCuenta));
    }
}
