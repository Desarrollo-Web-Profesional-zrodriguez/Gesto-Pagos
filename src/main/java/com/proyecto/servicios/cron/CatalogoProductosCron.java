package com.proyecto.servicios.cron;


import com.proyecto.servicios.entity.gestopago.GestoPagoProducto;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.gestopago.catalogo.ProductItemDto;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductoRepository;
import com.proyecto.servicios.service.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component 
@Slf4j 
public class CatalogoProductosCron {
    private final ProductoService productoService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GestoPagoProductoRepository productoRepository;
    private final GestoPagoProductoMapper productoMapper;

    @Value("${gestopago.redis.catalogo-key:gestopago:catalogo:productos_por_tipo_front}")
    private String redisKey;

    @Value("${gestopago.redis.catalogo-ttl-seconds:90000}")
    private Long redisTtlSeconds;
   
    public CatalogoProductosCron(ProductoService productoService,
                                 RedisTemplate<String, Object> redisTemplate,
                                 GestoPagoProductoRepository productoRepository,
                                 GestoPagoProductoMapper productoMapper) {
        this.productoService = productoService;
        this.redisTemplate = redisTemplate;
        this.productoRepository = productoRepository;
        this.productoMapper = productoMapper;
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

        log.info("GestoPago respondió 200 OK con {} productos. Categorizando por tipoFront...", productos.size());

        // Vategorizar por tipoFront
        Map<Integer, List<ProductItemDto>> productosPorTipoFront = productos.stream().collect(Collectors.groupingBy(p -> p.getTipoFront() != null ? p.getTipoFront() : 0));

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
    @Transactional 
    public void guardarEnBaseDatos(List<ProductItemDto> productos) {
        try {
            log.info("[POSTGRESQL] Limpiando registros anteriores y guardando {} productos...", productos.size());
            // Eliminar todo lo anterior siempre y cuando responde 200
            productoRepository.deleteAllInBatch();
            // Usamos MapStruct para convertir la lista de DTOs a entidades
            List<GestoPagoProducto> entidades = productoMapper.toEntityList(productos);
            // Guardamos todos los nuevos productos
            productoRepository.saveAll(entidades);
            log.info("[POSTGRESQL ÉXITO] Base de datos actualizada con {} productos de respaldos.", entidades.size());
        } catch (Exception e) {
            log.error("[POSTGRESQL ERROR] Error al guardar productos en PostgreSQL: {}", e.getMessage());
        }
    }
}

