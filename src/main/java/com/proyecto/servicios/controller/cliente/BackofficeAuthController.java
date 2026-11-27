package com.proyecto.servicios.controller.cliente;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.servicios.service.BackofficeAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth/backoffice")
@RequiredArgsConstructor
@Tag(name = "Autenticacion Backoffice", description = "Acceso exclusivo a la Consola Backoffice con 2FA (correo OTP o huella digital en móvil)")
public class BackofficeAuthController {

    private final BackofficeAuthService backofficeAuthService;

    @PostMapping("/login")
    @Operation(summary = "Paso 1: Validar credencial única y generar desafío 2FA")
    public ResponseEntity<Map<String, Object>> login(@RequestBody BackofficeLoginRequest request) {
        return ResponseEntity.ok(backofficeAuthService.iniciarLogin(
                request.getEmail(), request.getPassword(), request.getPlataforma()));
    }

    @PostMapping("/verificar-2fa")
    @Operation(summary = "Paso 2: Verificar 2FA (EMAIL_OTP en Web/Móvil o HUELLA solo en Móvil)")
    public ResponseEntity<Map<String, Object>> verificar2fa(@RequestBody BackofficeVerifyRequest request) {
        return ResponseEntity.ok(backofficeAuthService.verificarSegundoFactor(
                request.getChallengeId(),
                request.getMetodo(),
                request.getCodigo(),
                request.getBiometriaVerificada(),
                request.getPlataforma()));
    }

    @PostMapping("/reenviar-otp")
    @Operation(summary = "Reenviar código OTP de 6 dígitos al correo autorizado")
    public ResponseEntity<Map<String, Object>> reenviarOtp(@RequestBody BackofficeChallengeRequest request) {
        return ResponseEntity.ok(backofficeAuthService.reenviarOtp(request.getChallengeId()));
    }

    @GetMapping("/validar-sesion")
    @Operation(summary = "Validar token activo de Consola Backoffice")
    public ResponseEntity<Map<String, Object>> validarSesion(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        boolean valida = backofficeAuthService.validarToken(authHeader);
        return ResponseEntity.ok(Map.of("valida", valida));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión de Consola Backoffice")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        backofficeAuthService.cerrarSesion(authHeader);
        return ResponseEntity.ok(Map.of("mensaje", "Sesión Backoffice cerrada"));
    }

    @Data
    public static class BackofficeLoginRequest {
        private String email;
        private String password;
        private String plataforma = "WEB";
    }

    @Data
    public static class BackofficeVerifyRequest {
        private String challengeId;
        private String metodo;
        private String codigo;
        private Boolean biometriaVerificada;
        private String plataforma = "WEB";
    }

    @Data
    public static class BackofficeChallengeRequest {
        private String challengeId;
    }
}
