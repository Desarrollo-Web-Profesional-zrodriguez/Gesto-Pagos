package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.gestopago.catalogo.ProductCategorizedResponse;
import com.proyecto.servicios.model.gestopago.catalogo.ProductItemDto;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.ProductoService;
import feign.FeignException;
import feign.RetryableException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private final GestoPagoProductClient productClient;
    private final GestoPagoTokenService tokenService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GestoPagoProductoRepository productoRepository;
    private final GestoPagoProductoMapper productoMapper;
    private final ObjectMapper objectMapper;

    @Value("${gestopago.redis.catalogo-key:gestopago:catalogo:productos_por_tipo_front}")
    private String redisKey;
    
    public ProductoServiceImpl(GestoPagoProductClient productClient,
                               GestoPagoTokenService tokenService,
                               RedisTemplate<String, Object> redisTemplate,
                               GestoPagoProductoRepository productoRepository,
                               GestoPagoProductoMapper productoMapper) {
        this.productClient = productClient;
        this.tokenService = tokenService;
        this.redisTemplate = redisTemplate;
        this.productoRepository = productoRepository;
        this.productoMapper = productoMapper;
        this.objectMapper = new ObjectMapper();
    }

    /**    
     * Endpoint interno consumido por el Cron a las 6:00 AM para traer el XML de GestoPago
     */
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

     /**
     * Método que consumirá el Controlador REST.
     * NO llama a GestoPago. Consulta Redis y si no hay datos, consulta PostgreSQL.
     */
    @Override 
    public ProductCategorizedResponse obtenerProductosCategorizados() {
        log.info("Consultando catálogo categorizado para el cliente(sin llamar a GestoPago)...");

        // Intentar consultar desde Redis (el último registro)}
        try {
            Object cacheData = redisTemplate.opsForValue().get(redisKey);
            if(cacheData != null) {
                log.info("[REDIS] Catálogo encontrado en memoria.");

                // convertimos el JSON de Redis de forma segura a Map<Integer, List<ProductoItemDto>>
                Map<Integer, List<ProductItemDto>> categorias = objectMapper.convertValue(
                    cacheData, 
                    new TypeReference<Map<Integer, List<ProductItemDto>>>() {}
                );

                if (!categorias.isEmpty()) {
                    // Garantizar orden de menor a mayor en las categorías (0, 1, 2...)
                    Map<Integer, List<ProductItemDto>> categoriasOrdenadas = new TreeMap<>(categorias);
                    int total = categoriasOrdenadas.values().stream().mapToInt(List::size).sum();

                    return ProductCategorizedResponse.builder()
                            .status(200)
                            .message("Catálogo obtenido exitosamente")
                            .origen("REDIS")
                            .totalProductos(total)
                            .categorias(categoriasOrdenadas)
                            .build();
                }
            }
        } catch (Exception e) {
            log.warn("[REDIS ERROR] No se pudo leer de Redis: {}. Buscando respaldo en PostgreSQL...", e.getMessage());
        }

        // Si redis no tiene respuesta o falló, consultar en PostgreSQL
        log.info("[POSTGRESQL] Consulta productos almacenados en base de datos...");
        List<GestoPagoProducto> productosBD = productoRepository.findAllByOrderByTipoFrontAscIdProductoAsc();

        if (productosBD != null && !productosBD.isEmpty()) {
            // Usamos MapStruct para convertir de Entidad a DTO
            List<ProductItemDto> dtos = productoMapper.toDtoList(productosBD);

            // Normalizar categoría 0 si viene sin tipoFront
            dtos.forEach(p -> {
                if (p.getTipoFront() == null) {
                    p.setTipoFront(0);
                }
            });

            // Agrupamos por tipoFront conservando el orden de menor a mayor
            Map<Integer, List<ProductItemDto>> categoriasBD = dtos.stream()
                    .collect(Collectors.groupingBy(
                            ProductItemDto::getTipoFront,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            return ProductCategorizedResponse.builder()
                    .status(200)
                    .message("Catálogo obtenido exitosamente desde base de datos")
                    .origen("POSTGRESQL")
                    .totalProductos(productosBD.size())
                    .categorias(categoriasBD)
                    .build();
        }

        // Si ni en Redis ni en BD hay datos
        log.warn("No se encontró catálogo disponible en Redis ni en PostgreSQL.");
        return ProductCategorizedResponse.builder()
        .status(404)
        .message("No hay catálogo disponible en caché ni en base de datos. Espere la sincronización de las 6:00 AM")
        .origen("NINGUNO")
        .totalProductos(0)
        .categorias(Collections.emptyMap())
        .build();
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