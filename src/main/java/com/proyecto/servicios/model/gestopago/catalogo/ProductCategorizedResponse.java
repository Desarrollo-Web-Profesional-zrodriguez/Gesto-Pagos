package com.proyecto.servicios.model.gestopago.catalogo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCategorizedResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("origen")
    private String origen; // "REDIS" o "POSTGRESQL"

    @JsonProperty("totalProductos")
    private Integer totalProductos;

    /**
     * Mapa donde la llave es el tipoFront (1, 2, etc.)
     * y el valor es la lista de productos pertenecientes a esa categoría.
     */
    @JsonProperty("categorias")
    private Map<Integer, List<ProductItemDto>> categorias;
}
