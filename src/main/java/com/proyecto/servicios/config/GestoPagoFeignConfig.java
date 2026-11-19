package com.proyecto.servicios.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Inyecta la cabecera {@code X-API-Key} documentada en la especificación técnica de PuntoRed / GestoPago
 * para todas las peticiones salientes hacia los clientes Feign de GestoPago.
 */
@Configuration
public class GestoPagoFeignConfig {

    @Value("${gestopago.api-key:YSX1HpAFum4TpCecyFBxs4eIjAlbhKqK6fpcSQp8}")
    private String gestoPagoApiKey;

    @Bean
    public RequestInterceptor gestoPagoApiKeyInterceptor() {
        return template -> {
            String targetName = template.feignTarget() != null ? template.feignTarget().name() : "";
            String targetUrl = template.feignTarget() != null ? template.feignTarget().url() : "";
            if ((targetName.startsWith("gestoPago") || targetUrl.contains("gestopago"))
                    && gestoPagoApiKey != null && !gestoPagoApiKey.isBlank()) {
                template.header("X-API-Key", gestoPagoApiKey.trim());
            }
        };
    }
}
