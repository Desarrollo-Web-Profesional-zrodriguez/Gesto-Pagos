package com.proyecto.servicios.service.cliente;

import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;

public interface AuthService {

    // Login por Password y/o Biometría MediaPipe
    LoginResponse login(LoginRequest request);

    // Validación de sesión activa y verificación de los 5 minutos de inactividad
    boolean validarSesion(String tokenSesion);

    // Cierre de sesión (baja lógica de la sesión)
    void logout(String tokenSesion);
}
