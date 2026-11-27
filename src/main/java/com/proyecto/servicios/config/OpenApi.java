package com.proyecto.servicios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Onboarding de Clientes y Gestion Financiera")
                        .description("Servicios REST para registro de clientes personas fisicas, creacion de cuentas bancarias, autenticacion con biometria facial MediaPipe, control de sesiones con inactividad y catalogos en base de datos.")
                        .version("1.0.0")
                        .contact(new Contact().name("Equipo de Desarrollo")))
                .addServersItem(new Server().url("/").description("Servidor actual"));
    }
}
