package com.proyecto.servicios.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class LogoServicioResolver {

    public static final String LOGO_DEFAULT_SERVICIO = "https://cdn-icons-png.flaticon.com/512/1019/1019607.png";

    private static final Map<String, String> LOGOS_POR_PALABRA_CLAVE;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        // Iconos genéricos de respaldo cuando no se cuenta con un idServicio oficial de PuntoRed
        map.put("AGUA", "https://cdn-icons-png.flaticon.com/512/3105/3105807.png");
        map.put("LUZ", "https://cdn-icons-png.flaticon.com/512/2807/2807572.png");
        map.put("PREDIAL", "https://cdn-icons-png.flaticon.com/512/2257/2257295.png");

        LOGOS_POR_PALABRA_CLAVE = Collections.unmodifiableMap(map);
    }

    public static final String DEFAULT_GESTOPAGO_BASE_URL = "https://gestopago.portalventas.net";

    private final String gestopagoBaseUrl;

    public LogoServicioResolver() {
        this(DEFAULT_GESTOPAGO_BASE_URL);
    }

    public LogoServicioResolver(String gestopagoBaseUrl) {
        this.gestopagoBaseUrl = (gestopagoBaseUrl != null && !gestopagoBaseUrl.trim().isEmpty())
                ? stripTrailingSlashes(gestopagoBaseUrl.trim())
                : DEFAULT_GESTOPAGO_BASE_URL;
    }

    private static String stripTrailingSlashes(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }

    /**
     * Resuelve la URL oficial del logo de PuntoRed / GestoPago según la especificación técnica:
     * Endpoint getServiceLogo: https://gestopago.portalventas.net/sistema/images/gestopago/servicios/{idServicio}.png
     */
    public String resolverUrlLogoPuntoRed(Long idServicio) {
        if (idServicio != null && idServicio > 0) {
            return String.format("%s/sistema/images/gestopago/servicios/%d.png", gestopagoBaseUrl, idServicio);
        }
        return LOGO_DEFAULT_SERVICIO;
    }

    /**
     * Resuelve la URL oficial de la imagen de referencia/ayuda de PuntoRed / GestoPago:
     * Endpoint getReferenceImage: https://gestopago.portalventas.net/sistema/images/gestopago/referencias/{idServicio}.jpg
     */
    public String resolverUrlReferenciaPuntoRed(Long idServicio) {
        if (idServicio != null && idServicio > 0) {
            return String.format("%s/sistema/images/gestopago/referencias/%d.jpg", gestopagoBaseUrl, idServicio);
        }
        return null;
    }

    /**
     * Resuelve la URL del logo priorizando el endpoint oficial getServiceLogo de PuntoRed
     * ({@code /sistema/images/gestopago/servicios/{idServicio}.png}) y usando iconos de respaldo
     * cuando no se cuenta con un {@code idServicio} válido.
     */
    @SuppressWarnings("java:S1172")
    public String resolverUrlLogo(String servicio, Long idServicio, Long idProducto) {
        if (idServicio != null && idServicio > 0) {
            return resolverUrlLogoPuntoRed(idServicio);
        }
        if (servicio != null && !servicio.trim().isEmpty()) {
            String texto = servicio.toUpperCase().trim();
            for (Map.Entry<String, String> entry : LOGOS_POR_PALABRA_CLAVE.entrySet()) {
                if (texto.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return LOGO_DEFAULT_SERVICIO;
    }

    /**
     * Resuelve la URL del logo combinando nombre de servicio y nombre de producto si ambos existen.
     */
    public String resolverUrlLogo(String servicio, String producto, Long idServicio, Long idProducto) {
        String candidato = resolverUrlLogo(servicio, idServicio, idProducto);
        if (!LOGO_DEFAULT_SERVICIO.equals(candidato)) {
            return candidato;
        }
        if (producto != null && !producto.trim().isEmpty()) {
            String textoProd = producto.toUpperCase().trim();
            for (Map.Entry<String, String> entry : LOGOS_POR_PALABRA_CLAVE.entrySet()) {
                if (textoProd.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return LOGO_DEFAULT_SERVICIO;
    }
}
