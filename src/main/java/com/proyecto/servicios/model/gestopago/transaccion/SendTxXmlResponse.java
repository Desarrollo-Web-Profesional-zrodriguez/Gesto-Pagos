package com.proyecto.servicios.model.gestopago.transaccion;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class SendTxXmlResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(name = "ID_TX")
    private String idTx;

    @XmlElement(name = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @XmlElement(name = "SALDO")
    private String saldo;

    @XmlElement(name = "COMISION")
    private String comision;

    @XmlElement(name = "SALDO_F")
    private String saldoF;

    @XmlElement(name = "COMISION_F")
    private String comisionF;

    @XmlElement(name = "FECHA")
    private String fecha;

    @XmlElement(name = "MONTO")
    private String monto;

    @XmlElement(name = "MENSAJE")
    private MensajeTxXml mensaje;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MensajeTxXml implements Serializable {

        private static final long serialVersionUID = 1L;

        @XmlElement(name = "CODIGO")
        private String codigo;

        @XmlElement(name = "TEXTO")
        private String texto;

        @XmlElement(name = "REFERENCIA")
        private String referencia;

        @XmlElement(name = "PIN")
        private String pin;

        @XmlElement(name = "legend")
        private String legend;

        @XmlElement(name = "ID_TX")
        private String idTx;
    }
}
