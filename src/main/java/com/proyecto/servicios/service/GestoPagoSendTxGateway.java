package com.proyecto.servicios.service;

import java.io.StringReader;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx;
import com.proyecto.servicios.model.gestopago.transaccion.RespuestaSendTx.Veredicto;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxXmlResponse;

import feign.FeignException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import lombok.extern.slf4j.Slf4j;

/**
 * Única puerta de salida hacia sendTx.do. Encapsula autenticación Bearer, renovación del token,
 * lectura segura del XML y, sobre todo, la CLASIFICACIÓN del resultado en tres veredictos
 * (EXITO / RECHAZADO / INDETERMINADO). Esa clasificación decide qué se hace con el dinero:
 *
 * <ul>
 *   <li>EXITO: confirmar el cargo.</li>
 *   <li>RECHAZADO: se tiene certeza de que no se procesó -> liberar saldo.</li>
 *   <li>INDETERMINADO: no se sabe -> mantener la retención y conciliar. Un timeout NO implica "no procesado".</li>
 * </ul>
 *
 * Nunca registra el token, el payload ni el cuerpo completo de la respuesta.
 */
@Component
@Slf4j
public class GestoPagoSendTxGateway {

    private static final String CODIGO_EXITO = "01";
    /** Códigos del proveedor que significan "en proceso", "falla aguas abajo" o "timeout (82 -> requiere confirmTx)". */
    private static final Set<String> CODIGOS_INDETERMINADOS = Set.of("07", "08", "82");

    private final GestoPagoTokenService tokenService;
    private final GestoPagoProductClient gestoPagoClient;
    private final JAXBContext jaxbContext;
    private final XMLInputFactory xmlInputFactory;

    public GestoPagoSendTxGateway(GestoPagoTokenService tokenService, GestoPagoProductClient gestoPagoClient) {
        this.tokenService = tokenService;
        this.gestoPagoClient = gestoPagoClient;
        try {
            this.jaxbContext = JAXBContext.newInstance(SendTxXmlResponse.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("No fue posible inicializar el contexto JAXB de sendTx", e);
        }
        // Lectura endurecida contra XXE: sin DTD ni entidades externas
        this.xmlInputFactory = XMLInputFactory.newFactory();
        this.xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        this.xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    }

    /** Ejecuta sendTx con el payload ya cifrado y nunca lanza: siempre devuelve un veredicto. */
    public RespuestaSendTx ejecutar(String payloadCifrado, String upc) {
        String token;
        try {
            token = tokenService.obtenerTokenValido();
        } catch (Exception e) {
            log.error("[sendTx]: UPC={} no se pudo obtener el token ({})", upc, e.getClass().getSimpleName());
            return noEnviado(CatalogoCodigoError.TOKEN_GESTOPAGO_INVALIDO);
        }
        if (token == null || token.isBlank()) {
            log.error("[sendTx]: UPC={} sin token valido de GestoPago", upc);
            return noEnviado(CatalogoCodigoError.TOKEN_GESTOPAGO_INVALIDO);
        }

        String respuesta;
        try {
            respuesta = enviarConRenovacionDeToken(token, payloadCifrado, upc);
        } catch (TokenNoDisponibleException e) {
            return noEnviado(CatalogoCodigoError.TOKEN_GESTOPAGO_INVALIDO);
        } catch (FeignException e) {
            int status = e.status();
            if (status >= 400 && status < 500) {
                // El proveedor respondió explícitamente que no aceptó la petición: no se procesó.
                log.warn("[sendTx]: UPC={} rechazada por el proveedor a nivel HTTP ({})", upc, status);
                return noEnviado(CatalogoCodigoError.ERROR_COMUNICACION_GESTOPAGO);
            }
            // 5xx, timeout de lectura/conexión (status -1): la petición pudo haberse aplicado.
            log.error("[sendTx]: UPC={} resultado INDETERMINADO (HTTP {}, {})", upc, status, e.getClass().getSimpleName());
            return indeterminado(null, null);
        } catch (Exception e) {
            log.error("[sendTx]: UPC={} resultado INDETERMINADO ({})", upc, e.getClass().getSimpleName());
            return indeterminado(null, null);
        }

        return clasificar(respuesta, upc);
    }

    private RespuestaSendTx clasificar(String respuesta, String upc) {
        if (respuesta == null || respuesta.isBlank()) {
            log.error("[sendTx]: UPC={} respuesta vacia del proveedor: INDETERMINADO", upc);
            return indeterminado(respuesta, null);
        }

        String limpio = respuesta.trim();
        if (limpio.startsWith("{") && limpio.contains("EXPIRED")) {
            // El proveedor rechazó la autenticación: la operación no se procesó.
            log.warn("[sendTx]: UPC={} token expirado segun el proveedor", upc);
            return noEnviado(CatalogoCodigoError.TOKEN_GESTOPAGO_INVALIDO);
        }

        SendTxXmlResponse xml;
        try {
            xml = parsear(limpio);
        } catch (Exception e) {
            log.error("[sendTx]: UPC={} respuesta ilegible ({}): INDETERMINADO", upc, e.getClass().getSimpleName());
            return indeterminado(respuesta, null);
        }

        String codigo = (xml != null && xml.getMensaje() != null && xml.getMensaje().getCodigo() != null)
                ? xml.getMensaje().getCodigo().trim() : null;
        String texto = (xml != null && xml.getMensaje() != null && xml.getMensaje().getTexto() != null)
                ? xml.getMensaje().getTexto().trim() : null;

        if (codigo == null || codigo.isEmpty()) {
            log.error("[sendTx]: UPC={} respuesta sin CODIGO: INDETERMINADO", upc);
            return new RespuestaSendTx(Veredicto.INDETERMINADO, null, texto, respuesta, xml, null);
        }
        if (CODIGO_EXITO.equals(codigo)) {
            return new RespuestaSendTx(Veredicto.EXITO, codigo, texto, respuesta, xml, null);
        }
        if (CODIGOS_INDETERMINADOS.contains(codigo)) {
            log.warn("[sendTx]: UPC={} el proveedor respondio CODIGO={} (en proceso / error aguas abajo): INDETERMINADO", upc, codigo);
            return new RespuestaSendTx(Veredicto.INDETERMINADO, codigo, texto, respuesta, xml, null);
        }
        // El texto del proveedor se registra sanitizado (sin IPs) solo en servidor; jamás se devuelve tal cual al cliente.
        log.warn("[sendTx]: UPC={} rechazo explicito del proveedor CODIGO={} TEXTO={}",
                upc, codigo, com.proyecto.servicios.util.DatosSensibles.sanitizarTextoProveedor(texto));
        return new RespuestaSendTx(Veredicto.RECHAZADO, codigo, texto, respuesta, xml, null);
    }

    private String enviarConRenovacionDeToken(String token, String payloadCifrado, String upc) {
        try {
            return invocar(token, payloadCifrado, upc);
        } catch (FeignException e) {
            boolean tokenRechazado = e.status() == 401 || e.status() == 403
                    || (e.status() >= 400 && e.status() < 500 && e.contentUTF8() != null && e.contentUTF8().contains("EXPIRED"));
            if (!tokenRechazado) {
                throw e;
            }
            // Un 401/403 significa que la petición fue rechazada ANTES de procesarse: reintentar con el mismo UPC es seguro.
            log.warn("[sendTx]: UPC={} token rechazado (HTTP {}). Renovando y reintentando una vez", upc, e.status());
            String nuevoToken;
            try {
                tokenService.renovarToken();
                nuevoToken = tokenService.obtenerTokenValido();
            } catch (Exception ex) {
                throw new TokenNoDisponibleException();
            }
            if (nuevoToken == null || nuevoToken.isBlank()) {
                throw new TokenNoDisponibleException();
            }
            return invocar(nuevoToken, payloadCifrado, upc);
        }
    }

    private String invocar(String token, String payloadCifrado, String upc) {
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("random", System.currentTimeMillis() + "_" + upc);
        form.add("signed", payloadCifrado);
        return gestoPagoClient.sendTx(authHeader, form);
    }

    private SendTxXmlResponse parsear(String xml) throws Exception {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(xml));
        try {
            return (SendTxXmlResponse) unmarshaller.unmarshal(reader);
        } finally {
            reader.close();
        }
    }

    private RespuestaSendTx noEnviado(CatalogoCodigoError error) {
        return new RespuestaSendTx(Veredicto.RECHAZADO, null, null, null, null, error);
    }

    private RespuestaSendTx indeterminado(String raw, SendTxXmlResponse xml) {
        return new RespuestaSendTx(Veredicto.INDETERMINADO, null, null, raw, xml, null);
    }

    /** Señal interna: no se pudo conseguir un token válido tras el 401/403 (la petición no se procesó). */
    private static final class TokenNoDisponibleException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private TokenNoDisponibleException() {
            super(null, null, false, false);
        }
    }
}
