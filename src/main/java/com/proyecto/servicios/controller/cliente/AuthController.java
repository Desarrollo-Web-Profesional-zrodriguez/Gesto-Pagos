package com.proyecto.servicios.controller.cliente;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<String> validarSesion(
            @Parameter(description = "Token de sesion en formato 'Bearer <token>'", example = "Bearer 550e8400-e29b-41d4-a716-446655440000") 
            @RequestHeader("Authorization") String token) {
        String tokenLimpio = token.replace("Bearer ", "").trim();
        authService.validarSesion(tokenLimpio);
        return ResponseEntity.ok("Sesion valida y activa. Inactividad reseteada.");
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion", description = "Finaliza la sesion activa del usuario (baja logica de la sesion).")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Token de sesion en formato 'Bearer <token>'", example = "Bearer 550e8400-e29b-41d4-a716-446655440000") 
            @RequestHeader("Authorization") String token) {
        String tokenLimpio = token.replace("Bearer ", "").trim();
        authService.logout(tokenLimpio);
        return ResponseEntity.noContent().build();
    }
}
