package com.proyecto.servicios.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Error de validación de Bean Validation (@Valid en DTOs) -> HTTP 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidaciones(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Error de validacion en los datos de entrada")
                .mensaje("Existen campos que no cumplen con el formato o restricciones requeridas")
                .path(request.getRequestURI())
                .detallesValidacion(errores)
                .build();

        log.warn("[Validacion]: Fallo en {} con {} errores: {}", request.getRequestURI(), errores.size(), errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. Conflictos de duplicidad (CURP, RFC, Cliente ya registrado) -> HTTP 409
    @ExceptionHandler({CurpDuplicadaException.class, RfcDuplicadoException.class, ClienteYaRegistradoException.class})
    public ResponseEntity<ErrorResponse> manejarConflictos(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflicto de unicidad")
                .mensaje(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("[Conflicto]: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 3. No encontrado (Cliente no existe, Cuenta no existe) -> HTTP 404
    @ExceptionHandler({ClienteNoEncontradoException.class, CuentaNoEncontradaException.class})
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Recurso no encontrado")
                .mensaje(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("[No Encontrado]: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 4. Reglas de negocio (Menor de edad, saldos invalidos, operaciones no permitidas) -> HTTP 400
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> manejarReglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Violacion de regla de negocio")
                .mensaje(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("[ReglaNegocio]: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 5. Autenticación (Credenciales incorrectas, biometría no coincide, timeout 5 min) -> HTTP 401
    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<ErrorResponse> manejarAutenticacion(AutenticacionException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Error de autenticacion")
                .mensaje(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("[Autenticacion]: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 6. Parámetros o cabeceras requeridas faltantes -> HTTP 400
    @ExceptionHandler({MissingRequestHeaderException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> manejarFaltantes(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Parametro o cabecera requerida faltante")
                .mensaje(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        log.warn("[ParametroFaltante]: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 7. Excepción Genérica Inesperada -> HTTP 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarGenerico(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Error interno del servidor")
                .mensaje("Ha ocurrido un error inesperado al procesar la solicitud")
                .path(request.getRequestURI())
                .build();

        log.error("[ErrorServidor]: Ha ocurrido un error no controlado en {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
