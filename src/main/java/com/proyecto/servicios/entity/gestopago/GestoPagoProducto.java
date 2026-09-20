package com.proyecto.servicios.entity.gestopago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "gestopago_productos")
@Getter 
@Setter 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class GestoPagoProducto {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "id_producto", nullable = false, unique = true)
    private Long idProducto;

    @Column(name = "id_servicio")
    private Long idServicio;

    @Column(name = "servicio", length = 150)
    private String servicio;

     @Column(name = "producto", length = 200)
    private String producto;

    @Column(name = "precio", precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(name = "tipo_front")
    private Integer tipoFront;

    @Column(name = "has_digito_verificador")
    private Boolean hasDigitoVerificador;

    @Column(name = "show_ayuda")
    private Boolean showAyuda;

    @Column(name = "tipo_referencia", length = 50)
    private String tipoReferencia;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist 
    @PreUpdate 
    void actualizarFecha() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
