package com.proyecto.servicios.entity.cliente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long idCliente;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "segundo_nombre")
    private String segundoNombre;

    @Column(name = "apellido_paterno")
    private String apellidoPaterno;

    @Column(name = "apellido_materno")
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "curp")
    private String curp;

    @Column(name = "rfc")
    private String rfc;

    @Column(name = "sexo")
    private String sexo;

    @Column(name = "nacionalidad")
    private String nacionalidad;

    @Column(name = "estado_civil")
    private String estadoCivil;

    @Column(name = "correo_electronico")
    private String correoElectronico;

    @Column(name = "telefono_movil")
    private String telefonoMovil;

    @Column(name = "telefono_alternativo")
    private String telefonoAlternativo;

    @Column(name = "ocupacion")
    private String ocupacion;

    @Column(name = "empresa")
    private String empresa;

    @Column(name = "ingreso_mensual")
    private Double ingresoMensual;

    @Builder.Default
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // Relación 1 a N con Domicilios
    @Builder.Default
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Domicilio> domicilios = new ArrayList<>();

    // Relación 1 a N con Cuentas
    @Builder.Default
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Cuenta> cuentas = new ArrayList<>();

    // Relación 1 a 1 con UsuarioLogin
    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UsuarioLogin usuarioLogin;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaModificacion = LocalDateTime.now();
        if (this.activo == null) {
            this.activo = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
