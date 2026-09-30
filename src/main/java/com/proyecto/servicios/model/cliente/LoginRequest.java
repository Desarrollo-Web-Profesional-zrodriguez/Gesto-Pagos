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
@Schema(description = "Solicitud de inicio de sesion (Login)")
public class LoginRequest {

    @NotBlank(message = "El usuario o correo es obligatorio")
    @Schema(description = "Nombre de usuario o correo electronico", example = "juan.perez@example.com")
    private String username;

    // Opcional si el acceso se realiza por biometría facial directa
    @Schema(description = "Contrasena del usuario", example = "PasswordSeguro123*")
    private String password;

    // Vector/Embedding facial generado por MediaPipe (cadena JSON de flotantes)
    @Schema(description = "Vector de embedding facial de MediaPipe", example = "[0.12, -0.45, 0.89, -0.05, 0.67]")
    private String biometricoFacialEmbedding;
}
