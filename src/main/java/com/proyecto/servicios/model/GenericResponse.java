package com.proyecto.servicios.model;

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
@Schema(description = "Respuesta estandar basada en el Catalogo de Codigos de Respuesta y GestoPago")
public class GenericResponse {

    @Schema(description = "Codigo numerico del catalogo (1-99 si proviene de GestoPago, 100+ si es validacion interna)", example = "6")
    private Integer codigo;

    @Schema(description = "Codigo original devuelto por GestoPago cuando el rechazo proviene del proveedor (ej. '06', '05', '02')", example = "06")
    private String codigoGestoPago;

    @Schema(description = "Clave descriptiva del catalogo de errores", example = "GESTOPAGO_06_TRANSACCION_DUPLICADA")
    private String codigoCatalogo;

    @Schema(description = "Origen de la respuesta o rechazo (GESTOPAGO, VALIDACION_INTERNA, SEGURIDAD_2FA, COMUNICACION_GESTOPAGO)", example = "GESTOPAGO")
    private String origen;

    @Schema(description = "Mensaje detallado del resultado o motivo del rechazo", example = "Transaccion aplicada anteriormente | Autorización [100016171] | No es posible pagar la misma factura 2 veces")
    private String mensaje;

    @Schema(description = "Detalle de campos invalidos cuando falla la validacion de formato (@Valid)")
    private Map<String, String> detallesValidacion;

    public GenericResponse(Integer codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }
}
