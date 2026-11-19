package com.proyecto.servicios.entity.cliente;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sesiones_login")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Long idSesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private UsuarioLogin usuario;

    @Column(name = "token_sesion")
    private String tokenSesion;

    // Bandera de estado activa/inactiva
    @Builder.Default
    @Column(name = "activa")
    private Boolean activa = true;

    // Se actualiza en cada petición para medir los 5 minutos de inactividad
    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "fecha_inicio", updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        this.fechaInicio = ahora;
        this.ultimoAcceso = ahora;
        if (this.activa == null) {
            this.activa = true;
        }
        if (this.fechaExpiracion == null) {
            // Expiración por inactividad inicial a 5 minutos
            this.fechaExpiracion = ahora.plusMinutes(5);
        }
    }

    /**
     * Valida si han transcurrido más de 5 minutos desde la última interacción.
     */
    public boolean haExpiradoPorInactividad(int minutosInactividadPermitidos) {
        if (!Boolean.TRUE.equals(this.activa)) {
            return true;
        }
        return LocalDateTime.now().isAfter(this.ultimoAcceso.plusMinutes(minutosInactividadPermitidos));
    }
}
