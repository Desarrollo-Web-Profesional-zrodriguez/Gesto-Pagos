package com.proyecto.servicios.config;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class StrictTwoDecimalNumberDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken currentToken = p.currentToken();

        // 1. Si viene como String entre comillas (ejemplo: "200.00"), RECHAZAR explícitamente
        if (currentToken == JsonToken.VALUE_STRING) {
            String text = p.getText();
            String fieldName = p.currentName() != null ? p.currentName() : "monto";
            throw new IllegalArgumentException(
                String.format("El campo '%s' no debe enviarse entre comillas (\"%s\"). Debe ser un valor numerico directo con dos decimales (ejemplo: 200.00)", fieldName, text)
            );
        }

        // 2. Si no es un número directo
        if (currentToken != JsonToken.VALUE_NUMBER_FLOAT && currentToken != JsonToken.VALUE_NUMBER_INT) {
            String fieldName = p.currentName() != null ? p.currentName() : "monto";
            throw new IllegalArgumentException(
                String.format("El campo '%s' debe ser un valor numerico directo con dos decimales (ejemplo: 200.00)", fieldName)
            );
        }

        // 3. Obtener el texto del número tal como fue enviado en el JSON
        String rawNumber = p.getText() != null ? p.getText().trim() : "";

        // 4. Validar formato estricto con exactamente dos decimales (ej: 200.00, rechazando 200 o 200.5)
        if (!rawNumber.matches("^\\d+\\.\\d{2}$")) {
            String fieldName = p.currentName() != null ? p.currentName() : "monto";
            throw new IllegalArgumentException(
                String.format("El campo '%s' debe ser un numero con exactamente dos decimales (ejemplo: 200.00, no se acepta %s)", fieldName, rawNumber)
            );
        }

        return new BigDecimal(rawNumber);
    }
}
