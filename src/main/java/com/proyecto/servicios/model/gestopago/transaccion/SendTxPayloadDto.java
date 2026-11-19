package com.proyecto.servicios.model.gestopago.transaccion;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SendTxPayloadDto {

    @JsonProperty("idProducto")
    private Long idProducto;

    @JsonProperty("idServicio")
    private Long idServicio;

    @JsonProperty("telefono")
    private String telefono;

    @JsonProperty("referencia")
    private String referencia;

    @JsonProperty("horaLocal")
    private String horaLocal;

    @JsonProperty("montoPago")
    private BigDecimal montoPago;

    @JsonProperty("upc")
    private String upc;

    @JsonProperty("unidad")
    private String unidad;
}
