package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.catalogo.ProductCategorizedResponse;
import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;

public interface ProductoService {

    /**
     * Consulta directamente a GestoPago (usado exclusivamente por el Cron de las 6 AM)
     */
    ProductListResponse obtenerListaProductos();

    /**
     * Consulta el catálogo categorizado para el Controlador.
     * 1. Consulta el último registro disponible en Redis
     * 2. Si Redis no tiene respuesta o falla, consulta el respaldo en PostgreSQL
     */
    ProductCategorizedResponse obtenerProductosCategorizados();
} 