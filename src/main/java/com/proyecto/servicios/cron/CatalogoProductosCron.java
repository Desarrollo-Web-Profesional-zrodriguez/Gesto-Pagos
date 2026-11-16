package com.proyecto.servicios.cron;


import com.proyecto.servicios.model.gestopago.catalogo.ProductItemDto;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;
import com.proyecto.servicios.service.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component 
@Slf4j 
public class CatalogoProductosCron {
    private final ProductoService productoService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${gestopago.redis.catalogo-key:gestopago:catalogo:productos_por_tipo_front}")
    private String redisKey;

    @Value("${gestopago.redis.catalogo-ttl-seconds:90000}")
    private Long redisTtlSeconds;
   
    public CatalogoProductosCron(ProductoService productoService,
                                 RedisTemplate<String, Object> redisTemplate) {
        this.productoService = productoService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Cron que se dispara a las 6:00
     */
    @Scheduled(cron = "${gestopago.cron.catalogo:0 0 6 * * *}", zone = "${gestopago.cron.zone:America/Mexico_City}")
    public void sincronizarCatalogoProductos() {
        log.info("[CRON 6:00 AM] Iniciando Sinxronización del catálogo de GestoPago...");

        ProductListResponse respuesta = productoService.obtenerListaProductos();

        // Validamos que el sevicio responda 200
        if (respuesta == null || respuesta.getStatus() == null || respuesta.getStatus() != 200) {
            log.error("El servicio de GestoPago no respondio 200 OK. Estado recibido: {}. No se actualzia Redis ni la Base de datos.", respuesta != null ? respuesta.getStatus() : "NULO");
            return;
        }

        List<ProductItemDto> productos = respuesta.getProductos();
        if (productos == null || productos.isEmpty()) {
            log.warn("GestoPago respondió 200 OK pero la lista de productos esta vacía.");
            return;
        }

        log.info("GestoPago respondió 200 OK con {} productos. Normalizando categoría 0 y ordenando de menor a mayor...", productos.size());

        // 1. Si no tiene categoría (tipoFront == null), asignar categoría 0
        productos.forEach(p -> {
            if (p.getTipoFront() == null) {
                p.setTipoFront(0);
            }
        });

        // 2. Ordenar de menor a mayor: primero por tipoFront (0, 1, 2...) y luego por idProducto
        productos.sort(Comparator
                .comparing(ProductItemDto::getTipoFront)
                .thenComparing(ProductItemDto::getIdProducto, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        // 3. Agrupar conservando el orden de menor a mayor (LinkedHashMap)
        Map<Integer, List<ProductItemDto>> productosPorTipoFront = productos.stream()
                .collect(Collectors.groupingBy(
                        ProductItemDto::getTipoFront,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        boolean guardadoEnRedisExistoso = false;

        // Intento primario Guardar en Redis
        try {
            log.info("Intento guardar catálogo categorizado en Redis en la clave '{}'...", redisKey);
            redisTemplate.opsForValue().set(redisKey, productosPorTipoFront, Duration.ofSeconds(redisTtlSeconds));
            guardadoEnRedisExistoso = true;
            log.info("[REDISÉXITO] Catálogo guardado en Redis correctamente. NO se guarda en base de datos PostgresSQL.");
        } catch (Exception e) {
            log.warn("[REDIS ERROR] Falló la conexión o guardado en Redis: {}. Disparando fallback a PostgreSQL...", e.getMessage());
        }

        // Fallback: Si Redis Falló, se eliminan los anteriores y se guardan los nuevos en PostgreSQL
        if (!guardadoEnRedisExistoso) {
            guardarEnBaseDatos(productos);
        }
    }

    /**
     * Fallback para guardar/actualizar en PostgreSQL si Redis Falló.
     */
    public void guardarEnBaseDatos(List<ProductItemDto> productos) {
        try {
            productoService.guardarEnBaseDatos(productos);
        } catch (Exception e) {
            log.error("[POSTGRESQL ERROR] Error al guardar productos en PostgreSQL: {}", e.getMessage());
        }
    }
}

