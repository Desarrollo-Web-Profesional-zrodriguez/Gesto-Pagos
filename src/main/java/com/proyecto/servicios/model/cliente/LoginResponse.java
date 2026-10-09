package com.proyecto.servicios.model.cliente;

import java.time.LocalDateTime;

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
public class LoginResponse {
    private String tokenSesion;
    private Boolean activa;
    private String tipoToken;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaExpiracion;
    private Integer minutosInactividadMaximos;
    private Long idUsuario;
    private Long idCliente;
    private String username;
    private String nombreCliente;
    private String mensaje;
}
