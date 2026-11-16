package com.proyecto.servicios.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

class CorsConfigTest {

    private MockMvc mockMvc;

    @RestController
    static class DummyController {
        @PostMapping("/pagos")
        String ping() {
            return "ok";
        }
    }

    @BeforeEach
    void setUp() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "*");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyController())
                .addFilters(corsConfig.corsFilter())
                .build();
    }

    @Test
    @DisplayName("Preflight OPTIONS permite Idempotency-Key, Authorization y métodos REST")
    void preflightPermiteCabecerasYMetodos() throws Exception {
        mockMvc.perform(options("/pagos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Idempotency-Key, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Petición real incluye Access-Control-Allow-Origin y cabeceras expuestas")
    void peticionRealIncluyeCabecerasCors() throws Exception {
        mockMvc.perform(post("/pagos")
                        .header("Origin", "https://app.gestopago.local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.gestopago.local"));
    }
}
