package com.proyecto.servicios.model.cliente;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.proyecto.servicios.validation.ExactamenteDosDecimales;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Past;
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
@Schema(description = "Solicitud para actualizacion parcial de cliente (PATCH)")
public class ClientePatchRequest {

    @Schema(description = "Identificador del cliente a modificar (RFC, CURP, correo electronico o numero de cuenta)", example = "PELJ920520HDFRRN09")
    private String identificador;

    // --- Datos Personales modificables ---
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras y espacios, sin numeros ni '#'")
    @Schema(description = "Primer nombre (solo texto)", example = "Juan")
    private String nombre;

    @Size(min = 3, max = 50, message = "El segundo nombre debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El segundo nombre solo debe contener letras y espacios, sin numeros ni '#'")
    @Schema(description = "Segundo nombre opcional (solo texto)", example = "Carlos")
    private String segundoNombre;

    @Size(min = 3, max = 50, message = "El apellido paterno debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios, sin numeros ni '#'")
    @Schema(description = "Apellido paterno (solo texto)", example = "Perez")
    private String apellidoPaterno;

    @Size(min = 3, max = 50, message = "El apellido materno debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido materno solo debe contener letras y espacios, sin numeros ni '#'")
    @Schema(description = "Apellido materno (solo texto)", example = "Lopez")
    private String apellidoMaterno;

    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.proyecto.servicios.config.FlexibleLocalDateDeserializer.class)
    @Schema(description = "Fecha de nacimiento en formato AAAA-MM-DD", example = "1992-05-20")
    private LocalDate fechaNacimiento;

    @Pattern(regexp = "^$|^(?i)(Masculino|Femenino)$", message = "El sexo solo puede ser 'Masculino' o 'Femenino' (catalogo)")
    @Schema(description = "Sexo (Masculino o Femenino)", example = "Masculino")
    private String sexo;

    @Schema(description = "ID de la nacionalidad del catalogo de base de datos (ejemplo: 1)", example = "1")
    private Long idNacionalidad;

    @Schema(description = "Nombre o gentilicio de la nacionalidad (ejemplo: Mexicana)", example = "Mexicana")
    private String nacionalidad;

    @Pattern(regexp = "^$|^(?i)(Soltero|Casado|Uni[oó]n libre|Viudo)$", message = "El estado civil debe ser Soltero, Casado, Union libre o Viudo (catalogo)")
    @Schema(description = "Estado civil (Soltero, Casado, Union libre, Viudo)", example = "Soltero")
    private String estadoCivil;

    // --- Datos de Contacto modificables ---
    @Pattern(regexp = "^$|^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "El formato del correo electronico no es valido (ejemplo: usuario@dominio.com)")
    @Size(max = 100, message = "El correo electronico no debe exceder 100 caracteres")
    @Schema(description = "Correo electronico de contacto", example = "juan.perez@example.com")
    private String correoElectronico;

    @Pattern(regexp = "^$|^\\d{10}$", message = "El telefono movil solo debe contener numeros y exactamente 10 digitos")
    @Schema(description = "Telefono movil a 10 digitos (solo numeros)", example = "5599887766")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^\\d{10}$", message = "El telefono alternativo solo debe contener numeros y exactamente 10 digitos")
    @Schema(description = "Telefono alternativo a 10 digitos (solo numeros)", example = "5588776655")
    private String telefonoAlternativo;

    // --- Domicilio modificable ---
    @Valid
    private DomicilioDto domicilio;

    // --- Informacion Laboral modificable ---
    @Size(min = 3, max = 100, message = "La ocupacion debe tener entre 3 y 100 caracteres")
    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "La ocupacion solo debe contener letras y espacios, sin '#'")
    @Schema(description = "Puesto laboral u ocupacion (solo texto)", example = "Director de Ingenieria")
    private String ocupacion;

    @Size(min = 2, max = 100, message = "El nombre de la empresa debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^$|^[^#]+$", message = "El nombre de la empresa no debe contener el caracter '#'")
    @Pattern(regexp = "^$|^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s.,&-]+$", message = "El nombre de la empresa contiene caracteres no permitidos (sin '#')")
    @Schema(description = "Nombre de la empresa (sin caracter '#')", example = "Tech Global")
    private String empresa;

    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @ExactamenteDosDecimales(message = "El ingreso mensual debe tener exactamente dos decimales (ejemplo: 100.00, no se acepta 100)")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.proyecto.servicios.config.StrictTwoDecimalNumberDeserializer.class)
    @Schema(description = "Ingreso mensual en numero directo con dos decimales sin comillas", example = "65000.00")
    private BigDecimal ingresoMensual;

    // Bandera para baja lógica o reactivación
    @Schema(description = "Bandera de estado activo", example = "true")
    private Boolean activo;
}
