package com.proyecto.servicios.entity.cliente;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "saldos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Saldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_saldo")
    private Long idSaldo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuenta")
    private Cuenta cuenta;

    @Builder.Default
    @Column(name = "saldo_disponible")
    private Double saldoDisponible = 0.0;

    @Builder.Default
    @Column(name = "saldo_contable")
    private Double saldoContable = 0.0;

    @Column(name = "fecha_ultima_actualizacion")
    private LocalDateTime fechaUltimaActualizacion;

    @PrePersist
    @PreUpdate
    public void actualizarFecha() {
        this.fechaUltimaActualizacion = LocalDateTime.now();
        if (this.saldoDisponible == null) {
            this.saldoDisponible = 0.0;
        }
        if (this.saldoContable == null) {
            this.saldoContable = 0.0;
        }
    }
}
