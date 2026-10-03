package com.proyecto.servicios.model.cliente;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
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
@Schema(description = "Solicitud para actualizacion parcial de cliente (PATCH)")
public class ClientePatchRequest {

    // --- Datos Personales modificables ---
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    @Schema(description = "Primer nombre", example = "Juan")
    private String nombre;

    @Schema(description = "Segundo nombre", example = "Carlos")
    private String segundoNombre;

    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    @Schema(description = "Apellido paterno", example = "Perez")
    private String apellidoPaterno;

    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
    @Schema(description = "Apellido materno", example = "Lopez")
    private String apellidoMaterno;

    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    @Schema(description = "Fecha de nacimiento", example = "1992-05-20")
    private LocalDate fechaNacimiento;

    @Pattern(regexp = "^$|^(?i)(Masculino|Femenino)$", message = "El sexo solo puede ser 'Masculino' o 'Femenino'")
    @Schema(description = "Sexo (Masculino o Femenino)", example = "Masculino")
    private String sexo;

    @Schema(description = "Nacionalidad", example = "Mexicana")
    private String nacionalidad;

    @Pattern(regexp = "^$|^(?i)(Soltero|Casado|Uni[oó]n libre|Viudo)$", message = "El estado civil debe ser Soltero, Casado, Union libre o Viudo")
    @Schema(description = "Estado civil (Soltero, Casado, Union libre, Viudo)", example = "Soltero")
    private String estadoCivil;

    // --- Datos de Contacto modificables ---
    @Email(message = "El formato del correo electronico no es valido")
    @Size(max = 100, message = "El correo electronico no debe exceder 100 caracteres")
    @Schema(description = "Correo electronico de contacto", example = "juan.perez@example.com")
    private String correoElectronico;

    @Pattern(regexp = "^\\d{10}$", message = "El telefono movil debe contener exactamente 10 digitos")
    @Schema(description = "Telefono movil a 10 digitos", example = "5599887766")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^\\d{10}$", message = "El telefono alternativo debe contener 10 digitos si es proporcionado")
    @Schema(description = "Telefono alternativo a 10 digitos", example = "5588776655")
    private String telefonoAlternativo;

    // --- Domicilio modificable ---
    @Valid
    private DomicilioDto domicilio;

    // --- Informacion Laboral modificable ---
    @Schema(description = "Puesto laboral u ocupacion", example = "Director de Ingenieria")
    private String ocupacion;

    @Schema(description = "Nombre de la empresa", example = "Tech Global")
    private String empresa;

    @Positive(message = "El ingreso mensual debe ser mayor a cero")
    @Schema(description = "Ingreso mensual", example = "65000.0")
    private Double ingresoMensual;

    // Bandera para baja lógica o reactivación
    @Schema(description = "Bandera de estado activo", example = "true")
    private Boolean activo;
}
