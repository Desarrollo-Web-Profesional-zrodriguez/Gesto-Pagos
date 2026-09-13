package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;
import com.proyecto.servicios.service.ProductoService;
import feign.FeignException;
import feign.RetryableException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.Collections;

@Service
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private final GestoPagoProductClient productClient;

    @Value("${gestopago.service.token}")
    private String tokenConfigurado;

    public ProductoServiceImpl(GestoPagoProductClient productClient) {
        this.productClient = productClient;
    }

    @Override
    public ProductListResponse obtenerListaProductos() {
        log.info("Iniciamos la invocacion al servicio externo de catalogo de productos");

        if (tokenConfigurado == null || tokenConfigurado.trim().isEmpty()) {
            log.error("Error de configuracion: No se encontro el token de autenticacion configurado");
            return ProductListResponse.builder()
                    .status(401)
                    .message("Token de autenticacion no configurado")
                    .productos(Collections.emptyList())
                    .build();
        }

        try {
            String authHeader = tokenConfigurado.startsWith("Bearer ")
                    ? tokenConfigurado
                    : "Bearer " + tokenConfigurado;

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

        } catch (RetryableException e) {
            log.error("Error de timeout o comunicacion con el servicio externo de productos: {}", e.getMessage());
            return ProductListResponse.builder()
                    .status(504)
                    .message("Tiempo de espera agotado al conectar con el servicio externo")
                    .productos(Collections.emptyList())
                    .build();

        } catch (FeignException.Unauthorized e) {
            log.error("Error de autenticacion (401) con el servicio externo.");
            return ProductListResponse.builder()
                    .status(401)
                    .message("Error de autenticacion con el proveedor externo")
                    .productos(Collections.emptyList())
                    .build();

        } catch (FeignException e) {
            log.error("Respuesta no exitosa del servicio externo de productos. HTTP Status: {}", e.status());
            return ProductListResponse.builder()
                    .status(e.status())
                    .message("El servicio externo respondio con error HTTP " + e.status())
                    .productos(Collections.emptyList())
                    .build();

        } catch (Exception e) {
            log.error("Error inesperado durante la consulta de productos: {}", e.getMessage(), e);
            return ProductListResponse.builder()
                    .status(500)
                    .message("Error interno al procesar la lista de productos")
                    .productos(Collections.emptyList())
                    .build();
        }
    }
}
