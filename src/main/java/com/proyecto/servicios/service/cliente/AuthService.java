package com.proyecto.servicios.service.cliente;

import java.util.Map;

import com.proyecto.servicios.model.cliente.BiometriaFacialRequest;
import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;

public interface AuthService {

    // Login por Password y/o Biometría MediaPipe
    LoginResponse login(LoginRequest request);

    // Validación de sesión activa y verificación de los 5 minutos de inactividad
    boolean validarSesion(String tokenSesion);

    // Cierre de sesión (baja lógica de la sesión)
    void logout(String tokenSesion);

    // Registrar, actualizar o desactivar biometría facial desde el perfil autenticado
    Map<String, Object> configurarBiometriaFacial(String tokenSesion, BiometriaFacialRequest request);

    // Consultar si el usuario autenticado cuenta con biometría facial registrada
    Map<String, Object> consultarEstadoBiometria(String tokenSesion);
}
