package com.proyecto.servicios.config;

import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.proyecto.servicios.exception.ErrorResponse;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI openAPI() {
        // Registrar explicitamente el esquema de ErrorResponse en components/schemas
        Map<String, Schema> errorSchemas = ModelConverters.getInstance().read(ErrorResponse.class);
        Components components = new Components();
        errorSchemas.forEach(components::addSchemas);

        return new OpenAPI()
                .info(new Info()
                        .title("API Onboarding de Clientes y Gestion Financiera")
                        .description("Servicios REST para registro de clientes personas fisicas, creacion de cuentas bancarias, autenticacion con biometria facial MediaPipe, control de sesiones con inactividad y catalogos en base de datos.")
                        .version("1.0.0")
                        .contact(new Contact().name("Equipo de Desarrollo")))
                .components(components)
                .addServersItem(new Server().url("/").description("Servidor actual"));
    }

    @Bean
    public OpenApiCustomizer globalResponsesOpenApiCustomizer() {
        return openApi -> {
            // Asegurar que components y schemas existan
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            if (openApi.getComponents().getSchemas() == null || !openApi.getComponents().getSchemas().containsKey("ErrorResponse")) {
                Map<String, Schema> errorSchemas = ModelConverters.getInstance().read(ErrorResponse.class);
                errorSchemas.forEach(openApi.getComponents()::addSchemas);
            }

            Schema<?> errorSchema = new Schema<>().$ref("#/components/schemas/ErrorResponse");
            Content errorContent = new Content().addMediaType("application/json", new MediaType().schema(errorSchema));

            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();

                if (!responses.containsKey("400")) {
                    responses.addApiResponse("400", new ApiResponse()
                            .description("Bad Request - Error de validacion o formato en los datos de entrada")
                            .content(errorContent));
                }
                if (!responses.containsKey("401")) {
                    responses.addApiResponse("401", new ApiResponse()
                            .description("Unauthorized - Credenciales invalidas o sesion inactiva")
                            .content(errorContent));
                }
                if (!responses.containsKey("404")) {
                    responses.addApiResponse("404", new ApiResponse()
                            .description("Not Found - Recurso no encontrado")
                            .content(errorContent));
                }
                if (!responses.containsKey("409")) {
                    responses.addApiResponse("409", new ApiResponse()
                            .description("Conflict - Conflicto de duplicidad (CURP, RFC o correo)")
                            .content(errorContent));
                }
                if (!responses.containsKey("500")) {
                    responses.addApiResponse("500", new ApiResponse()
                            .description("Internal Server Error - Error inesperado en el servidor")
                            .content(errorContent));
                }
            }));
        };
    }
}
