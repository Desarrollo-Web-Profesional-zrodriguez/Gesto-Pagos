package com.proyecto.servicios.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
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

    // 3. No encontrado (Cliente no existe, Cuenta no existe, Recurso no encontrado) -> HTTP 404
    @ExceptionHandler({ClienteNoEncontradoException.class, CuentaNoEncontradaException.class, RecursoNoEncontradoException.class})
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

    // 7. Formato de JSON o tipos de datos no legibles -> HTTP 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarMensajeNoLegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String campo = "cuerpo";
        String detalle = "El cuerpo de la solicitud contiene tipos de datos o formatos no validos";

        Throwable cause = ex.getCause();
        if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife) {
            if (ife.getPath() != null && !ife.getPath().isEmpty()) {
                campo = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            }
            Object valor = ife.getValue();
            Class<?> targetType = ife.getTargetType();
            if (targetType != null && java.time.LocalDate.class.isAssignableFrom(targetType)) {
                detalle = String.format("El campo '%s' tiene un formato de fecha invalido ('%s'). Solo se acepta el formato 'AAAA-MM-DD' (ejemplo: 1992-05-20)", campo, valor);
            } else {
                detalle = String.format("El campo '%s' recibio un valor de formato no valido ('%s')", campo, valor);
            }
        } else if (cause != null && cause.getCause() instanceof IllegalArgumentException iae) {
            detalle = iae.getMessage();
            if (detalle.contains("'")) {
                int firstQuote = detalle.indexOf('\'');
                int secondQuote = detalle.indexOf('\'', firstQuote + 1);
                if (firstQuote != -1 && secondQuote != -1) {
                    campo = detalle.substring(firstQuote + 1, secondQuote);
                }
            }
        } else if (cause instanceof IllegalArgumentException iae) {
            detalle = iae.getMessage();
            if (detalle != null && detalle.contains("'")) {
                int firstQuote = detalle.indexOf('\'');
                int secondQuote = detalle.indexOf('\'', firstQuote + 1);
                if (firstQuote != -1 && secondQuote != -1) {
                    campo = detalle.substring(firstQuote + 1, secondQuote);
                }
            }
        } else if (ex.getMessage() != null && ex.getMessage().contains("fechaNacimiento")) {
            campo = "fechaNacimiento";
            detalle = "El campo 'fechaNacimiento' tiene un formato de fecha invalido. Solo se acepta el formato 'AAAA-MM-DD' (ejemplo: 1992-05-20)";
        }

        Map<String, String> detalles = new HashMap<>();
        detalles.put(campo, detalle);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Error de formato en los datos de entrada")
                .mensaje(detalle)
                .path(request.getRequestURI())
                .detallesValidacion(detalles)
                .build();

        log.warn("[FormatoInvalido]: Fallo en {}: {}", request.getRequestURI(), detalle);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 8. Violación de restricciones en parámetros/variables (Bean Validation) -> HTTP 400
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> manejarViolacionRestricciones(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errores = new HashMap<>();
        ex.getConstraintViolations().forEach(cv -> {
            String prop = cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : "parametro";
            if (prop.contains(".")) {
                prop = prop.substring(prop.lastIndexOf('.') + 1);
            }
            errores.put(prop, cv.getMessage());
        });

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Error de validacion en los parametros de la solicitud")
                .mensaje("Uno o mas parametros de la URL no cumplen con las reglas requeridas")
                .path(request.getRequestURI())
                .detallesValidacion(errores)
                .build();

        log.warn("[RestriccionInvalida]: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 8.1. Validación de parámetros en métodos de controladores (Spring Boot 3) -> HTTP 400
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> manejarHandlerMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        Map<String, String> errores = new HashMap<>();
        ex.getAllValidationResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> {
                errores.put(paramName != null ? paramName : "parametro", error.getDefaultMessage());
            });
        });

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Error de validacion en los parametros de la solicitud")
                .mensaje("Uno o mas parametros de la URL no cumplen con las reglas requeridas")
                .path(request.getRequestURI())
                .detallesValidacion(errores)
                .build();

        log.warn("[ValidacionParametro]: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 8.2. Error de tipo de dato en parámetro (ej. texto en vez de número) -> HTTP 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String mensaje = String.format("El parametro '%s' recibio un valor invalido ('%s'). Se esperaba un tipo de dato valido.",
                ex.getName(), ex.getValue());
        Map<String, String> detalles = new HashMap<>();
        detalles.put(ex.getName(), mensaje);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Tipo de dato invalido en parametro")
                .mensaje(mensaje)
                .path(request.getRequestURI())
                .detallesValidacion(detalles)
                .build();

        log.warn("[TipoInvalido]: {}", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 8.3. Ruta o recurso no encontrado -> HTTP 404
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoNoEncontrado(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Recurso no encontrado")
                .mensaje("La ruta solicitada no existe o no tiene un controlador asignado")
                .path(request.getRequestURI())
                .build();

        log.warn("[NoEncontrado]: Ruta inexistente {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 9. Excepción Genérica Inesperada -> HTTP 500
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
