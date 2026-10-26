package com.proyecto.servicios.entity.gestopago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gestopago_transacciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GestoPagoTransaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion_local")
    private Long idTransaccionLocal;

    @Column(name = "upc", nullable = false, length = 20)
    private String upc;

    @Column(name = "numero_cuenta", nullable = false, length = 16)
    private String numeroCuenta;

    @Column(name = "id_servicio", nullable = false)
    private Long idServicio;

    @Column(name = "id_producto", nullable = false)
    private Long idProducto;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "referencia", nullable = false, length = 100)
    private String referencia;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "hora_local", nullable = false, length = 15)
    private String horaLocal;

    @Column(name = "unidad", nullable = false, length = 25)
    private String unidad;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "payload_cifrado", columnDefinition = "TEXT")
    private String payloadCifrado;

    @Column(name = "response_xml", columnDefinition = "TEXT")
    private String responseXml;

    @Column(name = "codigo_respuesta", length = 10)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", columnDefinition = "TEXT")
    private String mensajeRespuesta;

    @Column(name = "id_tx_gestopago", length = 50)
    private String idTxGestoPago;

    @Column(name = "num_autorizacion", length = 60)
    private String numAutorizacion;

    @Column(name = "comision", precision = 12, scale = 2)
    private BigDecimal comision;

    @Column(name = "pin", length = 250)
    private String pin;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;

    @Column(name = "estatus", nullable = false, length = 30)
    private String estatus;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }
}
