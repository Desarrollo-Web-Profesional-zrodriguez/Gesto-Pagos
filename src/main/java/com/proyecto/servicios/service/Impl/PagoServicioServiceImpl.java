package com.proyecto.servicios.service.Impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.SesionLogin;
import com.proyecto.servicios.entity.cliente.UsuarioLogin;
import com.proyecto.servicios.entity.gestopago.EstatusTransaccion;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.exception.AutenticacionException;
import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.email.ComprobanteEmailResult;
import com.proyecto.servicios.model.email.ComprobantePagoEmailDto;
import com.proyecto.servicios.model.gestopago.catalogo.ProductCategorizedResponse;
import com.proyecto.servicios.model.gestopago.catalogo.ProductItemDto;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;
import com.proyecto.servicios.model.gestopago.transaccion.PagoEstatusResponse;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioResponse;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxPayloadDto;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.SesionLoginRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioLoginRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.service.ComprobanteEmailService;
import com.proyecto.servicios.service.GestoPagoSendTxGateway;
import com.proyecto.servicios.service.PagoLedgerService;
import com.proyecto.servicios.service.PagoLedgerService.Reserva;
import com.proyecto.servicios.service.PagoLedgerService.SaldosFinales;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.service.ProductoService;
import com.proyecto.servicios.service.cliente.util.BiometriaFacialUtil;
import com.proyecto.servicios.service.cliente.util.PasswordEncryptionUtil;
import com.proyecto.servicios.util.AesEncryption;
import com.proyecto.servicios.util.DatosSensibles;
import com.proyecto.servicios.util.LogoServicioResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orquesta el flujo completo de un pago:
 * validación (catálogo, monto, cuenta, cliente, 2FA) -> cifrado -> reserva atómica del saldo ->
 * llamada a sendTx (fuera de transacción) -> cierre según veredicto (confirmar / liberar / conciliar).
 *
 * <p>No es transaccional a nivel global a propósito: cada paso monetario es una transacción corta
 * en {@link PagoLedgerService}, de modo que nunca se mantienen locks ni conexiones durante la llamada HTTP externa.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PagoServicioServiceImpl implements PagoServicioService {

    private static final DateTimeFormatter HORA_LOCAL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final int MINUTOS_INACTIVIDAD_SESION = 5;

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioLoginRepository usuarioLoginRepository;
    private final SesionLoginRepository sesionLoginRepository;
    private final ProductoService productoService;
    private final PagoLedgerService ledger;
    private final GestoPagoSendTxGateway sendTxGateway;
    private final GestoPagoTransaccionRepository transaccionRepository;
    private final ComprobanteEmailService comprobanteEmailService;
    private final LogoServicioResolver logoServicioResolver;
    private final ObjectMapper objectMapper;

    @Value("${gestopago.aes.secret-key}")
    private String aesSecretKey;

    @Value("${gestopago.aes.iv}")
    private String aesIv;

    @Value("${gestopago.service.unidad:083}")
    private String unidadNegocio;

    /** Rango permitido para productos de monto abierto (precio 0 / nulo en el catálogo). */
    @Value("${gestopago.pago.monto-minimo:1.00}")
    private BigDecimal montoMinimoAbierto = new BigDecimal("1.00");

    @Value("${gestopago.pago.monto-maximo:10000.00}")
    private BigDecimal montoMaximoAbierto = new BigDecimal("10000.00");

    @Override
    public PagoServicioResponse procesarPago(PagoServicioRequest request, String idempotencyKey) {
        String numCuenta = request.getNumCuenta().trim();
        String cuentaLog = DatosSensibles.enmascararCuenta(numCuenta);
        String claveIdempotencia = normalizarIdempotencyKey(idempotencyKey);
        log.info("[PagoServicio]: Iniciando solicitud de pago cuenta={}, idServicio={}, idProducto={}, monto={}",
                cuentaLog, request.getIdServicio(), request.getIdProducto(), request.getMonto());

        // 1. Servicio y producto deben existir en el catálogo (/productos)
        ProductItemDto productoCatalogo = validarServicioYProductoEnCatalogo(request.getIdServicio(), request.getIdProducto());

        // 2. El monto debe ser el permitido por el catálogo (nunca se confía en el monto del cliente)
        BigDecimal montoPago = request.getMonto().setScale(2, RoundingMode.HALF_UP);
        validarMontoContraCatalogo(productoCatalogo, montoPago);

        // 3. Cuenta existente y activa
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(cuentaLog));
        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new ReglaNegocioException(CatalogoCodigoError.CUENTA_INACTIVA, "La cuenta " + cuentaLog + " no se encuentra activa");
        }

        // 4. Cliente titular existente y activo
        Cliente cliente = clienteRepository.findByCuentasNumeroCuenta(numCuenta)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontro cliente titular asociado a la cuenta: " + cuentaLog));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException(CatalogoCodigoError.CLIENTE_INACTIVO, "El cliente titular de la cuenta se encuentra inactivo");
        }

        // 5. Identidad del titular: 2FA (biometría facial / sesión activa / contraseña)
        validarAutenticacion2FA(cliente, request.getCodigo2fa());

        // 6. Construir y cifrar el payload ANTES de tocar el saldo (si falla, no hay nada que revertir)
        String horaLocal = LocalDateTime.now().format(HORA_LOCAL_FORMATTER);
        String upc = RandomStringUtils.randomAlphanumeric(10);
        String telefono = request.getTelefono().trim();
        String referencia = request.getReferencia().trim();

        SendTxPayloadDto payload = SendTxPayloadDto.builder()
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .telefono(telefono)
                .referencia(referencia)
                .horaLocal(horaLocal)
                .montoPago(montoPago)
                .upc(upc)
                .unidad(unidadNegocio)
                .build();

        String jsonPayload;
        String encryptedPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
            encryptedPayload = AesEncryption.AESCipher(AesEncryption.Mode.ENCRYPT.name(), jsonPayload, aesIv, aesSecretKey);
        } catch (Exception e) {
            log.error("[PagoServicio]: Fallo al cifrar el payload UPC={} ({})", upc, e.getClass().getSimpleName());
            throw new ReglaNegocioException(CatalogoCodigoError.ERROR_CIFRADO_AES, CatalogoCodigoError.ERROR_CIFRADO_AES.getDescripcion());
        }
        if (encryptedPayload == null || encryptedPayload.isBlank()) {
            throw new ReglaNegocioException(CatalogoCodigoError.ERROR_CIFRADO_AES, CatalogoCodigoError.ERROR_CIFRADO_AES.getDescripcion());
        }

        // 7. Reserva atómica: bloqueo de saldo + anti-duplicados + saldo suficiente + retención + registro PROCESANDO
        GestoPagoTransaccion registro = GestoPagoTransaccion.builder()
                .upc(upc)
                .numeroCuenta(numCuenta)
                .idServicio(request.getIdServicio())
                .idProducto(request.getIdProducto())
                .telefono(telefono)
                .referencia(referencia)
                .monto(montoPago)
                .horaLocal(horaLocal)
                .unidad(unidadNegocio)
                .payloadJson(jsonPayload)
                .payloadCifrado(encryptedPayload)
                .idempotencyKey(claveIdempotencia)
                .huella(calcularHuella(numCuenta, request, telefono, referencia, montoPago))
                .estatus(EstatusTransaccion.PROCESANDO.name())
                .build();
        Reserva reserva = ledger.reservar(registro);

        // 8. Llamada al proveedor (sin transacción abierta). Devuelve un veredicto, nunca lanza.
        RespuestaSendTx respuesta = sendTxGateway.ejecutar(encryptedPayload, upc);

        return switch (respuesta.veredicto()) {
            case EXITO -> cerrarComoExitosa(request, cliente, productoCatalogo, numCuenta, upc, horaLocal,
                    montoPago, telefono, referencia, reserva, respuesta);
            case RECHAZADO -> {
                ledger.liberarRetencion(numCuenta, upc, respuesta);
                throw excepcionPorRechazo(respuesta);
            }
            case INDETERMINADO -> {
                ledger.marcarPendiente(upc, respuesta);
                throw new ReglaNegocioException(CatalogoCodigoError.PAGO_PENDIENTE_CONCILIACION,
                        CatalogoCodigoError.PAGO_PENDIENTE_CONCILIACION.getDescripcion()
                                + ". Referencia de seguimiento (UPC): " + upc);
            }
        };
    }

    private PagoServicioResponse cerrarComoExitosa(PagoServicioRequest request, Cliente cliente, ProductItemDto productoCatalogo,
                                                    String numCuenta, String upc, String horaLocal, BigDecimal montoPago,
                                                    String telefono, String referencia, Reserva reserva, RespuestaSendTx respuesta) {
        SendTxXmlResponse xml = respuesta.parsed();

        // Fase 2: el proveedor ya aplicó el cargo. Si el commit local falla, NO se revierte nada:
        // la transacción queda PROCESANDO con la retención y el job de conciliación la cerrará.
        BigDecimal saldoContable = null;
        BigDecimal saldoDisponible = reserva.saldoDisponibleRetenido();
        try {
            SaldosFinales finales = ledger.confirmarExito(numCuenta, upc, respuesta);
            saldoContable = finales.saldoContable();
            saldoDisponible = finales.saldoDisponible();
        } catch (Exception e) {
            log.error("[PagoServicio CRITICO]: Proveedor aplico UPC={} pero no se pudo confirmar localmente ({}). Se conciliara.",
                    upc, e.getClass().getSimpleName());
        }

        BigDecimal comisionProveedor = parsearDecimalSeguro(xml.getComision());
        String idTx = xml.getIdTx() != null ? xml.getIdTx() : (xml.getMensaje() != null ? xml.getMensaje().getIdTx() : null);
        String pin = (xml.getMensaje() != null) ? xml.getMensaje().getPin() : null;
        String legend = (xml.getMensaje() != null && xml.getMensaje().getLegend() != null)
                ? xml.getMensaje().getLegend().trim()
                : productoCatalogo.getLegend();
        String textoProveedor = respuesta.texto() != null ? respuesta.texto() : "Sin mensaje del proveedor";

        log.info("[PagoServicio]: Pago exitoso cuenta={}, UPC={}, ID_TX={}, NUM_AUTORIZACION={}",
                DatosSensibles.enmascararCuenta(numCuenta), upc, idTx, xml.getNumAutorizacion());

        String urlLogo = productoCatalogo.getUrlLogo();
        if (urlLogo == null || urlLogo.trim().isEmpty()) {
            urlLogo = logoServicioResolver.resolverUrlLogo(productoCatalogo.getServicio(), productoCatalogo.getProducto(),
                    request.getIdServicio(), request.getIdProducto());
        }

        // Comprobante por correo (no bloqueante: un fallo de Brevo jamás afecta un pago ya aplicado)
        String emailDestino = (request.getCorreoElectronico() != null && !request.getCorreoElectronico().trim().isEmpty())
                ? request.getCorreoElectronico().trim()
                : (cliente.getCorreoElectronico() != null ? cliente.getCorreoElectronico().trim() : null);
        String nombreCliente = ((cliente.getNombre() != null ? cliente.getNombre() : "") + " "
                + (cliente.getApellidoPaterno() != null ? cliente.getApellidoPaterno() : "")).trim();

        ComprobantePagoEmailDto emailDto = ComprobantePagoEmailDto.builder()
                .destinatarioEmail(emailDestino)
                .destinatarioNombre(!nombreCliente.isEmpty() ? nombreCliente : "Estimado Cliente")
                .numAutorizacion(xml.getNumAutorizacion())
                .idTx(idTx)
                .upc(upc)
                .numCuenta(numCuenta)
                .idServicio(request.getIdServicio())
                .servicio(productoCatalogo.getServicio())
                .idProducto(request.getIdProducto())
                .producto(productoCatalogo.getProducto())
                .telefono(telefono)
                .referencia(referencia)
                .montoPagado(montoPago)
                .comision(comisionProveedor)
                .legend(legend)
                .fechaProveedor(xml.getFecha())
                .horaLocal(horaLocal)
                .logoUrl(urlLogo)
                .tipoFront(productoCatalogo.getTipoFront())
                .build();

        ComprobanteEmailResult emailResult = null;
        try {
            emailResult = comprobanteEmailService.enviarComprobantePago(emailDto);
        } catch (Exception e) {
            log.error("[PagoServicio]: Error no bloqueante al enviar comprobante ({})", e.getClass().getSimpleName());
        }
        boolean emailEnviado = emailResult != null && emailResult.isEnviado();
        String messageId = emailResult != null ? emailResult.getMessageId() : null;

        return PagoServicioResponse.builder()
                .codigo("01")
                .mensaje("Pago Exitoso")
                .detalleProveedor(textoProveedor)
                .idTx(idTx)
                .numAutorizacion(xml.getNumAutorizacion())
                .upc(upc)
                .numCuenta(numCuenta)
                .idServicio(request.getIdServicio())
                .servicio(productoCatalogo.getServicio())
                .idProducto(request.getIdProducto())
                .producto(productoCatalogo.getProducto())
                .telefono(telefono)
                .referencia(referencia)
                .montoPagado(montoPago)
                .comision(comisionProveedor)
                .saldoAnterior(reserva.saldoAnterior())
                .saldoDisponibleActual(saldoDisponible)
                .saldoContableActual(saldoContable)
                .pin(pin != null && !pin.trim().isEmpty() ? pin.trim() : null)
                .legend(legend != null && !legend.trim().isEmpty() ? legend.trim() : null)
                .horaLocal(horaLocal)
                .fechaProveedor(xml.getFecha())
                .urlLogo(urlLogo)
                .emailEnviado(emailEnviado)
                .emailMessageId(messageId)
                .build();
    }

    private ReglaNegocioException excepcionPorRechazo(RespuestaSendTx respuesta) {
        if (respuesta.esRechazoExplicitoDelProveedor()) {
            return ReglaNegocioException.desdeGestoPago(respuesta.codigo(), respuesta.texto());
        }
        CatalogoCodigoError error = respuesta.errorInterno() != null
                ? respuesta.errorInterno()
                : CatalogoCodigoError.ERROR_COMUNICACION_GESTOPAGO;
        return new ReglaNegocioException(error, error.getDescripcion());
    }

    @Override
    public List<GestoPagoTransaccion> obtenerHistorialPorCuenta(String numeroCuenta) {
        return transaccionRepository.findByNumeroCuentaOrderByFechaCreacionDesc(numeroCuenta.trim());
    }

    @Override
    public PagoEstatusResponse consultarEstatus(String upc) {
        GestoPagoTransaccion tx = transaccionRepository.findByUpc(upc.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException(CatalogoCodigoError.TRANSACCION_NO_ENCONTRADA,
                        CatalogoCodigoError.TRANSACCION_NO_ENCONTRADA.getDescripcion()));

        EstatusTransaccion estatus = EstatusTransaccion.valueOf(tx.getEstatus());
        boolean definitivo = estatus == EstatusTransaccion.EXITOSA || estatus == EstatusTransaccion.RECHAZADA;
        String mensaje = switch (estatus) {
            case EXITOSA -> "El pago fue aplicado correctamente";
            case RECHAZADA -> "El pago no fue aplicado y el monto fue liberado en su cuenta";
            case PROCESANDO, PENDIENTE_CONCILIACION -> "El resultado esta siendo verificado con el proveedor. No reintente la operacion";
            case REVISION_MANUAL -> "La operacion requiere revision del equipo de soporte. No reintente la operacion";
        };

        return PagoEstatusResponse.builder()
                .upc(tx.getUpc())
                .estatus(tx.getEstatus())
                .definitivo(definitivo)
                .mensaje(mensaje)
                .monto(tx.getMonto())
                .numAutorizacion(estatus == EstatusTransaccion.EXITOSA ? tx.getNumAutorizacion() : null)
                .fechaCreacion(tx.getFechaCreacion())
                .fechaActualizacion(tx.getFechaActualizacion())
                .build();
    }

    // ---------------------------------------------------------------------------------------------
    // Validaciones
    // ---------------------------------------------------------------------------------------------

    private String normalizarIdempotencyKey(String clave) {
        if (clave == null || clave.isBlank()) {
            return null;
        }
        String limpia = clave.trim();
        if (!IDEMPOTENCY_KEY_PATTERN.matcher(limpia).matches()) {
            throw new ReglaNegocioException(CatalogoCodigoError.DATOS_ENTRADA_INVALIDOS,
                    "La cabecera Idempotency-Key debe tener entre 8 y 64 caracteres alfanumericos, guion o guion bajo");
        }
        return limpia;
    }

    private String calcularHuella(String numCuenta, PagoServicioRequest request, String telefono, String referencia, BigDecimal monto) {
        return DatosSensibles.sha256(String.join("|", numCuenta, String.valueOf(request.getIdServicio()),
                String.valueOf(request.getIdProducto()), monto.toPlainString(), telefono, referencia));
    }

    /**
     * Producto de monto fijo (precio &gt; 0 en el catálogo): el monto debe ser exactamente ese precio.
     * Producto de monto abierto: debe estar dentro del rango configurado.
     */
    private void validarMontoContraCatalogo(ProductItemDto producto, BigDecimal monto) {
        BigDecimal precio = producto.getPrecio();
        if (precio != null && precio.signum() > 0) {
            if (monto.compareTo(precio.setScale(2, RoundingMode.HALF_UP)) != 0) {
                throw new ReglaNegocioException(CatalogoCodigoError.MONTO_NO_PERMITIDO,
                        "El producto solicitado solo admite el monto de " + precio.setScale(2, RoundingMode.HALF_UP));
            }
            return;
        }
        if (monto.compareTo(montoMinimoAbierto) < 0 || monto.compareTo(montoMaximoAbierto) > 0) {
            throw new ReglaNegocioException(CatalogoCodigoError.MONTO_NO_PERMITIDO,
                    "El monto debe estar entre " + montoMinimoAbierto.setScale(2, RoundingMode.HALF_UP)
                            + " y " + montoMaximoAbierto.setScale(2, RoundingMode.HALF_UP));
        }
    }

    /**
     * Garantiza que el idServicio e idProducto existan en el catálogo de /productos (Redis -> PostgreSQL -> GestoPago fallback).
     */
    private ProductItemDto validarServicioYProductoEnCatalogo(Long idServicio, Long idProducto) {
        ProductCategorizedResponse catalogo = productoService.obtenerProductosCategorizados();
        List<ProductItemDto> listaProductos = null;

        if (catalogo != null && catalogo.getCategorias() != null && !catalogo.getCategorias().isEmpty()) {
            listaProductos = catalogo.getCategorias().values().stream()
                    .flatMap(Collection::stream)
                    .toList();
        } else {
            // En caso de que aún no haya corrido el cron de las 6:00 AM, consultamos el listado directo
            ProductListResponse directo = productoService.obtenerListaProductos();
            if (directo != null && directo.getProductos() != null) {
                listaProductos = directo.getProductos();
            }
        }

        if (listaProductos == null || listaProductos.isEmpty()) {
            throw new RecursoNoEncontradoException(CatalogoCodigoError.CATALOGO_NO_DISPONIBLE,
                    "El catalogo de productos (/productos) no se encuentra disponible para validar el servicio");
        }

        boolean existeServicio = listaProductos.stream()
                .anyMatch(p -> p.getIdServicio() != null && p.getIdServicio().equals(idServicio));

        if (!existeServicio) {
            throw new RecursoNoEncontradoException(CatalogoCodigoError.SERVICIO_NO_EXISTE,
                    "El servicio con idServicio " + idServicio + " no existe en el catalogo de /productos");
        }

        ProductItemDto productoEncontrado = listaProductos.stream()
                .filter(p -> p.getIdServicio() != null && p.getIdServicio().equals(idServicio)
                        && p.getIdProducto() != null && p.getIdProducto().equals(idProducto))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(CatalogoCodigoError.PRODUCTO_NO_EXISTE,
                        "El producto con idProducto " + idProducto + " no existe o no pertenece al servicio con idServicio " + idServicio + " en el catalogo de /productos"));

        if (productoEncontrado.getUrlLogo() == null || productoEncontrado.getUrlLogo().trim().isEmpty()) {
            productoEncontrado.setUrlLogo(logoServicioResolver.resolverUrlLogo(
                    productoEncontrado.getServicio(),
                    productoEncontrado.getProducto(),
                    productoEncontrado.getIdServicio(),
                    productoEncontrado.getIdProducto()
            ));
        }

        return productoEncontrado;
    }

    /**
     * Valida el segundo factor de autenticación (2FA) usando el módulo de Login:
     * 1) Vector de biometría facial MediaPipe (BiometriaFacialUtil)
     * 2) Token de sesión activa vigente del mismo usuario (SesionLogin)
     * 3) Contraseña cifrada BCrypt del usuario (PasswordEncryptionUtil)
     */
    private void validarAutenticacion2FA(Cliente cliente, String valor2fa) {
        if (valor2fa == null || valor2fa.trim().isEmpty()) {
            throw new AutenticacionException(CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA,
                    "El factor de autenticacion (2FA) es obligatorio");
        }

        UsuarioLogin usuario = usuarioLoginRepository.findByClienteIdCliente(cliente.getIdCliente())
                .orElseThrow(() -> new AutenticacionException(CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA,
                        "El cliente titular no cuenta con credenciales de seguridad registradas"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new AutenticacionException(CatalogoCodigoError.USUARIO_INACTIVO,
                    "El usuario de acceso del cliente se encuentra inactivo");
        }

        String limpio = valor2fa.trim();

        // Opción 1 (Camino principal solicitado): Biometría Facial MediaPipe
        if (usuario.getBiometricoFacialEmbedding() != null && !usuario.getBiometricoFacialEmbedding().trim().isEmpty()) {
            if (BiometriaFacialUtil.coincideEmbedding(usuario.getBiometricoFacialEmbedding(), limpio)) {
                log.info("[PagoServicio 2FA]: Validacion exitosa mediante Biometria Facial para clienteId={}", cliente.getIdCliente());
                return;
            }
        }

        // Opción 2: Token de sesión activa del cliente (proveniente de /auth/login)
        String tokenCandidate = limpio.replaceAll("(?i)^bearer\\s*", "").trim();
        List<SesionLogin> sesionesActivas = sesionLoginRepository.findByUsuarioIdUsuarioAndActivaTrue(usuario.getIdUsuario());
        for (SesionLogin sesion : sesionesActivas) {
            if (sesion.getTokenSesion().equals(tokenCandidate)) {
                if (sesion.haExpiradoPorInactividad(MINUTOS_INACTIVIDAD_SESION)) {
                    sesion.setActiva(false);
                    sesionLoginRepository.save(sesion);
                    throw new AutenticacionException(CatalogoCodigoError.SESION_EXPIRADA,
                            "El token de sesion proporcionado como 2FA ha expirado por inactividad (5 minutos)");
                }
                LocalDateTime ahora = LocalDateTime.now();
                sesion.setUltimoAcceso(ahora);
                sesion.setFechaExpiracion(ahora.plusMinutes(MINUTOS_INACTIVIDAD_SESION));
                sesionLoginRepository.save(sesion);
                log.info("[PagoServicio 2FA]: Validacion exitosa mediante sesion activa para clienteId={}", cliente.getIdCliente());
                return;
            }
        }

        // Opción 3: Contraseña registrada del cliente
        if (usuario.getPasswordHash() != null && PasswordEncryptionUtil.verificarPassword(limpio, usuario.getPasswordHash())) {
            log.info("[PagoServicio 2FA]: Validacion exitosa mediante credencial de usuario para clienteId={}", cliente.getIdCliente());
            return;
        }

        throw new AutenticacionException(CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA,
                "Validacion 2FA fallida: el vector biometrico facial, token de sesion o credencial no coincide con el titular de la cuenta");
    }

    private BigDecimal parsearDecimalSeguro(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(valor.trim().replace(",", "")).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
