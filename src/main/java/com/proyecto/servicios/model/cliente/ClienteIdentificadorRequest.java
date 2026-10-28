package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Solicitud con identificador de cliente (RFC, CURP, correo electronico o numero de cuenta)")
public class ClienteIdentificadorRequest {

    @NotBlank(message = "El identificador del cliente es requerido")
    @Schema(description = "RFC, CURP, correo electronico o numero de cuenta", example = "PELJ920520HDFRRN09")
    private String identificador;
}
