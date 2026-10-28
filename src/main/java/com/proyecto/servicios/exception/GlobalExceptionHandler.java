package com.proyecto.servicios.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.proyecto.servicios.model.GenericResponse;

import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Validaciones de formato en DTOs (@Valid) -> Código de Catálogo 100 (DATOS_ENTRADA_INVALIDOS)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> dtoValidation(MethodArgumentNotValidException errors) {
        Map<String, String> listError = new LinkedHashMap<>();
        for (FieldError fieldError : errors.getFieldErrors()) {
            listError.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        CatalogoCodigoError cat = CatalogoCodigoError.DATOS_ENTRADA_INVALIDOS;
        GenericResponse response = GenericResponse.builder()
                .codigo(cat.getCodigo())
                .codigoCatalogo(cat.name())
                .origen(cat.getOrigen())
                .mensaje(cat.getDescripcion() + ": " + listError)
                .detallesValidacion(listError)
                .build();
        return new ResponseEntity<>(response, cat.getHttpStatus());
    }

    /**
     * Cuenta bancaria no encontrada -> Código de Catálogo 201 (CUENTA_NO_ENCONTRADA)
     */
    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<GenericResponse> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        return construirRespuestaCatalogo(CatalogoCodigoError.CUENTA_NO_ENCONTRADA, ex.getMessage());
    }

    /**
     * Cliente no encontrado -> Código de Catálogo 301 (CLIENTE_NO_ENCONTRADO)
     */
    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<GenericResponse> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        return construirRespuestaCatalogo(CatalogoCodigoError.CLIENTE_NO_ENCONTRADO, ex.getMessage());
    }

    /**
     * Cliente ya registrado -> Código de Catálogo 303 (CLIENTE_YA_REGISTRADO)
     */
    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<GenericResponse> handleClienteYaRegistrado(ClienteYaRegistradoException ex) {
        return construirRespuestaCatalogo(CatalogoCodigoError.CLIENTE_YA_REGISTRADO, ex.getMessage());
    }

    /**
     * CURP duplicada -> Código de Catálogo 304 (CURP_DUPLICADA)
     */
    @ExceptionHandler(CurpDuplicadaException.class)
    public ResponseEntity<GenericResponse> handleCurpDuplicada(CurpDuplicadaException ex) {
        return construirRespuestaCatalogo(CatalogoCodigoError.CURP_DUPLICADA, ex.getMessage());
    }

    /**
     * RFC duplicado -> Código de Catálogo 305 (RFC_DUPLICADO)
     */
    @ExceptionHandler(RfcDuplicadoException.class)
    public ResponseEntity<GenericResponse> handleRfcDuplicado(RfcDuplicadoException ex) {
        return construirRespuestaCatalogo(CatalogoCodigoError.RFC_DUPLICADO, ex.getMessage());
    }

    /**
     * Recursos de catálogo (/productos) no encontrados -> Códigos 101, 102, 103
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<GenericResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return construirRespuestaCatalogo(ex.getCatalogoError(), ex.getMessage());
    }

    /**
     * Errores de seguridad, Login y 2FA -> Códigos 401, 402, 403
     */
    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<GenericResponse> handleAutenticacion(AutenticacionException ex) {
        return construirRespuestaCatalogo(ex.getCatalogoError(), ex.getMessage());
    }

    /**
     * Reglas de negocio (Saldo insuficiente, Cuenta/Cliente inactivo, monto no permitido, pago pendiente)
     * y rechazos de GestoPago. El HTTP status proviene del catálogo (422, 502, 202, ...).
     */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<GenericResponse> handleReglaNegocio(ReglaNegocioException ex) {
        CatalogoCodigoError cat = ex.getCatalogoError() != null
                ? ex.getCatalogoError()
                : CatalogoCodigoError.REGLA_NEGOCIO_GENERAL;
        Integer codigoFinal = ex.getCodigoPersonalizado() != null ? ex.getCodigoPersonalizado() : cat.getCodigo();

        GenericResponse response = GenericResponse.builder()
                .codigo(codigoFinal)
                .codigoGestoPago(ex.getCodigoGestoPago())
                .codigoCatalogo(cat.name())
                .origen(cat.getOrigen())
                .mensaje(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, cat.getHttpStatus());
    }

    /**
     * Solicitud duplicada (misma Idempotency-Key o misma operacion reciente) -> 409 Conflict
     */
    @ExceptionHandler(SolicitudDuplicadaException.class)
    public ResponseEntity<GenericResponse> handleSolicitudDuplicada(SolicitudDuplicadaException ex) {
        return construirRespuestaCatalogo(CatalogoCodigoError.SOLICITUD_DUPLICADA, ex.getMessage());
    }

    /**
     * Violacion de unicidad en BD (ultima barrera contra duplicados concurrentes) -> 409 sin detalles de SQL
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleIntegridadDatos(DataIntegrityViolationException ex) {
        log.warn("[Handler]: Violacion de integridad de datos: {}", ex.getClass().getSimpleName());
        return construirRespuestaCatalogo(CatalogoCodigoError.SOLICITUD_DUPLICADA, null);
    }

    /**
     * JSON malformado, tipos incorrectos o montos con formato invalido -> 400 sin reflejar el detalle del parser
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleCuerpoIlegible(HttpMessageNotReadableException ex) {
        return construirRespuestaCatalogo(CatalogoCodigoError.DATOS_ENTRADA_INVALIDOS,
                "El cuerpo de la peticion es invalido o tiene un formato incorrecto");
    }

    /**
     * Validaciones de parametros de ruta / cabeceras (@Validated) -> 400
     */
    @ExceptionHandler({ConstraintViolationException.class, MissingRequestHeaderException.class})
    public ResponseEntity<GenericResponse> handleParametrosInvalidos(Exception ex) {
        return construirRespuestaCatalogo(CatalogoCodigoError.DATOS_ENTRADA_INVALIDOS,
                "Uno o mas parametros de la peticion no son validos");
    }

    /**
     * Falla de un servicio externo no clasificado (catalogo, token) -> 502 sin exponer cuerpo del proveedor
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<GenericResponse> handleFeign(FeignException ex) {
        log.error("[Handler]: Error de comunicacion con servicio externo (HTTP {})", ex.status());
        return construirRespuestaCatalogo(CatalogoCodigoError.ERROR_COMUNICACION_GESTOPAGO, null);
    }

    /**
     * Fallback para cualquier otra RuntimeException no clasificada: el detalle va solo al log,
     * jamas a la respuesta (podria contener SQL, rutas, credenciales o datos del cliente).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GenericResponse> businessException(RuntimeException ex) {
        log.error("[Handler]: Error interno no controlado", ex);
        return construirRespuestaCatalogo(CatalogoCodigoError.ERROR_INTERNO, null);
    }

    private ResponseEntity<GenericResponse> construirRespuestaCatalogo(CatalogoCodigoError cat, String mensaje) {
        GenericResponse response = GenericResponse.builder()
                .codigo(cat.getCodigo())
                .codigoGestoPago(cat.getCodigoGestoPago())
                .codigoCatalogo(cat.name())
                .origen(cat.getOrigen())
                .mensaje(mensaje != null ? mensaje : cat.getDescripcion())
                .build();
        return new ResponseEntity<>(response, cat.getHttpStatus());
    }
}
