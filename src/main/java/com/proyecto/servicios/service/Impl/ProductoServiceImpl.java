package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.ProductoService;
import feign.FeignException;
import feign.RetryableException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.Collections;

@Service
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private final GestoPagoProductClient productClient;
    private final GestoPagoTokenService tokenService;

    public ProductoServiceImpl(GestoPagoProductClient productClient, GestoPagoTokenService tokenService) {
        this.productClient = productClient;
        this.tokenService = tokenService;
    }

    @Override
    public ProductListResponse obtenerListaProductos() {
        log.info("Iniciamos la invocacion al servicio externo de catalogo de productos");

        String token = tokenService.obtenerTokenValido();

        if (token == null || token.trim().isEmpty()) {
            log.error("Error de autenticacion: No se encontro un token dinamico disponible");
            return ProductListResponse.builder()
                    .status(401)
                    .message("Token de autenticacion no disponible")
                    .productos(Collections.emptyList())
                    .build();
        }

        try {
            return consultarProductosConToken(token);
        } catch (FeignException e) {
            // Si el token expiro (401 o 403), forzamos renovacion inmediata y reintentamos 1 vez
            if (e.status() == 401 || e.status() == 403) {
                log.warn("El servicio externo respondio HTTP {}. Renovando token y reintentando...", e.status());
                tokenService.renovarToken();
                String nuevoToken = tokenService.obtenerTokenValido();
                if (nuevoToken != null && !nuevoToken.equals(token)) {
                    try {
                        return consultarProductosConToken(nuevoToken);
                    } catch (Exception retryEx) {
                        return manejarExcepcion(retryEx);
                    }
                }
            }
            return manejarExcepcion(e);
        } catch (Exception e) {
            return manejarExcepcion(e);
        }
    }

    private ProductListResponse consultarProductosConToken(String token) throws Exception {
        String authHeader = token.startsWith("Bearer ")
                ? token
                : "Bearer " + token;

        // Invocación HTTP al servicio externo
        String xmlResponse = productClient.getProductList(authHeader);

        // Deserialización XML -> Java con JAXB
        JAXBContext context = JAXBContext.newInstance(ProductListResponse.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ProductListResponse response = (ProductListResponse) unmarshaller.unmarshal(new StringReader(xmlResponse));

        response.setStatus(200);
        if (response.getMensaje() != null && response.getMensaje().getTexto() != null) {
            response.setMessage(response.getMensaje().getTexto());
        } else {
            response.setMessage("Operacion realizada con exito");
        }

        int total = (response.getProductos() != null) ? response.getProductos().size() : 0;
        log.info("Invocacion finalizada exitosamente. Total productos recibidos: {}", total);

        return response;
    }

    private ProductListResponse manejarExcepcion(Exception e) {
        if (e instanceof RetryableException) {
            log.error("Error de timeout o comunicacion con el servicio externo de productos: {}", e.getMessage());
            return ProductListResponse.builder()
                    .status(504)
                    .message("Tiempo de espera agotado al conectar con el servicio externo")
                    .productos(Collections.emptyList())
                    .build();
        } else if (e instanceof FeignException.Unauthorized) {
            log.error("Error de autenticacion (401) con el servicio externo.");
            return ProductListResponse.builder()
                    .status(401)
                    .message("Error de autenticacion con el proveedor externo")
                    .productos(Collections.emptyList())
                    .build();
        } else if (e instanceof FeignException fe) {
            log.error("Respuesta no exitosa del servicio externo de productos. HTTP Status: {}", fe.status());
            return ProductListResponse.builder()
                    .status(fe.status())
                    .message("El servicio externo respondio con error HTTP " + fe.status())
                    .productos(Collections.emptyList())
                    .build();
        } else {
            log.error("Error inesperado durante la consulta de productos: {}", e.getMessage(), e);
            return ProductListResponse.builder()
                    .status(500)
                    .message("Error interno al procesar la lista de productos")
                    .productos(Collections.emptyList())
                    .build();
        }
    }
}