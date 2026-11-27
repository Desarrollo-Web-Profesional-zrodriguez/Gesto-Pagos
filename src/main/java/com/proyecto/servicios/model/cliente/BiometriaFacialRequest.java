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
@Schema(description = "Solicitud para activar, actualizar o desactivar el reconocimiento facial del titular")
public class BiometriaFacialRequest {

    @NotBlank(message = "Ingresa tu contrasena actual para confirmar la operacion")
    @Schema(description = "Contrasena actual de la cuenta para autorizar el cambio biometrico", example = "PasswordSeguro123*")
    private String passwordActual;

    @Schema(description = "Firma biometrica facial normalizada", example = "[0.142,-0.089,0.231]")
    private String biometricoFacialEmbedding;

    @Schema(description = "Indica si se desea eliminar el registro facial actual", example = "false")
    private Boolean eliminar;
}
