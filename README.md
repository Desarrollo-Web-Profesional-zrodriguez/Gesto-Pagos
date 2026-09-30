# Documentación Técnica: Integración de Catálogo de Productos (PuntoRed / GestoPago)

## 1. Resumen de la Solución
Se implementó el consumo del endpoint `GET /sistema/service/getProductList.do` respetando la arquitectura en capas existente en el proyecto, desacoplando credenciales mediante propiedades y asegurando el manejo controlado de errores, trazabilidad y pruebas unitarias exhaustivas con JUnit 5 y Mockito.

---

## 2. Matriz de Cumplimiento de Entregables

A continuación se detalla la correspondencia entre cada uno de los requerimientos solicitados y los archivos del proyecto donde se da cumplimiento:

| Requerimiento / Criterio de Aceptación | Archivo(s) donde se cumple | Descripción de la Implementación |
| :--- | :--- | :--- |
| **Cliente Feign declarativo para endpoint externo** | [`GestoPagoProductClient.java`](file:///c:/Users/artes/Proyectos/prueba/src/main/java/com/proyecto/servicios/client/GestoPagoProductClient.java) | Cliente HTTP con Spring Cloud OpenFeign apuntando a `/sistema/service/getProductList.do` y recibiendo el header `Authorization: Bearer <token>`. |
| **Modelos de Datos / DTOs con soporte XML y JSON** | [`ProductItemDto.java`](file:///c:/Users/artes/Proyectos/prueba/src/main/java/com/proyecto/servicios/model/gestopago/catalogo/ProductItemDto.java)<br>[`ProductListResponse.java`](file:///c:/Users/artes/Proyectos/prueba/src/main/java/com/proyecto/servicios/model/gestopago/catalogo/ProductListResponse.java) | Mapeo de atributos (`@XmlAttribute`) y elementos (`@XmlElement`, CDATA `<legend>`), getters/setters con Lombok y serialización Jackson (`@JsonProperty`). |
| **Lógica de Negocio y Deserialización JAXB** | [`ProductoServiceImpl.java`](file:///c:/Users/artes/Proyectos/prueba/src/main/java/com/proyecto/servicios/service/Impl/ProductoServiceImpl.java) | Deserialización XML -> Java con JAXB, validación de token y asignación de status/mensaje de respuesta. |
| **Manejo Granular de Excepciones** | [`ProductoServiceImpl.java`](file:///c:/Users/artes/Proyectos/prueba/src/main/java/com/proyecto/servicios/service/Impl/ProductoServiceImpl.java) (Líneas 68-99) | Captura controlada de:<ul><li>`RetryableException` (504 Timeout)</li><li>`FeignException.Unauthorized` (401 Error de autenticación)</li><li>`FeignException` (Errores HTTP 4xx/5xx del proveedor)</li><li>`Exception` (500 Error interno / XML malformado)</li></ul> |
| **Desacoplamiento y Gestión Dinámica de Token** | [`GestoPagoTokenService.java`](file:///c:/Users/artes/Proyectos/prueba/src/main/java/com/proyecto/servicios/service/GestoPagoTokenService.java)<br>[`ProductoServiceImpl.java`](file:///c:/Users/artes/Proyectos/prueba/src/main/java/com/proyecto/servicios/service/Impl/ProductoServiceImpl.java) | Token obtenido y renovado dinámicamente mediante `GestoPagoTokenService`, con reintento automático y sin depender de tokens estáticos expirables. |
| **Controlador REST** | [`ProductoController.java`](file:///c:/Users/artes/Proyectos/prueba/src/main/java/com/proyecto/servicios/controller/ProductoController.java) | Exposición del endpoint `GET /productos` retornando `ResponseEntity<ProductListResponse>` en formato JSON. |
| **Trazabilidad y Logs Seguros** | [`ProductoServiceImpl.java`](file:///c:/Users/artes/Proyectos/prueba/src/main/java/com/proyecto/servicios/service/Impl/ProductoServiceImpl.java) | Logs informativos con SLF4J (inicio, fin, conteo de productos) sin registrar información sensible ni tokens. |
| **Pruebas Unitarias Automatizadas** | [`ProductoServiceImplTest.java`](file:///c:/Users/artes/Proyectos/prueba/src/test/java/com/proyecto/servicios/service/ProductoServiceImplTest.java) | Cobertura con JUnit 5 y Mockito para:<ul><li>Caso exitoso (200 OK y parseo de productos)</li><li>Token no configurado (401)</li><li>Timeout / Red (`RetryableException` -> 504)</li><li>Token rechazado (`FeignException.Unauthorized` -> 401)</li><li>Error de servidor (`FeignException` -> 500)</li><li>XML inválido / malformado (`Exception` -> 500)</li></ul> |

---

## 3. Decisiones Técnicas y Arquitectura
* **Inyección de Dependencias por Constructor:** En `ProductoServiceImpl` para facilitar el testing y respetar principios SOLID.
* **Resiliencia ante Cambios en el Esquema:** Uso de `@JsonIgnoreProperties(ignoreUnknown = true)` en los DTOs.
* **Compatibilidad JAXB:** Mapeo exacto de los atributos XML (`idProducto`, `servicio`, `producto`, `precio`, `hasDigitoVerificador`, `legend`) devueltos por GestoPago.

---

## 4. Ejecución de Pruebas

Para ejecutar la suite de pruebas unitarias:
```powershell
./gradlew test
```

