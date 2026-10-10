package com.proyecto.servicios.service.cliente.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordEncryptionUtil {

    private static final int ITERACIONES = 65536;
    private static final int LONGITUD_KEY = 256;
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";

    private PasswordEncryptionUtil() {}

    /**
     * Cifra la contraseña en texto plano generando un Salt criptográfico seguro.
     */
    public static String cifrarPassword(String passwordPlano) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(passwordPlano.toCharArray(), salt, ITERACIONES, LONGITUD_KEY);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            // Formato guardado: saltBase64:hashBase64
            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Error al cifrar la contrasena", e);
        }
    }

    /**
     * Compara una contraseña en texto plano contra el hash almacenado.
     */
    public static boolean verificarPassword(String passwordPlano, String hashAlmacenado) {
        if (passwordPlano == null || hashAlmacenado == null || !hashAlmacenado.contains(":")) {
            return false;
        }

        try {
            String[] partes = hashAlmacenado.split(":");
            byte[] salt = Base64.getDecoder().decode(partes[0]);
            byte[] hashEsperado = Base64.getDecoder().decode(partes[1]);

            PBEKeySpec spec = new PBEKeySpec(passwordPlano.toCharArray(), salt, ITERACIONES, LONGITUD_KEY);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO);
            byte[] hashCalculado = factory.generateSecret(spec).getEncoded();

            // Comparación constante en tiempo para prevenir timing attacks
            int diff = hashEsperado.length ^ hashCalculado.length;
            for (int i = 0; i < hashEsperado.length && i < hashCalculado.length; i++) {
                diff |= hashEsperado[i] ^ hashCalculado[i];
            }
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
