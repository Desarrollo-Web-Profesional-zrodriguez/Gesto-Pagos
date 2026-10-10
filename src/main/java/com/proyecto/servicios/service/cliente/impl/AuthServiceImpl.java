package com.proyecto.servicios.service.cliente.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyecto.servicios.entity.cliente.SesionLogin;
import com.proyecto.servicios.entity.cliente.UsuarioLogin;
import com.proyecto.servicios.exception.AutenticacionException;
import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;
import com.proyecto.servicios.repositorys.cliente.SesionLoginRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioLoginRepository;
import com.proyecto.servicios.service.cliente.AuthService;
import com.proyecto.servicios.service.cliente.util.BiometriaFacialUtil;
import com.proyecto.servicios.service.cliente.util.PasswordEncryptionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MINUTOS_INACTIVIDAD = 5;

    private final UsuarioLoginRepository usuarioLoginRepository;
    private final SesionLoginRepository sesionLoginRepository;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("[Auth]: Intento de login para usuario: {}", request.getUsername());

        UsuarioLogin usuario = usuarioLoginRepository.findByUsername(request.getUsername().trim().toLowerCase())
                .orElseThrow(() -> new AutenticacionException("El correo electronico no existe"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new AutenticacionException("El usuario se encuentra inactivo");
        }

        boolean autenticado = false;

        // 1. Verificación por Contraseña cifrada
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (PasswordEncryptionUtil.verificarPassword(request.getPassword(), usuario.getPasswordHash())) {
                autenticado = true;
                log.info("[Auth]: Autenticacion exitosa por contrasena para: {}", usuario.getUsername());
            } else {
                log.warn("[Auth]: Contrasena distinta a la registrada para: {}", usuario.getUsername());
                throw new AutenticacionException("La contrasena es distinta a la registrada");
            }
        }

        // 2. Verificación por Biometría Facial (MediaPipe Embeddings)
        if (!autenticado && request.getBiometricoFacialEmbedding() != null && !request.getBiometricoFacialEmbedding().trim().isEmpty()) {
            if (usuario.getBiometricoFacialEmbedding() == null) {
                throw new AutenticacionException("El usuario no tiene registrado perfil biometrico facial");
            }

            boolean coincidencia = BiometriaFacialUtil.coincideEmbedding(
                    usuario.getBiometricoFacialEmbedding(), 
                    request.getBiometricoFacialEmbedding()
            );

            if (coincidencia) {
                autenticado = true;
                log.info("[Auth]: Autenticacion exitosa por biometria facial MediaPipe para: {}", usuario.getUsername());
            } else {
                log.warn("[Auth]: Fallo la verificacion de embedding biometrico facial para: {}", usuario.getUsername());
                throw new AutenticacionException("El vector biometrico facial es distinto al registrado");
            }
        }

        if (!autenticado) {
            throw new AutenticacionException("Debe proporcionar contrasena o vector biometrico facial");
        }

        // Inactivar sesiones previas del usuario
        List<SesionLogin> sesionesPrevias = sesionLoginRepository.findByUsuarioIdUsuarioAndActivaTrue(usuario.getIdUsuario());
        for (SesionLogin sesionPrevia : sesionesPrevias) {
            sesionPrevia.setActiva(false);
        }
        sesionLoginRepository.saveAll(sesionesPrevias);

        // Crear nueva sesión con timeout de 5 minutos de inactividad
        LocalDateTime ahora = LocalDateTime.now();
        String tokenSesion = UUID.randomUUID().toString();

        SesionLogin nuevaSesion = SesionLogin.builder()
                .usuario(usuario)
                .tokenSesion(tokenSesion)
                .activa(true)
                .ultimoAcceso(ahora)
                .fechaInicio(ahora)
                .fechaExpiracion(ahora.plusMinutes(MINUTOS_INACTIVIDAD))
                .build();

        sesionLoginRepository.save(nuevaSesion);

        String nombreCliente = (usuario.getCliente() != null) 
                ? usuario.getCliente().getNombre() + " " + usuario.getCliente().getApellidoPaterno() 
                : usuario.getUsername();

        return LoginResponse.builder()
                .tokenSesion(tokenSesion)
                .activa(true)
                .tipoToken("Bearer")
                .fechaInicio(ahora)
                .fechaExpiracion(ahora.plusMinutes(MINUTOS_INACTIVIDAD))
                .minutosInactividadMaximos(MINUTOS_INACTIVIDAD)
                .idUsuario(usuario.getIdUsuario())
                .idCliente(usuario.getCliente() != null ? usuario.getCliente().getIdCliente() : null)
                .username(usuario.getUsername())
                .nombreCliente(nombreCliente)
                .mensaje("Inicio de sesion exitoso. Inactividad maxima permitida: 5 minutos.")
                .build();
    }

    @Override
    @Transactional
    public boolean validarSesion(String tokenSesion) {
        if (tokenSesion == null || tokenSesion.trim().isEmpty()) {
            throw new AutenticacionException("Token de sesion no proporcionado");
        }

        SesionLogin sesion = sesionLoginRepository.findByTokenSesion(tokenSesion.trim())
                .orElseThrow(() -> new AutenticacionException("Sesion no encontrada"));

        if (!Boolean.TRUE.equals(sesion.getActiva())) {
            throw new AutenticacionException("La sesion se encuentra inactiva o fue cerrada");
        }

        // Validar si superó los 5 minutos de inactividad
        if (sesion.haExpiradoPorInactividad(MINUTOS_INACTIVIDAD)) {
            sesion.setActiva(false);
            sesionLoginRepository.save(sesion);
            log.warn("[Auth]: Sesion {} cerrada automaticamente por 5 minutos de inactividad", tokenSesion);
            throw new AutenticacionException("La sesion ha expirado por inactividad (limite de 5 minutos excedido)");
        }

        // Renovar último acceso y extender la expiración otros 5 minutos
        LocalDateTime ahora = LocalDateTime.now();
        sesion.setUltimoAcceso(ahora);
        sesion.setFechaExpiracion(ahora.plusMinutes(MINUTOS_INACTIVIDAD));
        sesionLoginRepository.save(sesion);

        return true;
    }

    @Override
    @Transactional
    public void logout(String tokenSesion) {
        sesionLoginRepository.findByTokenSesion(tokenSesion.trim()).ifPresent(sesion -> {
            sesion.setActiva(false);
            sesionLoginRepository.save(sesion);
            log.info("[Auth]: Sesion {} finalizada voluntariamente", tokenSesion);
        });
    }
}
