package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoProductClient", url = "${gestopago.service.url}")
public interface GestoPagoProductClient {

    @GetMapping(value = "/sistema/service/getProductList.do")
    String getProductList(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeaders
    );
}
