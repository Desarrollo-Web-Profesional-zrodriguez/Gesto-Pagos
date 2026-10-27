package com.proyecto.servicios.model.email;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprobantePagoEmailDto {
    private String destinatarioEmail;
    private String destinatarioNombre;
    private String numAutorizacion;
    private String idTx;
    private String upc;
    private String numCuenta;
    private Long idServicio;
    private String servicio;
    private Long idProducto;
    private String producto;
    private String telefono;
    private String referencia;
    private BigDecimal montoPagado;
    private BigDecimal comision;
    private String legend;
    private String fechaProveedor;
    private String horaLocal;
    private String logoUrl;
    private Integer tipoFront;
}
