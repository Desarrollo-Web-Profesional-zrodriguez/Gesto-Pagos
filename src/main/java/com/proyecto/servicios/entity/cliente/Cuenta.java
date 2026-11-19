package com.proyecto.servicios.entity.cliente;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "cuentas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    private Long idCuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @Column(name = "numero_cuenta")
    private String numeroCuenta;

    @Builder.Default
    @Column(name = "tipo_cuenta")
    private String tipoCuenta = "DEBITO";

    @Builder.Default
    @Column(name = "estatus")
    private String estatus = "ACTIVA";

    @Column(name = "fecha_apertura")
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // Relación 1 a 1 con Saldo
    @OneToOne(mappedBy = "cuenta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Saldo saldo;

    @PrePersist
    public void prePersist() {
        this.fechaApertura = LocalDateTime.now();
        this.fechaModificacion = LocalDateTime.now();
        if (this.tipoCuenta == null) {
            this.tipoCuenta = "DEBITO";
        }
        if (this.estatus == null) {
            this.estatus = "ACTIVA";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
