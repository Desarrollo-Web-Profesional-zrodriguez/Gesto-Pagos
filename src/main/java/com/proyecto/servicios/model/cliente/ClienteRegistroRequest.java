package com.proyecto.servicios.model.cliente;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

    // --- Datos Personales ---
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    @Schema(description = "Primer nombre", example = "Juan")
    private String nombre;

    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    @Schema(description = "Segundo nombre (opcional)", example = "Carlos")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 3, max = 50, message = "El apellido paterno debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    @Schema(description = "Apellido paterno", example = "Perez")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 3, max = 50, message = "El apellido materno debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
    @Schema(description = "Apellido materno", example = "Lopez")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    @Schema(description = "Fecha de nacimiento (debe cumplir mayoria de edad)", example = "1992-05-20")
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
    @Pattern(regexp = "^(?i)(Masculino|Femenino)$", message = "El sexo solo puede ser 'Masculino' o 'Femenino'")
    @Schema(description = "Sexo (Masculino o Femenino)", example = "Masculino")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Schema(description = "Nacionalidad", example = "Mexicana")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(regexp = "^(?i)(Soltero|Casado|Uni[oó]n libre|Viudo)$", message = "El estado civil debe ser Soltero, Casado, Union libre o Viudo")
    @Schema(description = "Estado civil (Soltero, Casado, Union libre, Viudo)", example = "Soltero")
    private String estadoCivil;

    // --- Datos de Contacto ---
    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "El formato del correo electronico no es valido")
    @Size(max = 100, message = "El correo electronico no debe exceder 100 caracteres")
    @Schema(description = "Correo electronico de contacto", example = "juan.perez@example.com")
    private String correoElectronico;

    @NotBlank(message = "El telefono movil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El telefono movil debe contener exactamente 10 digitos")
    @Schema(description = "Telefono movil a 10 digitos", example = "5512345678")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^\\d{10}$", message = "El telefono alternativo debe contener 10 digitos si es proporcionado")
    @Schema(description = "Telefono alternativo opcional a 10 digitos", example = "5587654321")
    private String telefonoAlternativo;

    // --- Domicilio ---
    @Valid
    @NotNull(message = "El domicilio es obligatorio")
    private DomicilioDto domicilio;

    // --- Informacion Laboral ---
    @NotBlank(message = "La ocupacion es obligatoria")
    @Schema(description = "Ocupacion o puesto laboral", example = "Ingeniero de Software")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Schema(description = "Nombre de la empresa", example = "Tech Solutions")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @Positive(message = "El ingreso mensual debe ser mayor a cero")
    @Schema(description = "Ingreso mensual en moneda nacional", example = "45000.0")
    private Double ingresoMensual;

    // --- Saldo Inicial para apertura de cuenta ---
    @PositiveOrZero(message = "El saldo inicial no puede ser negativo")
    @Schema(description = "Saldo inicial asignado a la cuenta bancaria", example = "2500.0")
    private Double saldoInicial;

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
