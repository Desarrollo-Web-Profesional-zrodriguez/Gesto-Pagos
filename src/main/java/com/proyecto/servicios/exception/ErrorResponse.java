package com.proyecto.servicios.exception;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Estructura estandar de respuesta para errores de la API")
public class ErrorResponse {

    @Schema(description = "Marca de tiempo del error", example = "2026-10-02T18:48:36.306")
    private LocalDateTime timestamp;

    @Schema(description = "Codigo de estado HTTP", example = "400")
    private Integer status;

    @Schema(description = "Titulo descriptivo del error", example = "Error de validacion en los datos de entrada")
    private String error;

    @Schema(description = "Mensaje explicativo del error", example = "Existen campos que no cumplen con el formato o restricciones requeridas")
    private String mensaje;

    @Schema(description = "Ruta del recurso solicitado", example = "/clientes")
    private String path;

    @Schema(description = "Detalle especifico de validacion por campo", example = "{\"telefonoMovil\": \"El telefono movil solo debe contener numeros y exactamente 10 digitos\"}")
    private Map<String, String> detallesValidacion;
}