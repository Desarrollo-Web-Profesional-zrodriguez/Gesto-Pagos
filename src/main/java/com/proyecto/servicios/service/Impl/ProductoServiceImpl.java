package com.proyecto.servicios.service.Impl;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;
import com.proyecto.servicios.service.ProductoService;

import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;

@Service // Hacerlo un seervicio
@Slf4j  // Generar los logs
public class ProductoServiceImpl implements ProductoService {
    private final GestoPagoProductClient productClient;
    
    @Value("${gestopago.service.token}")
    private String tokenConfigurado;

    // Inyección de dependencias por el constructor
    public ProductoServiceImpl(GestoPagoProductClient productClient){
        this.productClient = productClient;
    }

    @Override 
    public ProductListResponse  obtenerListaProductos(){
        log.info("Iniciamos la invocación al servicio externo de catálogo de productos");

        // Validamos que exista el token
        if(tokenConfigurado == null || tokenConfigurado.trim().isEmpty()) {
            log.error("Error de configuracion: No se encontro el token de autenticacion cpnfigurado");
            return ProductListResponse.builder()
            .status(401)
            .message("Token de autenticación no configurado")
            .productos(Collections.emptyList())
            .build();
        }
    
        try{
            // Formatear la cabecera Berear Token
            String authHeader = tokenConfigurado.startsWith("Bearer ")
            ? tokenConfigurado
            : "Bearer " + tokenConfigurado;

            // Consumir el cliente externo
            ProductListResponse response = productClient.getProductList(authHeader);

            int totalProductos = (response != null && response.getProductos() != null)
            ? response.getProductos().size()
            : 0;

            log.info("Invocacion al servicio externo finalizada existosamente. Total prodcutos recibidos: {}", totalProductos);
            return response;
        } catch (RetryableException e) {
            // Manejo de timeouts y caídas de red
            log.error("Error de timeout o comunicacion con el servicio externo de prodcutos {}", e.getMessage());
            return ProductListResponse.builder()
            .status(501)
            .message("Timepo de espera agotado al conectar con el servicio externo")
            .productos(Collections.emptyList())
            .build();
        } catch (FeignException.Unauthorized e) {
            // Manejo de error 401
            log.error("Error de autenticación (401) con el servicio externo. Verificar las credenciales");
            return ProductListResponse.builder()
            .status(401)
            .message("Error de autenticacion con el proveedor externo")
            .productos(Collections.emptyList())
            .build();
        } catch (FeignException e) {
            // Manejo de errores de HTTP no exitosos (4xx, 5xx)
            log.error("Respuesta no exitosa del servicio externo de productos. HTTP Status: {}", e.status());
            return ProductListResponse.builder()
            .status(e.status())
            .message("El servicio externo respondio con error HTTP " + e.status())
            .productos(Collections.emptyList())
            .build();
        } catch (Exception e) {
            // Error general inesperado
            log.error("Error inesperado durante la consulta de productos: {}", e.getMessage(), e);
            return ProductListResponse.builder()
            .status(500)
            .message("Error interno al processar la lista de productos")
            .productos(Collections.emptyList())
            .build();
        }
    }
}
