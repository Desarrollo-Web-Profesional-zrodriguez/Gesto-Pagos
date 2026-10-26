package com.proyecto.servicios.model.gestopago.transaccion;

import java.math.BigDecimal;

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
@Schema(description = "Respuesta de pago de servicio o recarga procesada con GestoPago (sendTx)")
public class PagoServicioResponse {

    @Schema(description = "Codigo de resultado de GestoPago ('01' indica Pago Exitoso)", example = "01")
    private String codigo;

    @Schema(description = "Mensaje descriptivo de la transaccion", example = "Pago Exitoso")
    private String mensaje;

    @Schema(description = "Detalle devuelto por el proveedor GestoPago", example = "Operacion realizada con exito")
    private String detalleProveedor;

    @Schema(description = "Identificador unico de transaccion en GestoPago (ID_TX)", example = "291620941")
    private String idTx;

    @Schema(description = "Numero de autorizacion de la transaccion (NUM_AUTORIZACION)", example = "100074211")
    private String numAutorizacion;

    @Schema(description = "Codigo unico de pago de 10 caracteres (UPC)", example = "A9B8C7D6E5")
    private String upc;

    @Schema(description = "Numero de cuenta del cliente (16 digitos)", example = "1000000001000000")
    private String numCuenta;

    @Schema(description = "Identificador del servicio pagado", example = "76")
    private Long idServicio;

    @Schema(description = "Nombre del servicio pagado", example = "CFE")
    private String servicio;

    @Schema(description = "Identificador del producto pagado", example = "205")
    private Long idProducto;

    @Schema(description = "Nombre del producto pagado", example = "Pago de recibo CFE")
    private String producto;

    @Schema(description = "Telefono enviado en la operacion", example = "5512345678")
    private String telefono;

    @Schema(description = "Referencia aplicada al pago", example = "25720381")
    private String referencia;

    @Schema(description = "Monto descontado al cliente", example = "150.00")
    private BigDecimal montoPagado;

    @Schema(description = "Comision reportada por el proveedor", example = "6.00")
    private BigDecimal comision;

    @Schema(description = "Saldo disponible del cliente antes del pago", example = "2500.00")
    private BigDecimal saldoAnterior;

    @Schema(description = "Saldo disponible del cliente despues del pago", example = "2350.00")
    private BigDecimal saldoDisponibleActual;

    @Schema(description = "Saldo contable del cliente despues del pago", example = "2350.00")
    private BigDecimal saldoContableActual;

    @Schema(description = "PIN o Folio en caso de productos canjeables / entretenimiento", example = "40919958")
    private String pin;

    @Schema(description = "Leyenda o instrucciones del proveedor para el comprobante", example = "Conserve su comprobante de pago")
    private String legend;

    @Schema(description = "Hora local enviada en formato yyyyMMddHHmmss", example = "20261023184500")
    private String horaLocal;

    @Schema(description = "Fecha de operacion reportada por GestoPago", example = "23/Oct/2026 18:45:01")
    private String fechaProveedor;
}
