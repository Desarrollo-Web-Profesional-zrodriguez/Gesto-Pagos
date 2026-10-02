package com.proyecto.servicios.controller.cliente;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.servicios.exception.AutenticacionException;
import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;
import com.proyecto.servicios.service.cliente.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion y Biometria", description = "Login seguro (password cifrado + biometria MediaPipe) y control de sesiones con contador de 5 min de inactividad")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", description = "Autentica mediante contrasena cifrada y/o vector biometrico facial MediaPipe. Inicia contador de 5 minutos de inactividad.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/validar-sesion")
    @Operation(summary = "Validar sesion activa", description = "Verifica si el token sigue activo dentro de los 5 minutos de inactividad y renueva el tiempo.")
    public ResponseEntity<Map<String, Object>> validarSesion(
            @Parameter(description = "Token de sesion en formato 'Bearer <token>'", example = "Bearer 550e8400-e29b-41d4-a716-446655440000") 
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "Token de sesion directo (util en Swagger si no enviaste header)", example = "550e8400-e29b-41d4-a716-446655440000") 
            @RequestParam(value = "token", required = false) String tokenParam) {

        String tokenLimpio = extraerToken(authHeader, tokenParam);
        authService.validarSesion(tokenLimpio);

        return ResponseEntity.ok(Map.of(
            "valida", true,
            "mensaje", "Sesion valida y activa. Inactividad reseteada a 5 minutos."
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion", description = "Finaliza la sesion activa del usuario (baja logica de la sesion).")
    public ResponseEntity<Map<String, Object>> logout(
            @Parameter(description = "Token de sesion en formato 'Bearer <token>'", example = "Bearer 550e8400-e29b-41d4-a716-446655440000") 
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "Token de sesion directo (util en Swagger si no enviaste header)", example = "550e8400-e29b-41d4-a716-446655440000") 
            @RequestParam(value = "token", required = false) String tokenParam) {

        String tokenLimpio = extraerToken(authHeader, tokenParam);
        authService.logout(tokenLimpio);

        return ResponseEntity.ok(Map.of(
            "mensaje", "Sesion cerrada exitosamente."
        ));
    }

    private String extraerToken(String authHeader, String tokenParam) {
        if (authHeader != null && !authHeader.trim().isEmpty() && !authHeader.trim().equalsIgnoreCase("Bearer")) {
            return authHeader.replaceAll("(?i)^bearer\\s*", "").trim();
        }
        if (tokenParam != null && !tokenParam.trim().isEmpty()) {
            return tokenParam.replaceAll("(?i)^bearer\\s*", "").trim();
        }
        throw new AutenticacionException("Debe proporcionar el token de sesion en el encabezado 'Authorization: Bearer <token>' o en el parametro '?token=<token>'");
    }
}
