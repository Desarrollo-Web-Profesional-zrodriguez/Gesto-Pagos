package com.proyecto.servicios.model.gestopago.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data // Genera automáticamente getters y setters (lombok)
@Builder // Construye objetos de forma fluida sin constructores grandes
@NoArgsConstructor // Constructor sin parámetros para Jackson (JSON)
@AllArgsConstructor // Constructor con parámetros para el patrón Builder
@JsonIgnoreProperties(ignoreUnknown = true) // Ignorar parámetros que no fueron considerados en este DTO
@XmlRootElement(name = "producto")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("idProducto")
    @XmlAttribute(name = "idProducto")
    private Long idProducto;

    @JsonProperty("idServicio")
    @XmlAttribute(name = "idServicio")
    private Long idServicio;

    @JsonProperty("servicio")
    @XmlAttribute(name = "servicio")
    private String servicio;

    @JsonProperty("producto")
    @XmlAttribute(name = "producto")
    private String producto;

    @JsonProperty("precio")
    @XmlAttribute(name = "precio")
    private BigDecimal precio;

    @JsonProperty("idCatTipoServicio")
    @XmlAttribute(name = "idCatTipoServicio")
    private Integer idCatTipoServicio;

    @JsonProperty("tipoFront")
    @XmlAttribute(name = "tipoFront")
    private Integer tipoFront;

    @JsonProperty("hasDigitoVerificador")
    @XmlAttribute(name = "hasDigitoVerificador")
    private Boolean hasDigitoVerificador;
    
    @JsonProperty("showAyuda")
    @XmlAttribute(name = "showAyuda")
    private Boolean showAyuda;

    @JsonProperty("tipoReferencia")
    @XmlAttribute(name = "tipoReferencia")
    private String tipoReferencia;

    @JsonProperty("legend")
    @XmlElement(name = "legend")
    private String legend;
}
