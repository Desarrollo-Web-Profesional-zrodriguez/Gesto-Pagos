package com.proyecto.servicios.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesEncryption {

    private AesEncryption() {
        // Helper class
    }

    public static String AESCipher(String mode, String input, String IV, String KEY) {
        byte[] iv = Base64.getDecoder().decode(IV);
        byte[] key = Base64.getDecoder().decode(KEY);
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        if (mode.equals("ENCRYPT")) {
            return encrypt(input, secretKey, ivspec);
        } else if (mode.equals("DECRYPT")) {
            return decrypt(input, secretKey, ivspec);
        }
        return null;
    }

    // Cipher to encrypt
    public static String encrypt(String input, SecretKey secretKey, IvParameterSpec ivspec) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    // Cypher to decrypt
    public static String decrypt(String input, SecretKey secretKey, IvParameterSpec ivspec) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(input)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public enum Mode {
        ENCRYPT, DECRYPT
    }
}
