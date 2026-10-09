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
    @Pattern(regexp = "^[^#]+$", message = "La calle no debe contener el caracter '#'")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s.,-]+$", message = "La calle contiene caracteres no permitidos (no se permite '#')")
    @Schema(description = "Nombre de la calle (sin caracter '#')", example = "Av. Insurgentes Sur")
    private String calle;

    @NotBlank(message = "El numero exterior es obligatorio")
    @Size(max = 20, message = "El numero exterior no puede superar 20 caracteres")
    @Pattern(regexp = "^[^#]+$", message = "El numero exterior no debe contener el caracter '#'")
    @Pattern(regexp = "^[a-zA-Z0-9\\s-]+$", message = "El numero exterior solo debe contener numeros o letras (sin '#')")
    @Schema(description = "Numero exterior (sin caracter '#')", example = "1602")
    private String numeroExterior;

    @Pattern(regexp = "^$|^[^#]+$", message = "El numero interior no debe contener el caracter '#'")
    @Pattern(regexp = "^$|^[a-zA-Z0-9\\s-]+$", message = "El numero interior solo debe contener numeros o letras (sin '#')")
    @Schema(description = "Numero interior opcional (sin caracter '#')", example = "Piso 4")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(min = 2, max = 100, message = "La colonia debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[^#]+$", message = "La colonia no debe contener el caracter '#'")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s.,-]+$", message = "La colonia contiene caracteres no permitidos (no se permite '#')")
    @Schema(description = "Colonia o asentamiento (sin caracter '#')", example = "Credito Constructor")
    private String colonia;

    @NotBlank(message = "El municipio o alcaldia es obligatorio")
    @Size(min = 2, max = 100, message = "El municipio debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El municipio solo debe contener letras y espacios")
    @Schema(description = "Municipio o alcaldia (solo texto)", example = "Benito Juarez")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(min = 2, max = 100, message = "El estado debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El estado solo debe contener letras y espacios")
    @Schema(description = "Entidad federativa o estado (solo texto)", example = "Ciudad de Mexico")
    private String estado;

    @NotBlank(message = "El codigo postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El codigo postal solo debe contener numeros y exactamente 5 digitos")
    @Schema(description = "Codigo postal de 5 digitos (solo numeros)", example = "03940")
    private String codigoPostal;

    @NotBlank(message = "El pais es obligatorio")
    @Size(min = 2, max = 50, message = "El pais debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El pais solo debe contener letras y espacios")
    @Schema(description = "Pais de residencia (solo texto)", example = "Mexico")
    private String pais;
}
