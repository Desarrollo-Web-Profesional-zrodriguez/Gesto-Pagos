package com.proyecto.servicios.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** Utilidades para no exponer datos sensibles en logs/mensajes y para huellas de operaciones. */
public final class DatosSensibles {

    private static final Pattern IPV4_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    private DatosSensibles() {
    }

    /** 8324433882000000 -> ************0000 */
    public static String enmascararCuenta(String cuenta) {
        if (cuenta == null || cuenta.length() <= 4) {
            return "****";
        }
        return "*".repeat(cuenta.length() - 4) + cuenta.substring(cuenta.length() - 4);
    }

    /** Oculta direcciones IP u otros datos sensibles que el proveedor pueda incluir en textos de error. */
    public static String sanitizarTextoProveedor(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        return IPV4_PATTERN.matcher(texto.trim()).replaceAll("[IP_OCULTA]");
    }

    /** Hash SHA-256 hexadecimal; sirve como huella estable de una operación sin almacenar sus datos en claro. */
    public static String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
