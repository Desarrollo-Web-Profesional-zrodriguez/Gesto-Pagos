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
@Schema(description = "Resultado de la validacion de referencia y resumen previo de la operacion de pago")
public class ValidarReferenciaResponse {

    @Schema(description = "Indica si la referencia/telefono y parametros del servicio son validos para continuar al pago", example = "true")
    private boolean valida;

    @Schema(description = "Mensaje descriptivo de la validacion", example = "Referencia validada correctamente para CFE")
    private String mensaje;

    @Schema(description = "Identificador del servicio", example = "76")
    private Long idServicio;

    @Schema(description = "Nombre del servicio o proveedor", example = "CFE")
    private String servicio;

    @Schema(description = "Identificador del producto", example = "205")
    private Long idProducto;

    @Schema(description = "Nombre del producto", example = "Pago de recibo CFE")
    private String producto;

    @Schema(description = "Tipo de flujo frontend en GestoPago (1=Recarga/PIN, 2=Pago de Servicio)", example = "2")
    private Integer tipoFront;

    @Schema(description = "Tipo de servicio en catalogo GestoPago (10/11=PIN/Entretenimiento)", example = "2")
    private Integer idCatTipoServicio;

    @Schema(description = "Tipo de referencia configurado por el catalogo", example = "a")
    private String tipoReferencia;

    @Schema(description = "Indica si el servicio valida digito verificador", example = "true")
    private Boolean hasDigitoVerificador;

    @Schema(description = "Leyenda o instrucciones devueltas por el catalogo para el servicio")
    private String legend;

    @Schema(description = "URL del logotipo del proveedor/servicio")
    private String urlLogo;

    @Schema(description = "URL de imagen de ayuda para localizar la referencia en el recibo")
    private String urlReferenciaAyuda;

    @Schema(description = "Referencia normalizada lista para enviar al pago", example = "25720381")
    private String referenciaNormalizada;

    @Schema(description = "Telefono normalizado listo para enviar al pago", example = "1111111111")
    private String telefonoNormalizado;

    @Schema(description = "Precio configurado en el catalogo (en tipoFront=2 representa comision de referencia)", example = "10.00")
    private BigDecimal precioCatalogo;

    @Schema(description = "Comision estimada por el catalogo", example = "10.00")
    private BigDecimal comisionEstimada;

    @Schema(description = "Monto validado de la operacion", example = "150.00")
    private BigDecimal montoValidado;

    @Schema(description = "Importe total estimado del cargo", example = "150.00")
    private BigDecimal totalEstimado;

    @Schema(description = "Numero de cuenta enmascarada que realizara el cargo", example = "************0000")
    private String numCuentaEnmascarada;

    @Schema(description = "Saldo disponible actual de la cuenta", example = "1000.00")
    private BigDecimal saldoDisponibleActual;

    @Schema(description = "Indica si el saldo disponible cubre el monto validado", example = "true")
    private Boolean saldoSuficiente;

    @Schema(description = "Fecha y hora de la verificacion")
    private LocalDateTime fechaVerificacion;
}
