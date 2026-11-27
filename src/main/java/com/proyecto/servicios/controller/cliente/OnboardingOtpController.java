package com.proyecto.servicios.controller.cliente;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.service.cliente.OnboardingOtpService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth/onboarding")
@RequiredArgsConstructor
@Validated
@Tag(name = "Verificacion de Correo Onboarding", description = "Envío y validación de código OTP de 6 dígitos para apertura de nuevas cuentas digitales")
public class OnboardingOtpController {

    private final OnboardingOtpService onboardingOtpService;

    @PostMapping("/enviar-otp")
    @Operation(summary = "Enviar código OTP de 6 dígitos al correo del nuevo cliente",
               description = "Valida que el correo, CURP y RFC no estén duplicados y envía un código OTP con vigencia de 5 minutos.")
    public ResponseEntity<Map<String, Object>> enviarOtp(@Valid @RequestBody EnviarOtpOnboardingRequest request) {
        Map<String, Object> result = onboardingOtpService.solicitarCodigoVerificacion(
                request.getCorreoElectronico(),
                request.getNombre(),
                request.getCurp(),
                request.getRfc()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/registrar-verificado")
    @Operation(summary = "Verificar código OTP y completar alta de cliente",
               description = "Verifica el código de 6 dígitos enviado al correo, crea el expediente/cuenta a 16 dígitos y envía correo de bienvenida.")
    public ResponseEntity<ClienteResponse> registrarVerificado(@Valid @RequestBody RegistroClienteVerificadoRequest request) {
        ClienteResponse creado = onboardingOtpService.verificarYRegistrarCliente(
                request.getCodigoOtp(),
                request.getRegistro()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Data
    public static class EnviarOtpOnboardingRequest {
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                 message = "Formato de correo electrónico inválido")
        private String correoElectronico;

        private String nombre;
        private String curp;
        private String rfc;
    }

    @Data
    public static class RegistroClienteVerificadoRequest {
        @NotBlank(message = "El código de verificación es obligatorio")
        @Pattern(regexp = "^\\d{6}$", message = "El código OTP debe contener exactamente 6 dígitos")
        private String codigoOtp;

        @NotNull(message = "Los datos de registro del cliente son obligatorios")
        @Valid
        private ClienteRegistroRequest registro;
    }
}
