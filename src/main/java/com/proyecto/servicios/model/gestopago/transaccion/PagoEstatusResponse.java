package com.proyecto.servicios.model.gestopago.transaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Estatus de una operacion de pago consultada por su UPC (sin datos sensibles del proveedor)")
public class PagoEstatusResponse {

    @Schema(description = "UPC de la operacion", example = "A9B8C7D6E5")
    private String upc;

    @Schema(description = "Estatus: PROCESANDO, EXITOSA, RECHAZADA, PENDIENTE_CONCILIACION o REVISION_MANUAL", example = "EXITOSA")
    private String estatus;

    @Schema(description = "Indica si el resultado es definitivo (EXITOSA/RECHAZADA) o aun se esta verificando", example = "true")
    private boolean definitivo;

    @Schema(description = "Explicacion del estatus para el cliente")
    private String mensaje;

    @Schema(description = "Monto de la operacion", example = "150.00")
    private BigDecimal monto;

    @Schema(description = "Numero de autorizacion cuando la operacion fue exitosa", example = "100074211")
    private String numAutorizacion;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;
}
