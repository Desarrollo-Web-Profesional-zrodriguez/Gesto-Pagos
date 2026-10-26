package com.proyecto.servicios.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.model.cliente.CuentaConsultaRequest;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioResponse;
import com.proyecto.servicios.service.PagoServicioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Pagos y Transacciones GestoPago (sendTx)", description = "Procesamiento de pagos de servicios y recargas con validacion de cuenta a 16 digitos, 2FA, saldo disponible y cifrado AES-256 hacia sendTx.do")
public class PagoServicioController {

    private final PagoServicioService pagoServicioService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Realizar pago de servicio o recarga (sendTx)",
        description = "Valida cuenta de 16 digitos, cliente activo, saldo suficiente, 2FA (biometria facial MediaPipe / sesion / password), existencia de idServicio e idProducto en /productos, cifra la peticion con AES-256 y la envia a sendTx.do con el Bearer Token."
    )
    public ResponseEntity<PagoServicioResponse> realizarPago(@Valid @RequestBody PagoServicioRequest request) {
        log.info("[REST /pagos]: Solicitud POST de pago para cuenta: {}, idServicio: {}, idProducto: {}",
                request.getNumCuenta(), request.getIdServicio(), request.getIdProducto());
        PagoServicioResponse response = pagoServicioService.procesarPago(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/historial", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Consultar bitacora de evidencia de transacciones por cuenta",
        description = "Devuelve los registros de peticiones y respuestas intercambiadas con PuntoRed/GestoPago para una cuenta de 16 digitos."
    )
    public ResponseEntity<List<GestoPagoTransaccion>> consultarHistorialPorCuenta(
            @Valid @RequestBody CuentaConsultaRequest request) {
        return ResponseEntity.ok(pagoServicioService.obtenerHistorialPorCuenta(request.getNumeroCuenta()));
    }
}
