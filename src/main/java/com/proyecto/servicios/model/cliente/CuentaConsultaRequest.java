package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Solicitud para consulta de cuenta bancaria o saldo mediante numero de cuenta")
public class CuentaConsultaRequest {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{16}$", message = "El numero de cuenta debe tener exactamente 16 digitos numericos")
    @Schema(description = "Numero de cuenta bancaria a 16 digitos", example = "8324433882000000")
    private String numeroCuenta;
}
