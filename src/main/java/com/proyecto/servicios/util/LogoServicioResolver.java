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
        // Telecomunicaciones y Cable / Internet
        map.put("IZZI", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/66/Logo_Izzi_Telecom.png/320px-Logo_Izzi_Telecom.png");
        map.put("TOTALPLAY", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/Logo_Totalplay.png/320px-Logo_Totalplay.png");
        map.put("MEGACABLE", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c9/Megacable_logo.svg/320px-Megacable_logo.svg.png");
        map.put("TELMEX", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Telmex_2014_logo.svg/320px-Telmex_2014_logo.svg.png");
        map.put("SKY", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Sky_M%C3%A9xico_logo_2017.svg/320px-Sky_M%C3%A9xico_logo_2017.svg.png");
        map.put("DISH", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/Dish_Network_logo.svg/320px-Dish_Network_logo.svg.png");

        // Telefonía y Recargas Móviles
        map.put("TELCEL", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/53/Telcel_logo.svg/320px-Telcel_logo.svg.png");
        map.put("MOVISTAR", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6a/Movistar_logo_2020.svg/320px-Movistar_logo_2020.svg.png");
        map.put("AT&T", "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/AT%26T_logo_2016.svg/320px-AT%26T_logo_2016.svg.png");
        map.put("ATT", "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/AT%26T_logo_2016.svg/320px-AT%26T_logo_2016.svg.png");
        map.put("UNEFON", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Unefon_logo.svg/320px-Unefon_logo.svg.png");
        map.put("BAIT", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d3/Logo_Bait.svg/320px-Logo_Bait.svg.png");
        map.put("VIRGIN", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/Virgin_Mobile_logo.svg/320px-Virgin_Mobile_logo.svg.png");
        map.put("OXXO", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/66/Oxxo_Logo.svg/320px-Oxxo_Logo.svg.png");

        // Servicios Públicos (Luz, Agua, Gas)
        map.put("CFE", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/22/CFE_logo.svg/320px-CFE_logo.svg.png");
        map.put("NATURGY", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Naturgy_logo.svg/320px-Naturgy_logo.svg.png");
        map.put("GAS NATURAL", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Naturgy_logo.svg/320px-Naturgy_logo.svg.png");
        map.put("SACMEX", "https://upload.wikimedia.org/wikipedia/commons/thumb/5/51/Sacmex.png/320px-Sacmex.png");
        map.put("AGUA", "https://cdn-icons-png.flaticon.com/512/3105/3105807.png");
        map.put("LUZ", "https://cdn-icons-png.flaticon.com/512/2807/2807572.png");
        map.put("PREDIAL", "https://cdn-icons-png.flaticon.com/512/2257/2257295.png");

        // Entretenimiento y Pines
        map.put("NETFLIX", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/08/Netflix_2015_logo.svg/320px-Netflix_2015_logo.svg.png");
        map.put("SPOTIFY", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/19/Spotify_logo_without_text.svg/320px-Spotify_logo_without_text.svg.png");
        map.put("XBOX", "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f9/Xbox_one_logo.svg/320px-Xbox_one_logo.svg.png");
        map.put("PLAYSTATION", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/00/PlayStation_logo.svg/320px-PlayStation_logo.svg.png");
        map.put("AMAZON", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/Amazon_logo.svg/320px-Amazon_logo.svg.png");
        map.put("UBER", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cc/Uber_logo_2018.png/320px-Uber_logo_2018.png");

        LOGOS_POR_PALABRA_CLAVE = Collections.unmodifiableMap(map);
    }

    public static final String DEFAULT_GESTOPAGO_BASE_URL = "https://gestopago.portalventas.net";

    private final String gestopagoBaseUrl;

    public LogoServicioResolver() {
        this(DEFAULT_GESTOPAGO_BASE_URL);
    }

    public LogoServicioResolver(String gestopagoBaseUrl) {
        this.gestopagoBaseUrl = (gestopagoBaseUrl != null && !gestopagoBaseUrl.trim().isEmpty())
                ? gestopagoBaseUrl.replaceAll("/+$", "")
                : DEFAULT_GESTOPAGO_BASE_URL;
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
     * Resuelve la URL del logo buscando coincidencias en el nombre del servicio o producto.
     */
    public String resolverUrlLogo(String servicio, Long idServicio, Long idProducto) {
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
