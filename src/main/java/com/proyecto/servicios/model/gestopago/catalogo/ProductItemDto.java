package com.proyecto.servicios.model.gestopago.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data  // Genera automáticamente getters y setters (lombok)
@Builder // Construye objetos de forma fluida sin constructores grandes
@NoArgsConstructor // Constructor sin parámetros para Jackson (JSON)
@AllArgsConstructor // Constructor con parámetros para el patrón Builder
@JsonIgnoreProperties(ignoreUnknown = true) // Ignorar parámetros que no fueron considerados en este DTO
@XmlRootElement(name = "producto")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("idProducto")
    @XmlElement(name = "idProducto")
    private Long idProducto;

    @JsonProperty("nombre")
    @XmlElement(name = "nombre")
    private String nombre;

    @JsonProperty("descripcion")
    @XmlElement(name = "descripcion")
    private String descripcion;

    @JsonProperty("categoria")
    @XmlElement(name = "categoria")
    private String categoria;

    @JsonProperty("precio")
    @XmlElement(name = "precio")
    private BigDecimal precio;

    @JsonProperty("activo")
    @XmlElement(name = "activo")
    private Boolean activo;
}
