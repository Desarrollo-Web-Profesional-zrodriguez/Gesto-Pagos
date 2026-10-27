package com.proyecto.servicios.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.proyecto.servicios.exception.CatalogoCodigoError;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.SolicitudDuplicadaException;
import com.proyecto.servicios.model.gestopago.transaccion.PagoEstatusResponse;
import com.proyecto.servicios.service.ComprobanteEmailService;
import com.proyecto.servicios.service.PagoServicioService;

/**
 * Prueba de integración de la capa web: Controller + validación Bean Validation + deserialización JSON
 * + {@link GlobalExceptionHandler}, verificando los códigos HTTP y que no se filtre información sensible.
 */
@ExtendWith(MockitoExtension.class)
class PagoServicioControllerTest {

    private static final String BODY_VALIDO = """
            {"numCuenta":"8324433882000000","idServicio":76,"idProducto":205,"monto":150.00,
             "telefono":"5512345678","referencia":"25720381","2fa":"[0.12,-0.45,0.89,-0.05,0.67]"}
            """;

    @Mock
    private PagoServicioService pagoServicioService;
    @Mock
    private ComprobanteEmailService comprobanteEmailService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PagoServicioController(pagoServicioService, comprobanteEmailService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions pagar(String body, String idempotencyKey) throws Exception {
        var req = post("/pagos").contentType(MediaType.APPLICATION_JSON).content(body);
        if (idempotencyKey != null) {
            req.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(req);
    }

    @Test
    @DisplayName("Monto sin dos decimales => 400 (no se procesa)")
    void montoSinDecimales_400() throws Exception {
        pagar(BODY_VALIDO.replace("150.00", "150"), null).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cuenta con formato invalido => 400 con detalle de validacion")
    void cuentaInvalida_400() throws Exception {
        pagar(BODY_VALIDO.replace("8324433882000000", "123"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoCatalogo").value("DATOS_ENTRADA_INVALIDOS"));
    }

    @Test
    @DisplayName("JSON malformado => 400 sin reflejar el detalle del parser")
    void jsonMalformado_400() throws Exception {
        pagar("{ esto no es json", null)
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("Unexpected"))));
    }

    @Test
    @DisplayName("Saldo insuficiente => 422")
    void saldoInsuficiente_422() throws Exception {
        when(pagoServicioService.procesarPago(any(), any()))
                .thenThrow(new ReglaNegocioException(CatalogoCodigoError.SALDO_INSUFICIENTE, "Saldo insuficiente"));

        pagar(BODY_VALIDO, null).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value(203));
    }

    @Test
    @DisplayName("Solicitud duplicada => 409")
    void duplicada_409() throws Exception {
        when(pagoServicioService.procesarPago(any(), eq("clave-idempotencia-001")))
                .thenThrow(new SolicitudDuplicadaException("ABCDE12345", "PROCESANDO"));

        pagar(BODY_VALIDO, "clave-idempotencia-001").andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigoCatalogo").value("SOLICITUD_DUPLICADA"));
    }

    @Test
    @DisplayName("Resultado pendiente de conciliacion => 202 Accepted")
    void pendiente_202() throws Exception {
        when(pagoServicioService.procesarPago(any(), any()))
                .thenThrow(new ReglaNegocioException(CatalogoCodigoError.PAGO_PENDIENTE_CONCILIACION, "En verificacion. UPC: ABCDE12345"));

        pagar(BODY_VALIDO, null).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.codigo").value(504));
    }

    @Test
    @DisplayName("Rechazo del proveedor por IP no autorizada => 502 sin filtrar la IP")
    void ipNoAutorizada_502() throws Exception {
        when(pagoServicioService.procesarPago(any(), any()))
                .thenThrow(ReglaNegocioException.desdeGestoPago("55", "IP Origen no valida. [187.243.48.32]"));

        pagar(BODY_VALIDO, null).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigoGestoPago").value("55"))
                .andExpect(content().string(not(containsString("187.243"))));
    }

    @Test
    @DisplayName("Error inesperado => 500 generico sin mensaje interno")
    void errorInesperado_500() throws Exception {
        when(pagoServicioService.procesarPago(any(), any()))
                .thenThrow(new IllegalStateException("could not execute statement; password=secreto SQL [insert ...]"));

        pagar(BODY_VALIDO, null).andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("secreto"))))
                .andExpect(content().string(not(containsString("SQL"))));
    }

    @Test
    @DisplayName("Consultar estatus: inexistente => 404; existente => 200")
    void consultarEstatus() throws Exception {
        when(pagoServicioService.consultarEstatus("ZZZZZ99999"))
                .thenThrow(new RecursoNoEncontradoException(CatalogoCodigoError.TRANSACCION_NO_ENCONTRADA, "no existe"));
        mockMvc.perform(get("/pagos/estatus/ZZZZZ99999")).andExpect(status().isNotFound());

        when(pagoServicioService.consultarEstatus("ABCDE12345")).thenReturn(PagoEstatusResponse.builder()
                .upc("ABCDE12345").estatus("EXITOSA").definitivo(true).monto(new BigDecimal("150.00")).build());
        mockMvc.perform(get("/pagos/estatus/ABCDE12345")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("EXITOSA"));
    }
}
