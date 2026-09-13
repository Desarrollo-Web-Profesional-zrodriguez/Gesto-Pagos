# Documentación Técnica: Integración de Catálogo de Productos (PuntoRed / GestoPago)

## 1. Resumen de la Solución
Se implementó el consumo del endpoint `GET /sistema/service/getProductList.do` respetando la arquitectura en capas existente en el proyecto, desacoplando credenciales mediante propiedades y asegurando el manejo controlado de errores y trazabilidad.

## 2. Decisiones Técnicas y Arquitectura
* **Capa Client (OpenFeign):** Se utilizó Spring Cloud OpenFeign (`GestoPagoProductClient`) para la comunicación declarativa HTTP. Se configuró compatibilidad con `application/json` y `application/xml` para soportar cualquier formato devuelto por el servicio `.do`.
* **Capa Service (`ProductoServiceImpl`):**
  * Inyección de dependencias por constructor garantizando Clean Code y facilitando pruebas unitarias.
  * Inyección de token dinámico (`Bearer <token>`) desde `application.properties` sin valores hardcodeados en código.
  * Manejo granular de excepciones:
    * `FeignException.Unauthorized` (401) para tokens vencidos/inválidos.
    * `RetryableException` (504) para caídas de red o timeouts.
    * `FeignException` general para respuestas HTTP no exitosas.
* **Capa Model / DTOs (`model.gestopago.catalogo`):** Separación modular de paquetes, soporte para deserialización JSON (`@JsonProperty`) y XML (`@XmlElement`, `@XmlRootElement`) y uso de `@JsonIgnoreProperties(ignoreUnknown = true)` para resiliencia ante cambios de esquema.
* **Capa Controller (`ProductoController`):** Exposición del endpoint REST `/productos` alineado a la convención existente de controladores.
* **Registro y Seguridad en Logs:** Trazabilidad de inicio, fin y recuento de productos usando SLF4J sin imprimir información confidencial (tokens ni contraseñas).

## 3. Pruebas Unitarias
Se implementaron tests en `ProductoServiceImplTest` con **JUnit 5** y **Mockito**, evaluando:
1. Caso exitoso (200 OK con mapeo de productos).
2. Validación de ausencia de token (401).
3. Manejo de timeouts de red (`RetryableException`).
