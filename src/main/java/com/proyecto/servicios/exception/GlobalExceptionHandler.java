package com.proyecto.servicios.exception;

import java.util.HashMap;
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
     * En caso de que alguna validacion no se cumpla para algun DTO se tira MethodArgumentNotValidException
     * este metodo la cacha y regresa los mensajes de error para el cliente, con los message de cada campo.
     * @param errors
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> dtoValidation(MethodArgumentNotValidException errors) {
        GenericResponse genericResponse = new GenericResponse();
        Map<String, String> listError = new HashMap<>();
        for (FieldError fieldError : errors.getFieldErrors()) {
            listError.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        genericResponse.setCodigo(1);
        genericResponse.setMensaje("Datos no validos" + listError);
        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Manejador para excepciones de negocio personalizadas
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GenericResponse> businessException(RuntimeException ex) {
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setCodigo(1);
        genericResponse.setMensaje(ex.getMessage());
        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
    }
}
