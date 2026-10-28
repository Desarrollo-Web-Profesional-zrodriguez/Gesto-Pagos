package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catálogo centralizado de códigos de respuesta y error para Pagos, Clientes, Cuentas y GestoPago.
 * 
 * - Los códigos 1 al 99 están reservados para los códigos oficiales devueltos por GestoPago (sendTx.do).
 * - Los códigos 100+ corresponden a validaciones internas del sistema (Catálogo, Cuenta, Cliente, 2FA, Comunicación).
 */
@Getter
@RequiredArgsConstructor
public enum CatalogoCodigoError {

    // =========================================================================
    // CÓDIGOS OFICIALES DE PROVEEDOR GESTOPAGO / PUNTORED (01 - 99)
    // =========================================================================
    GESTOPAGO_01_EXITOSO(1, "01", "GESTOPAGO", "Transaccion exitosa", HttpStatus.OK),
    GESTOPAGO_02_PARAMETROS_INVALIDOS(2, "02", "GESTOPAGO", "Referencia, telefono o parametros invalidos para el servicio", HttpStatus.BAD_REQUEST),
    GESTOPAGO_03_SERVICIO_NO_DISPONIBLE(3, "03", "GESTOPAGO", "Servicio o producto no disponible temporalmente en el proveedor", HttpStatus.BAD_REQUEST),
    GESTOPAGO_04_MONTO_INVALIDO(4, "04", "GESTOPAGO", "Monto invalido o fuera de rango permitido por el proveedor", HttpStatus.BAD_REQUEST),
    GESTOPAGO_05_SALDO_DISTRIBUIDOR_INSUFICIENTE(5, "05", "GESTOPAGO", "Saldo insuficiente en la bolsa del distribuidor GestoPago", HttpStatus.BAD_REQUEST),
    GESTOPAGO_06_TRANSACCION_DUPLICADA(6, "06", "GESTOPAGO", "Transaccion aplicada anteriormente (no es posible pagar la misma factura 2 veces)", HttpStatus.BAD_REQUEST),
    GESTOPAGO_07_TRANSACCION_EN_PROCESO(7, "07", "GESTOPAGO", "Transaccion en proceso de confirmacion en el proveedor", HttpStatus.BAD_REQUEST),
    GESTOPAGO_08_ERROR_PROVEEDOR_FINAL(8, "08", "GESTOPAGO", "Error de comunicacion con el proveedor final del servicio", HttpStatus.BAD_GATEWAY),
    GESTOPAGO_09_DISTRIBUIDOR_NO_AUTORIZADO(9, "09", "GESTOPAGO", "Distribuidor inactivo o no autorizado en GestoPago", HttpStatus.BAD_REQUEST),
    GESTOPAGO_RECHAZO_GENERICO(99, "99", "GESTOPAGO", "Transaccion rechazada por el proveedor GestoPago", HttpStatus.BAD_REQUEST),
    // Codigo 55 cuando el proveedor indica que la referencia no cumple el formato/digito verificador del servicio
    GESTOPAGO_55_REFERENCIA_INVALIDA(55, "55", "GESTOPAGO", "La referencia de pago es invalida para el servicio solicitado. Favor de revisar", HttpStatus.BAD_REQUEST),
    // Falla de integración (IP/origen no autorizado en el proveedor): no es culpa del cliente final
    GESTOPAGO_55_ORIGEN_NO_AUTORIZADO(55, "55", "GESTOPAGO", "El servicio de pagos no se encuentra disponible temporalmente. Intente mas tarde", HttpStatus.BAD_GATEWAY),

    // =========================================================================
    // 100 - 199: VALIDACIÓN DE DATOS DE ENTRADA Y CATÁLOGO DE PRODUCTOS
    // =========================================================================
    DATOS_ENTRADA_INVALIDOS(100, null, "VALIDACION_INTERNA", "Datos de entrada no validos en la peticion", HttpStatus.BAD_REQUEST),
    SERVICIO_NO_EXISTE(101, null, "VALIDACION_INTERNA", "El idServicio proporcionado no existe en el catalogo /productos", HttpStatus.NOT_FOUND),
    PRODUCTO_NO_EXISTE(102, null, "VALIDACION_INTERNA", "El idProducto proporcionado no existe o no pertenece al idServicio en /productos", HttpStatus.NOT_FOUND),
    CATALOGO_NO_DISPONIBLE(103, null, "VALIDACION_INTERNA", "El catalogo de productos (/productos) no se encuentra disponible", HttpStatus.SERVICE_UNAVAILABLE),
    TRANSACCION_NO_ENCONTRADA(104, null, "VALIDACION_INTERNA", "No se encontro ninguna transaccion con el UPC proporcionado", HttpStatus.NOT_FOUND),
    MONTO_NO_PERMITIDO(105, null, "VALIDACION_INTERNA", "El monto no corresponde con los valores permitidos para el producto", HttpStatus.UNPROCESSABLE_ENTITY),

    // =========================================================================
    // 200 - 299: VALIDACIONES DE CUENTA BANCARIA Y SALDOS DEL CLIENTE
    // =========================================================================
    CUENTA_NO_ENCONTRADA(201, null, "VALIDACION_INTERNA", "No se encontro ninguna cuenta bancaria con el numero de 16 digitos proporcionado", HttpStatus.NOT_FOUND),
    CUENTA_INACTIVA(202, null, "VALIDACION_INTERNA", "La cuenta bancaria del cliente no se encuentra activa", HttpStatus.BAD_REQUEST),
    SALDO_INSUFICIENTE(203, null, "VALIDACION_INTERNA", "Saldo disponible insuficiente en la cuenta bancaria del cliente", HttpStatus.UNPROCESSABLE_ENTITY),
    SOLICITUD_DUPLICADA(204, null, "VALIDACION_INTERNA", "Ya existe una solicitud equivalente registrada", HttpStatus.CONFLICT),

    // =========================================================================
    // 300 - 399: VALIDACIONES DE CLIENTE TITULAR Y ONBOARDING
    // =========================================================================
    CLIENTE_NO_ENCONTRADO(301, null, "VALIDACION_INTERNA", "No se encontro el cliente con los criterios proporcionados", HttpStatus.NOT_FOUND),
    CLIENTE_INACTIVO(302, null, "VALIDACION_INTERNA", "El cliente titular de la cuenta se encuentra inactivo", HttpStatus.BAD_REQUEST),
    CLIENTE_YA_REGISTRADO(303, null, "VALIDACION_INTERNA", "El cliente ya se encuentra registrado en el sistema", HttpStatus.CONFLICT),
    CURP_DUPLICADA(304, null, "VALIDACION_INTERNA", "Ya existe un cliente registrado con la CURP proporcionada", HttpStatus.CONFLICT),
    RFC_DUPLICADO(305, null, "VALIDACION_INTERNA", "Ya existe un cliente registrado con el RFC proporcionado", HttpStatus.CONFLICT),

    // =========================================================================
    // 400 - 499: SEGURIDAD, LOGIN Y SEGUNDO FACTOR DE AUTENTICACIÓN (2FA)
    // =========================================================================
    AUTENTICACION_2FA_FALLIDA(401, null, "SEGURIDAD_2FA", "Validacion 2FA o de credenciales fallida para el titular de la cuenta", HttpStatus.UNAUTHORIZED),
    SESION_EXPIRADA(402, null, "SEGURIDAD_2FA", "El token de sesion ha expirado por inactividad", HttpStatus.UNAUTHORIZED),
    USUARIO_INACTIVO(403, null, "SEGURIDAD_2FA", "El usuario de acceso del cliente se encuentra inactivo", HttpStatus.FORBIDDEN),

    // =========================================================================
    // 500 - 599: ERRORES TÉCNICOS, CIFRADO AES Y COMUNICACIÓN EXTERNA
    // =========================================================================
    ERROR_INTERNO(500, null, "SISTEMA_INTERNO", "Ocurrio un error interno. Intente nuevamente mas tarde", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_CIFRADO_AES(501, null, "SISTEMA_INTERNO", "Error al cifrar el payload con AES-256-CBC para GestoPago", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_COMUNICACION_GESTOPAGO(502, null, "COMUNICACION_GESTOPAGO", "Error de comunicacion al invocar el servicio sendTx.do de GestoPago", HttpStatus.BAD_GATEWAY),
    TOKEN_GESTOPAGO_INVALIDO(503, null, "COMUNICACION_GESTOPAGO", "No fue posible obtener un token valido de autenticacion para GestoPago", HttpStatus.BAD_GATEWAY),
    // 202: la operacion fue aceptada pero su resultado final aun se esta verificando con el proveedor
    PAGO_PENDIENTE_CONCILIACION(504, null, "COMUNICACION_GESTOPAGO", "El resultado del pago esta siendo verificado con el proveedor. No reintente la operacion", HttpStatus.ACCEPTED),

    // =========================================================================
    // 999: REGLA DE NEGOCIO GENERAL
    // =========================================================================
    REGLA_NEGOCIO_GENERAL(999, null, "VALIDACION_INTERNA", "Incumplimiento en regla de negocio", HttpStatus.BAD_REQUEST);

    private final Integer codigo;
    private final String codigoGestoPago;
    private final String origen;
    private final String descripcion;
    private final HttpStatus httpStatus;

    /**
     * Busca en el catálogo el código devuelto por GestoPago (ej. "02", "05", "06").
     * Si no está predefinido entre 01 y 09, devuelve GESTOPAGO_RECHAZO_GENERICO pero el llamador conserva el número real de GestoPago.
     */
    public static CatalogoCodigoError desdeCodigoGestoPago(String codigoGp) {
        if (codigoGp == null || codigoGp.trim().isEmpty()) {
            return GESTOPAGO_RECHAZO_GENERICO;
        }
        String normalizado = codigoGp.trim();
        for (CatalogoCodigoError item : values()) {
            if (item.codigoGestoPago != null && (item.codigoGestoPago.equalsIgnoreCase(normalizado)
                    || String.valueOf(item.codigo).equals(normalizado.replaceFirst("^0+(?!$)", "")))) {
                return item;
            }
        }
        return GESTOPAGO_RECHAZO_GENERICO;
    }
}
