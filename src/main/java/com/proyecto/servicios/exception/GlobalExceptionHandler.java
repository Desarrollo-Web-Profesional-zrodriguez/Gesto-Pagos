package com.proyecto.servicios.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.proyecto.servicios.model.GenericResponse;

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
     * Reglas de negocio (Saldo insuficiente, Cuenta/Cliente inactivo) y Rechazos directos de GestoPago (Códigos 02..99)
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
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Fallback para cualquier otra RuntimeException no clasificada
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GenericResponse> businessException(RuntimeException ex) {
        return construirRespuestaCatalogo(CatalogoCodigoError.REGLA_NEGOCIO_GENERAL, ex.getMessage());
    }

    private ResponseEntity<GenericResponse> construirRespuestaCatalogo(CatalogoCodigoError cat, String mensaje) {
        GenericResponse response = GenericResponse.builder()
                .codigo(cat.getCodigo())
                .codigoGestoPago(cat.getCodigoGestoPago())
                .codigoCatalogo(cat.name())
                .origen(cat.getOrigen())
                .mensaje(mensaje != null ? mensaje : cat.getDescripcion())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
