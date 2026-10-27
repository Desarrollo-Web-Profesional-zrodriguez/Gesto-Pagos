package com.proyecto.servicios.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.model.cliente.CuentaConsultaRequest;
import com.proyecto.servicios.model.gestopago.transaccion.PagoEstatusResponse;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioResponse;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.util.DatosSensibles;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
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
    private final com.proyecto.servicios.service.ComprobanteEmailService comprobanteEmailService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Realizar pago de servicio o recarga (sendTx)",
        description = "Valida cuenta de 16 digitos, cliente activo, saldo suficiente, 2FA (biometria facial MediaPipe / sesion / password), existencia de idServicio e idProducto en /productos, cifra la peticion con AES-256 y la envia a sendTx.do con el Bearer Token."
    )
    public ResponseEntity<PagoServicioResponse> realizarPago(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PagoServicioRequest request) {
        log.info("[REST /pagos]: Solicitud POST de pago para cuenta: {}, idServicio: {}, idProducto: {}",
                DatosSensibles.enmascararCuenta(request.getNumCuenta()), request.getIdServicio(), request.getIdProducto());
        PagoServicioResponse response = pagoServicioService.procesarPago(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/estatus/{upc}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Consultar el estatus de un pago por su UPC",
        description = "Permite conocer el desenlace de una operacion que respondio 202 (pendiente de conciliacion) sin reintentarla."
    )
    public ResponseEntity<PagoEstatusResponse> consultarEstatus(
            @PathVariable("upc") @Pattern(regexp = "^[A-Za-z0-9]{10}$", message = "El UPC debe tener 10 caracteres alfanumericos") String upc) {
        return ResponseEntity.ok(pagoServicioService.consultarEstatus(upc));
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

    @PostMapping(value = "/enviar-comprobante", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Enviar o reenviar comprobante digital de pago por correo (Brevo)",
        description = "Permite enviar o reenviar el comprobante HTML estilizado con el logo del servicio, autorizacion, importe, fecha, leyenda y telefono o referencia segun aplique."
    )
    public ResponseEntity<com.proyecto.servicios.model.email.ComprobanteEmailResult> enviarComprobante(
            @RequestBody com.proyecto.servicios.model.email.ComprobantePagoEmailDto request) {
        log.info("[REST /pagos/enviar-comprobante]: Solicitud de envio de comprobante a: {}, servicio: {}, aut: {}",
                request.getDestinatarioEmail(), request.getServicio(), request.getNumAutorizacion());
        com.proyecto.servicios.model.email.ComprobanteEmailResult resultado = comprobanteEmailService.enviarComprobantePago(request);
        return ResponseEntity.ok(resultado);
    }
}
