package com.proyecto.servicios.model.gestopago.transaccion;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.proyecto.servicios.config.StrictTwoDecimalNumberDeserializer;
import com.proyecto.servicios.validation.ExactamenteDosDecimales;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Solicitud previa para validar la referencia, formato y disponibilidad del servicio antes de confirmar un pago")
public class ValidarReferenciaRequest {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{16}$", message = "El numero de cuenta debe tener exactamente 16 digitos numericos")
    @JsonProperty("numCuenta")
    @JsonAlias({"numeroCuenta", "numCuenta"})
    @Schema(description = "Numero de cuenta bancaria del cliente a 16 digitos", example = "1000000001000000")
    private String numCuenta;

    @NotNull(message = "El idServicio es obligatorio")
    @Positive(message = "El idServicio debe ser un numero entero positivo mayor a cero")
    @JsonProperty("idServicio")
    @Schema(description = "Identificador del servicio obtenido del catalogo /productos", example = "76")
    private Long idServicio;

    @NotNull(message = "El idProducto es obligatorio")
    @Positive(message = "El idProducto debe ser un numero entero positivo mayor a cero")
    @JsonProperty("idProducto")
    @Schema(description = "Identificador del producto obtenido del catalogo /productos", example = "205")
    private Long idProducto;

    @Size(max = 100, message = "La referencia debe tener maximo 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "La referencia solo debe contener caracteres alfanumericos sin espacios ni simbolos especiales")
    @JsonProperty("referencia")
    @Schema(description = "Referencia alfanumerica del recibo o servicio a validar (obligatoria para pago de servicios tipoFront=2)", example = "25720381")
    private String referencia;

    @Pattern(regexp = "^$|^#?\\d{10}$", message = "El telefono debe contener 10 digitos numericos (o # seguido de 10 digitos para pines/entretenimiento)")
    @JsonProperty("telefono")
    @Schema(description = "Telefono de 10 digitos para recargas (tipoFront=1)", example = "5512345678")
    private String telefono;

    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @ExactamenteDosDecimales(message = "El monto debe tener exactamente dos decimales (ejemplo: 100.00, no se acepta 100)")
    @JsonDeserialize(using = StrictTwoDecimalNumberDeserializer.class)
    @JsonProperty("monto")
    @JsonAlias({"monto", "montoPago"})
    @Schema(description = "Monto opcional a prevalidar en formato estricto de dos decimales", example = "150.00")
    private BigDecimal monto;
}
