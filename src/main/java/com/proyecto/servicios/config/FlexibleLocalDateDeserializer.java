package com.proyecto.servicios.config;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        text = text.trim();

        // Valida que cumpla estrictamente el formato AAAA-MM-DD (solo guiones, no barras)
        if (!text.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new IllegalArgumentException(
                String.format("El campo 'fechaNacimiento' tiene un formato de fecha invalido ('%s'). Solo se acepta el formato 'AAAA-MM-DD' (ejemplo: 1992-05-20)", text)
            );
        }

        try {
            return LocalDate.parse(text, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                String.format("El campo 'fechaNacimiento' contiene una fecha invalida o inexistente ('%s'). Solo se acepta el formato 'AAAA-MM-DD' (ejemplo: 1992-05-20)", text)
            );
        }
    }
}
