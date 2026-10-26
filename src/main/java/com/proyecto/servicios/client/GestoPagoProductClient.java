package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoProductClient", url = "${gestopago.service.url}")
public interface GestoPagoProductClient {

    @GetMapping(value = "/sistema/service/getProductList.do")
    String getProductList(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeaders
    );

    @PostMapping(value = "/sistema/service/sendTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String sendTx(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody MultiValueMap<String, String> formParams
    );

    @PostMapping(value = "/sistema/service/confirmTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String confirmTx(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestBody MultiValueMap<String, String> formParams
    );
}
