package com.proyecto.servicios.service.cliente.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Saldo;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.cliente.SaldoResponse;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.SaldoRepository;
import com.proyecto.servicios.service.cliente.CuentaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final SaldoRepository saldoRepository;

    @Override
    @Transactional(readOnly = true)
    public CuentaResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
        return mapearACuentaResponse(cuenta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> obtenerCuentasActivas() {
        return cuentaRepository.findByEstatus("ACTIVA").stream()
                .map(this::mapearACuentaResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SaldoResponse obtenerSaldoPorNumeroCuenta(String numeroCuenta) {
        Saldo saldo = saldoRepository.findByCuentaNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));

        return SaldoResponse.builder()
                .idSaldo(saldo.getIdSaldo())
                .numeroCuenta(numeroCuenta)
                .saldoDisponible(saldo.getSaldoDisponible())
                .saldoContable(saldo.getSaldoContable())
                .fechaUltimaActualizacion(saldo.getFechaUltimaActualizacion())
                .build();
    }

    private CuentaResponse mapearACuentaResponse(Cuenta cuenta) {
        String nombreTitular = "";
        Long idCliente = null;
        if (cuenta.getCliente() != null) {
            idCliente = cuenta.getCliente().getIdCliente();
            nombreTitular = cuenta.getCliente().getNombre() + " " + cuenta.getCliente().getApellidoPaterno() + " " + cuenta.getCliente().getApellidoMaterno();
        }

        SaldoResponse sResp = null;
        if (cuenta.getSaldo() != null) {
            sResp = SaldoResponse.builder()
                    .idSaldo(cuenta.getSaldo().getIdSaldo())
                    .numeroCuenta(cuenta.getNumeroCuenta())
                    .saldoDisponible(cuenta.getSaldo().getSaldoDisponible())
                    .saldoContable(cuenta.getSaldo().getSaldoContable())
                    .fechaUltimaActualizacion(cuenta.getSaldo().getFechaUltimaActualizacion())
                    .build();
        }

        return CuentaResponse.builder()
                .idCuenta(cuenta.getIdCuenta())
                .idCliente(idCliente)
                .nombreTitular(nombreTitular.trim())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .tipoCuenta(cuenta.getTipoCuenta())
                .estatus(cuenta.getEstatus())
                .fechaApertura(cuenta.getFechaApertura())
                .saldo(sResp)
                .build();
    }
}
