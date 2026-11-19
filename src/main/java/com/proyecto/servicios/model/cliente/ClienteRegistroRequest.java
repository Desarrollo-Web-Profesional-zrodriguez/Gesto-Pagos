package com.proyecto.servicios.model.cliente;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.proyecto.servicios.validation.ExactamenteDosDecimales;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Solicitud para registrar nuevo cliente (Onboarding)")
public class ClienteRegistroRequest {

    @Schema(description = "Identificador opcional del cliente a actualizar (RFC, CURP, correo electronico o numero de cuenta). Si no se envia, se toma la CURP o RFC del cuerpo.", example = "PELJ920520HDFRRN09")
    private String identificador;

    // --- Datos Personales ---
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras y espacios, sin numeros ni caracteres como '#'")
    @Schema(description = "Primer nombre (solo texto)", example = "Juan")
    private String nombre;

    @Size(min = 3, max = 50, message = "El segundo nombre debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El segundo nombre solo debe contener letras y espacios, sin numeros ni caracteres como '#'")
    @Schema(description = "Segundo nombre opcional (solo texto)", example = "Carlos")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 3, max = 50, message = "El apellido paterno debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios, sin numeros ni caracteres como '#'")
    @Schema(description = "Apellido paterno (solo texto)", example = "Perez")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 3, max = 50, message = "El apellido materno debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido materno solo debe contener letras y espacios, sin numeros ni caracteres como '#'")
    @Schema(description = "Apellido materno (solo texto)", example = "Lopez")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.proyecto.servicios.config.FlexibleLocalDateDeserializer.class)
    @Schema(description = "Fecha de nacimiento en formato AAAA-MM-DD (debe cumplir mayoria de edad)", example = "1992-05-20")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$", message = "Formato de CURP invalido (debe tener exactamente 18 caracteres)")
    @Schema(description = "CURP oficial de 18 caracteres", example = "PELJ920520HDFRRN09")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-V1-9][A-Z1-9][0-9A]$", message = "Formato de RFC invalido (debe tener 12 o 13 caracteres)")
    @Schema(description = "RFC oficial con homoclave (12 o 13 caracteres)", example = "PELJ9205201A0")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^(?i)(Masculino|Femenino)$", message = "El sexo solo puede ser 'Masculino' o 'Femenino' (catalogo)")
    @Schema(description = "Sexo de catalogo (Masculino o Femenino)", example = "Masculino")
    private String sexo;

    @Schema(description = "ID de la nacionalidad del catalogo de base de datos (ejemplo: 1 para Mexicana)", example = "1")
    private Long idNacionalidad;

    @Schema(description = "Nombre o gentilicio de la nacionalidad (ejemplo: Mexicana)", example = "Mexicana")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(regexp = "^(?i)(Soltero|Casado|Uni[oó]n libre|Viudo)$", message = "El estado civil debe ser Soltero, Casado, Union libre o Viudo (catalogo)")
    @Schema(description = "Estado civil de catalogo (Soltero, Casado, Union libre, Viudo)", example = "Soltero")
    private String estadoCivil;

    // --- Datos de Contacto ---
    @NotBlank(message = "El correo electronico es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "El formato del correo electronico no es valido (ejemplo: usuario@dominio.com)")
    @Size(max = 100, message = "El correo electronico no debe exceder 100 caracteres")
    @Schema(description = "Correo electronico de contacto con formato valido", example = "juan.perez@example.com")
    private String correoElectronico;

    @NotBlank(message = "El telefono movil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El telefono movil solo debe contener numeros y exactamente 10 digitos")
    @Schema(description = "Telefono movil (solo numeros a 10 digitos)", example = "5512345678")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^\\d{10}$", message = "El telefono alternativo solo debe contener numeros y exactamente 10 digitos si es proporcionado")
    @Schema(description = "Telefono alternativo opcional (solo numeros a 10 digitos)", example = "5587654321")
    private String telefonoAlternativo;

    // --- Domicilio ---
    @Valid
    @NotNull(message = "El domicilio es obligatorio")
    private DomicilioDto domicilio;

    // --- Informacion Laboral ---
    @NotBlank(message = "La ocupacion es obligatoria")
    @Size(min = 3, max = 100, message = "La ocupacion debe tener entre 3 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "La ocupacion solo debe contener letras y espacios, sin numeros ni '#'")
    @Schema(description = "Ocupacion o puesto laboral (solo texto)", example = "Ingeniero de Software")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(min = 2, max = 100, message = "El nombre de la empresa debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[^#]+$", message = "El nombre de la empresa no debe contener el caracter '#'")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s.,&-]+$", message = "El nombre de la empresa contiene caracteres no permitidos (sin '#')")
    @Schema(description = "Nombre de la empresa (sin caracter '#')", example = "Tech Solutions")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @ExactamenteDosDecimales(message = "El ingreso mensual debe tener exactamente dos decimales (ejemplo: 100.00, no se acepta 100)")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.proyecto.servicios.config.StrictTwoDecimalNumberDeserializer.class)
    @Schema(description = "Ingreso mensual en numero directo con dos decimales sin comillas (ejemplo: 45000.00)", example = "45000.00")
    private BigDecimal ingresoMensual;

    // --- Saldo Inicial para apertura de cuenta ---
    @NotNull(message = "El saldo inicial es obligatorio")
    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    @ExactamenteDosDecimales(message = "El saldo inicial debe tener exactamente dos decimales (ejemplo: 100.00, no se acepta 100)")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.proyecto.servicios.config.StrictTwoDecimalNumberDeserializer.class)
    @Schema(description = "Saldo inicial asignado en numero directo con dos decimales sin comillas (ejemplo: 2500.00)", example = "2500.00")
    private BigDecimal saldoInicial;

    // --- Credenciales para Login y Biometria ---
    @NotBlank(message = "La contrasena para el acceso es obligatoria")
    @Size(min = 8, max = 64, message = "La contrasena debe tener entre 8 y 64 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s]).{8,64}$", 
        message = "La contrasena debe ser segura: incluir al menos una letra mayuscula, una minuscula, un numero y un caracter especial (@, #, $, *, etc.)"
    )
    @Schema(description = "Contrasena segura de acceso (minimo 8 caracteres, al menos una mayuscula, una minuscula, un numero y un caracter especial)", example = "PasswordSeguro123*")
    private String password;

    @Schema(description = "Vector de embeddings faciales generado por MediaPipe", example = "[0.12, -0.45, 0.89, -0.05, 0.67]")
    private String biometricoFacialEmbedding;
}
