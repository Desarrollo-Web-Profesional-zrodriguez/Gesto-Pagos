package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Datos del domicilio del cliente")
public class DomicilioDto {

    @NotBlank(message = "La calle es obligatoria")
    @Size(min = 2, max = 150, message = "La calle debe tener entre 2 y 150 caracteres")
    @Schema(description = "Nombre de la calle", example = "Av. Insurgentes Sur")
    private String calle;

    @NotBlank(message = "El numero exterior es obligatorio")
    @Size(max = 20, message = "El numero exterior no puede superar 20 caracteres")
    @Schema(description = "Numero exterior", example = "1602")
    private String numeroExterior;

    @Schema(description = "Numero interior (opcional)", example = "Piso 4")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(min = 2, max = 100, message = "La colonia debe tener entre 2 y 100 caracteres")
    @Schema(description = "Colonia o asentamiento", example = "Credito Constructor")
    private String colonia;

    @NotBlank(message = "El municipio o alcaldia es obligatorio")
    @Size(min = 2, max = 100, message = "El municipio debe tener entre 2 y 100 caracteres")
    @Schema(description = "Municipio o alcaldia", example = "Benito Juarez")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(min = 2, max = 100, message = "El estado debe tener entre 2 y 100 caracteres")
    @Schema(description = "Entidad federativa o estado", example = "Ciudad de Mexico")
    private String estado;

    @NotBlank(message = "El codigo postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El codigo postal debe contener exactamente 5 digitos")
    @Schema(description = "Codigo postal de 5 digitos", example = "03940")
    private String codigoPostal;

    @NotBlank(message = "El pais es obligatorio")
    @Size(min = 2, max = 50, message = "El pais debe tener entre 2 y 50 caracteres")
    @Schema(description = "Pais de residencia", example = "Mexico")
    private String pais;
}
