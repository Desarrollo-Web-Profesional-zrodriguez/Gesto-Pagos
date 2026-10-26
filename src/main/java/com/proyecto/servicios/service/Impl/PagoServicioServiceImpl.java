package com.proyecto.servicios.service.Impl;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Saldo;
import com.proyecto.servicios.entity.cliente.SesionLogin;
import com.proyecto.servicios.entity.cliente.UsuarioLogin;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.exception.AutenticacionException;
import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.model.gestopago.catalogo.ProductCategorizedResponse;
import com.proyecto.servicios.model.gestopago.catalogo.ProductItemDto;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.transaccion.PagoServicioResponse;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxPayloadDto;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.SaldoRepository;
import com.proyecto.servicios.repositorys.cliente.SesionLoginRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioLoginRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.PagoServicioService;
import com.proyecto.servicios.service.ProductoService;
import com.proyecto.servicios.service.cliente.util.BiometriaFacialUtil;
import com.proyecto.servicios.service.cliente.util.PasswordEncryptionUtil;
import com.proyecto.servicios.util.AesEncryption;

import feign.FeignException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PagoServicioServiceImpl implements PagoServicioService {

    private static final DateTimeFormatter HORA_LOCAL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MINUTOS_INACTIVIDAD_SESION = 5;

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final SaldoRepository saldoRepository;
    private final UsuarioLoginRepository usuarioLoginRepository;
    private final SesionLoginRepository sesionLoginRepository;
    private final ProductoService productoService;
    private final GestoPagoTokenService tokenService;
    private final GestoPagoProductClient gestoPagoClient;
    private final GestoPagoTransaccionRepository transaccionRepository;
    private final ObjectMapper objectMapper;

    @Value("${gestopago.aes.secret-key}")
    private String aesSecretKey;

    @Value("${gestopago.aes.iv}")
    private String aesIv;

    @Value("${gestopago.service.unidad:083}")
    private String unidadNegocio;

    /**
     * Flujo NO transaccional a nivel global para no retener conexiones de BD durante la llamada HTTP externa a sendTx.do.
     * Implementa el patrón bancario de Dos Fases (Retención en Saldo Disponible -> Confirmación en Saldo Contable / Reverso en caso de fallo).
     */
    @Override
    public PagoServicioResponse procesarPago(PagoServicioRequest request) {
        String numCuenta = request.getNumCuenta().trim();
        log.info("[PagoServicio]: Iniciando solicitud de pago para cuenta={}, idServicio={}, idProducto={}, monto={}",
                numCuenta, request.getIdServicio(), request.getIdProducto(), request.getMonto());

        // 1. Validar que existan el Servicio y el Producto en el catálogo (/productos)
        ProductItemDto productoCatalogo = validarServicioYProductoEnCatalogo(request.getIdServicio(), request.getIdProducto());

        // 2. Validar Cuenta existente y activa
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numCuenta));

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new ReglaNegocioException(CatalogoCodigoError.CUENTA_INACTIVA,
                    "La cuenta " + numCuenta + " no se encuentra activa");
        }

        // 3. Validar que el Cliente titular exista y esté activo
        Cliente cliente = clienteRepository.findByCuentasNumeroCuenta(numCuenta)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontro cliente titular asociado a la cuenta: " + numCuenta));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException(CatalogoCodigoError.CLIENTE_INACTIVO,
                    "El cliente titular de la cuenta se encuentra inactivo");
        }

        // 4. Validar 2FA (Biometría Facial MediaPipe, Token de Sesión Activo o Contraseña del titular)
        validarAutenticacion2FA(cliente, request.getCodigo2fa());

        // 5. Validar Saldo Suficiente y aplicar Fase 1: Retención preventiva en saldoDisponible
        Saldo saldo = saldoRepository.findByCuentaNumeroCuenta(numCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numCuenta));

        BigDecimal saldoAnterior = BigDecimal.valueOf(saldo.getSaldoDisponible() != null ? saldo.getSaldoDisponible() : 0.0)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoPago = request.getMonto().setScale(2, RoundingMode.HALF_UP);

        if (saldoAnterior.compareTo(montoPago) < 0) {
            throw new ReglaNegocioException(CatalogoCodigoError.SALDO_INSUFICIENTE, String.format(
                    "Saldo insuficiente en la cuenta. Saldo disponible: %.2f, monto requerido: %.2f",
                    saldoAnterior, montoPago));
        }

        // Retención preventiva sobre saldoDisponible (evita doble gasto concurrente mientras responde GestoPago)
        BigDecimal saldoConRetencion = saldoAnterior.subtract(montoPago).setScale(2, RoundingMode.HALF_UP);
        saldo.setSaldoDisponible(saldoConRetencion.doubleValue());
        saldoRepository.save(saldo);
        log.info("[PagoServicio]: Retencion preventiva aplicada en cuenta {}. Saldo anterior: {}, Saldo disponible retenido: {}",
                numCuenta, saldoAnterior, saldoConRetencion);

        // 6. Construir petición JSON para sendTx y cifrar con AesEncryption Helper
        String horaLocal = LocalDateTime.now().format(HORA_LOCAL_FORMATTER);
        String upc = RandomStringUtils.randomAlphanumeric(10);

        SendTxPayloadDto sendTxPayload = SendTxPayloadDto.builder()
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .telefono(request.getTelefono().trim())
                .referencia(request.getReferencia().trim())
                .horaLocal(horaLocal)
                .montoPago(montoPago)
                .upc(upc)
                .unidad(unidadNegocio)
                .build();

        String jsonPayload;
        String encryptedPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(sendTxPayload);
            encryptedPayload = AesEncryption.AESCipher(
                    AesEncryption.Mode.ENCRYPT.name(),
                    jsonPayload,
                    aesIv,
                    aesSecretKey
            );
            if (encryptedPayload == null || encryptedPayload.trim().isEmpty()) {
                throw new ReglaNegocioException(CatalogoCodigoError.ERROR_CIFRADO_AES,
                        "Error al cifrar la peticion para GestoPago con AES-256");
            }
        } catch (Exception e) {
            revertirRetencionSaldo(saldo, montoPago, numCuenta);
            if (e instanceof ReglaNegocioException rne) {
                throw rne;
            }
            throw new ReglaNegocioException(CatalogoCodigoError.ERROR_CIFRADO_AES,
                    "Error al preparar y cifrar el payload de la transaccion: " + e.getMessage());
        }

        // 7. Obtener Token válido y enviar petición cifrada a sendTx.do
        String xmlResponse;
        SendTxXmlResponse parsedXml;
        try {
            xmlResponse = enviarPeticionSendTxConReintento(encryptedPayload, upc);
            parsedXml = parsearRespuestaXml(xmlResponse);
        } catch (Exception e) {
            log.error("[PagoServicio]: Error de comunicacion con sendTx.do para UPC={}. Revirtiendo retencion de saldo.", upc, e);
            revertirRetencionSaldo(saldo, montoPago, numCuenta);
            guardarEvidenciaTransaccion(request, horaLocal, upc, jsonPayload, encryptedPayload,
                    e.getMessage(), "ERR", e.getMessage(), null, "ERROR_COMUNICACION");
            if (e instanceof ReglaNegocioException rne) {
                throw rne;
            }
            throw new ReglaNegocioException(CatalogoCodigoError.ERROR_COMUNICACION_GESTOPAGO,
                    "Error al comunicarse con el servicio sendTx de GestoPago: " + e.getMessage());
        }

        // 8. Evaluar el código devuelto por GestoPago (siempre responde HTTP 200, pero solo CODIGO == "01" es efectivo)
        String codigoGestoPago = (parsedXml != null && parsedXml.getMensaje() != null && parsedXml.getMensaje().getCodigo() != null)
                ? parsedXml.getMensaje().getCodigo().trim()
                : "SIN_CODIGO";
        String textoGestoPago = (parsedXml != null && parsedXml.getMensaje() != null && parsedXml.getMensaje().getTexto() != null)
                ? parsedXml.getMensaje().getTexto().trim()
                : "Sin mensaje del proveedor";

        if (!"01".equals(codigoGestoPago)) {
            log.warn("[PagoServicio]: Transaccion rechazada por GestoPago (CODIGO={}, TEXTO={}). Revirtiendo retencion en cuenta {}.",
                    codigoGestoPago, textoGestoPago, numCuenta);
            revertirRetencionSaldo(saldo, montoPago, numCuenta);
            guardarEvidenciaTransaccion(request, horaLocal, upc, jsonPayload, encryptedPayload,
                    xmlResponse, codigoGestoPago, textoGestoPago, parsedXml, "RECHAZADA");
            throw ReglaNegocioException.desdeGestoPago(codigoGestoPago, textoGestoPago);
        }

        // 9. Fase 2: Pago Exitoso (CODIGO == "01") -> Confirmar descuento definitivo en saldoContable
        BigDecimal nuevoSaldoContable = BigDecimal.valueOf(saldo.getSaldoContable() != null ? saldo.getSaldoContable() : saldoAnterior.doubleValue())
                .subtract(montoPago)
                .setScale(2, RoundingMode.HALF_UP);
        saldo.setSaldoContable(nuevoSaldoContable.doubleValue());
        saldoRepository.save(saldo);

        // 10. Guardar evidencia obligatoria de la transacción exitosa
        guardarEvidenciaTransaccion(request, horaLocal, upc, jsonPayload, encryptedPayload,
                xmlResponse, codigoGestoPago, textoGestoPago, parsedXml, "EXITOSA");

        BigDecimal comisionProveedor = parsearDecimalSeguro(parsedXml.getComision());
        String idTx = parsedXml.getIdTx() != null ? parsedXml.getIdTx() :
                (parsedXml.getMensaje() != null ? parsedXml.getMensaje().getIdTx() : null);
        String pin = (parsedXml.getMensaje() != null) ? parsedXml.getMensaje().getPin() : null;
        String legend = (parsedXml.getMensaje() != null && parsedXml.getMensaje().getLegend() != null)
                ? parsedXml.getMensaje().getLegend().trim()
                : productoCatalogo.getLegend();

        log.info("[PagoServicio]: Pago Exitoso (01) para cuenta={}, UPC={}, ID_TX={}, NUM_AUTORIZACION={}",
                numCuenta, upc, idTx, parsedXml.getNumAutorizacion());

        return PagoServicioResponse.builder()
                .codigo("01")
                .mensaje("Pago Exitoso")
                .detalleProveedor(textoGestoPago)
                .idTx(idTx)
                .numAutorizacion(parsedXml.getNumAutorizacion())
                .upc(upc)
                .numCuenta(numCuenta)
                .idServicio(request.getIdServicio())
                .servicio(productoCatalogo.getServicio())
                .idProducto(request.getIdProducto())
                .producto(productoCatalogo.getProducto())
                .telefono(request.getTelefono().trim())
                .referencia(request.getReferencia().trim())
                .montoPagado(montoPago)
                .comision(comisionProveedor)
                .saldoAnterior(saldoAnterior)
                .saldoDisponibleActual(saldoConRetencion)
                .saldoContableActual(nuevoSaldoContable)
                .pin(pin != null && !pin.trim().isEmpty() ? pin.trim() : null)
                .legend(legend != null && !legend.trim().isEmpty() ? legend.trim() : null)
                .horaLocal(horaLocal)
                .fechaProveedor(parsedXml.getFecha())
                .build();
    }

    @Override
    public List<GestoPagoTransaccion> obtenerHistorialPorCuenta(String numeroCuenta) {
        return transaccionRepository.findByNumeroCuentaOrderByFechaCreacionDesc(numeroCuenta.trim());
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

        return listaProductos.stream()
                .filter(p -> p.getIdServicio() != null && p.getIdServicio().equals(idServicio)
                        && p.getIdProducto() != null && p.getIdProducto().equals(idProducto))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(CatalogoCodigoError.PRODUCTO_NO_EXISTE,
                        "El producto con idProducto " + idProducto + " no existe o no pertenece al servicio con idServicio " + idServicio + " en el catalogo de /productos"));
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
                log.info("[PagoServicio 2FA]: Validacion exitosa mediante Biometria Facial MediaPipe para cliente={}", cliente.getCurp());
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
                log.info("[PagoServicio 2FA]: Validacion exitosa mediante sesion activa para cliente={}", cliente.getCurp());
                return;
            }
        }

        // Opción 3: Contraseña registrada del cliente
        if (usuario.getPasswordHash() != null && PasswordEncryptionUtil.verificarPassword(limpio, usuario.getPasswordHash())) {
            log.info("[PagoServicio 2FA]: Validacion exitosa mediante credencial de usuario para cliente={}", cliente.getCurp());
            return;
        }

        throw new AutenticacionException(CatalogoCodigoError.AUTENTICACION_2FA_FALLIDA,
                "Validacion 2FA fallida: el vector biometrico facial, token de sesion o credencial no coincide con el titular de la cuenta");
    }

    /**
     * Envía la petición POST a /sistema/service/sendTx.do con el token Bearer y el payload cifrado en 'signed'.
     * Si el token expiró (HTTP 401 o 403), lo renueva automáticamente y reintenta una vez.
     */
    private String enviarPeticionSendTxConReintento(String encryptedPayload, String upc) {
        String token = tokenService.obtenerTokenValido();
        if (token == null || token.trim().isEmpty()) {
            throw new ReglaNegocioException(CatalogoCodigoError.TOKEN_GESTOPAGO_INVALIDO,
                    "No fue posible obtener un token valido de autenticacion para GestoPago");
        }

        try {
            return invocarSendTx(token, encryptedPayload, upc);
        } catch (FeignException e) {
            if (e.status() == 401 || e.status() == 403 || (e.contentUTF8() != null && e.contentUTF8().contains("EXPIRED"))) {
                log.warn("[PagoServicio]: Token de GestoPago expirado o rechazado (HTTP {}). Renovando token y reintentando...", e.status());
                tokenService.renovarToken();
                String nuevoToken = tokenService.obtenerTokenValido();
                if (nuevoToken != null && !nuevoToken.trim().isEmpty()) {
                    return invocarSendTx(nuevoToken, encryptedPayload, upc);
                }
            }
            throw e;
        }
    }

    private String invocarSendTx(String token, String encryptedPayload, String upc) {
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
        formParams.add("random", System.currentTimeMillis() + "_" + upc);
        formParams.add("signed", encryptedPayload);

        return gestoPagoClient.sendTx(authHeader, formParams);
    }

    private SendTxXmlResponse parsearRespuestaXml(String xmlResponse) throws Exception {
        if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
            throw new ReglaNegocioException(CatalogoCodigoError.ERROR_COMUNICACION_GESTOPAGO,
                    "El servicio sendTx de GestoPago devolvio una respuesta vacia");
        }

        // Verificar si GestoPago respondió un JSON de token expirado con código 200/403
        String trimmed = xmlResponse.trim();
        if (trimmed.startsWith("{") && trimmed.contains("EXPIRED")) {
            throw new ReglaNegocioException(CatalogoCodigoError.TOKEN_GESTOPAGO_INVALIDO,
                    "El token de GestoPago se encuentra expirado: " + trimmed);
        }

        JAXBContext context = JAXBContext.newInstance(SendTxXmlResponse.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (SendTxXmlResponse) unmarshaller.unmarshal(new StringReader(trimmed));
    }

    private void revertirRetencionSaldo(Saldo saldo, BigDecimal montoRetenido, String numCuenta) {
        try {
            BigDecimal disponibleActual = BigDecimal.valueOf(saldo.getSaldoDisponible() != null ? saldo.getSaldoDisponible() : 0.0);
            BigDecimal disponibleRestaurado = disponibleActual.add(montoRetenido).setScale(2, RoundingMode.HALF_UP);
            saldo.setSaldoDisponible(disponibleRestaurado.doubleValue());
            saldoRepository.save(saldo);
            log.info("[PagoServicio]: Retencion revertida exitosamente en cuenta {}. Saldo disponible restaurado a: {}",
                    numCuenta, disponibleRestaurado);
        } catch (Exception ex) {
            log.error("[PagoServicio CRITICO]: No fue posible revertir la retencion en la cuenta {}: {}", numCuenta, ex.getMessage(), ex);
        }
    }

    private void guardarEvidenciaTransaccion(PagoServicioRequest request,
                                             String horaLocal,
                                             String upc,
                                             String jsonPayload,
                                             String encryptedPayload,
                                             String responseXml,
                                             String codigoRespuesta,
                                             String mensajeRespuesta,
                                             SendTxXmlResponse parsedXml,
                                             String estatus) {
        try {
            String idTx = null;
            String numAutorizacion = null;
            BigDecimal comision = null;
            String pin = null;
            String legend = null;

            if (parsedXml != null) {
                idTx = parsedXml.getIdTx();
                numAutorizacion = parsedXml.getNumAutorizacion();
                comision = parsearDecimalSeguro(parsedXml.getComision());
                if (parsedXml.getMensaje() != null) {
                    if (idTx == null) {
                        idTx = parsedXml.getMensaje().getIdTx();
                    }
                    pin = parsedXml.getMensaje().getPin();
                    legend = parsedXml.getMensaje().getLegend();
                }
            }

            GestoPagoTransaccion txLog = GestoPagoTransaccion.builder()
                    .upc(upc)
                    .numeroCuenta(request.getNumCuenta().trim())
                    .idServicio(request.getIdServicio())
                    .idProducto(request.getIdProducto())
                    .telefono(request.getTelefono().trim())
                    .referencia(request.getReferencia().trim())
                    .monto(request.getMonto())
                    .horaLocal(horaLocal)
                    .unidad(unidadNegocio)
                    .payloadJson(jsonPayload)
                    .payloadCifrado(encryptedPayload)
                    .responseXml(responseXml)
                    .codigoRespuesta(codigoRespuesta)
                    .mensajeRespuesta(mensajeRespuesta)
                    .idTxGestoPago(idTx)
                    .numAutorizacion(numAutorizacion)
                    .comision(comision)
                    .pin(pin)
                    .legend(legend)
                    .estatus(estatus)
                    .build();

            transaccionRepository.save(txLog);
        } catch (Exception e) {
            log.error("[PagoServicio]: Error al guardar bitacora de evidencia en gestopago_transacciones: {}", e.getMessage());
        }
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
