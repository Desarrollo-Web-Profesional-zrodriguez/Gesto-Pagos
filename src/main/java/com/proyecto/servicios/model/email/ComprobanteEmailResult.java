package com.proyecto.servicios.model.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteEmailResult {
    private boolean enviado;
    private String messageId;
    private String mensaje;
    private String errorDetalle;
}
