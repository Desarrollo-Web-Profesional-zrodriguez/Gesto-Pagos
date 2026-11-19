package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.catalogo.ProductCategorizedResponse;
import com.proyecto.servicios.service.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductoController {

    private final ProductoService productoService;

    // Inyección de dependencias por constructor
    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductCategorizedResponse> obtenerProductos() {
        ProductCategorizedResponse response = productoService.obtenerProductosCategorizados();
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
}
