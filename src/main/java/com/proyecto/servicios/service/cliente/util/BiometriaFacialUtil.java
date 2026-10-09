package com.proyecto.servicios.service.cliente.util;

import java.util.Arrays;

public final class BiometriaFacialUtil {

    private static final double UMBRAL_SIMILITUD_MINIMO = 0.80;

    private BiometriaFacialUtil() {}

    /**
     * Valida si el embedding capturado por MediaPipe coincide con el almacenado.
     */
    public static boolean coincideEmbedding(String embeddingGuardado, String embeddingEntrante) {
        if (embeddingGuardado == null || embeddingEntrante == null) {
            return false;
        }

        try {
            double[] vectorA = parsearVector(embeddingGuardado);
            double[] vectorB = parsearVector(embeddingEntrante);

            if (vectorA.length == 0 || vectorA.length != vectorB.length) {
                return false;
            }

            double similitud = calcularSimilitudCoseno(vectorA, vectorB);
            return similitud >= UMBRAL_SIMILITUD_MINIMO;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calcula la Similitud de Coseno: (A . B) / (||A|| * ||B||)
     */
    public static double calcularSimilitudCoseno(double[] a, double[] b) {
        double productoPunto = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;

        for (int i = 0; i < a.length; i++) {
            productoPunto += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }

        if (normaA == 0.0 || normaB == 0.0) {
            return 0.0;
        }

        return productoPunto / (Math.sqrt(normaA) * Math.sqrt(normaB));
    }

    private static double[] parsearVector(String vectorStr) {
        // Limpia corchetes en formato JSON "[0.12, -0.34, ...]"
        String limpio = vectorStr.replaceAll("[\\[\\]\\s]", "");
        if (limpio.isEmpty()) {
            return new double[0];
        }
        return Arrays.stream(limpio.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
    }
}
