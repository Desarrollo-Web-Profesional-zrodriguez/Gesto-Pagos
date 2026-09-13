package com.proyecto.servicios.client;

import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.proyecto.servicios.model.gestopago.catalogo.ProductListResponse;

@FeignClient(name = "gestoPagoProductClient", url = "${gestopago.service.url}")
public interface GestoPagoProductClient {
    @GetMapping(
        // Condumir el endpoint exacto
        value = "/sistema/service/getProductList.do",
        // Deserealizar formato JSON o XML
        consumes = {MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE}
    )
    ProductListResponse getProductList(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeaders // Enviar las cabeceras Bearer Token
    );
}
