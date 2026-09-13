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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "productListResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("status")
    @XmlElement(name = "status")
    private Integer status;

    @JsonProperty("message")
    @XmlElement(name = "message")
    private String message;

    @JsonProperty("productos")
    @XmlElement(name = "producto")
    private List<ProductItemDto> productos;
}
