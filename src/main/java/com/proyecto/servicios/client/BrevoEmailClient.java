package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.proyecto.servicios.model.email.BrevoSendEmailRequest;
import com.proyecto.servicios.model.email.BrevoSendEmailResponse;

@FeignClient(name = "brevoEmailClient", url = "${brevo.api.url:https://api.brevo.com}")
public interface BrevoEmailClient {

    @PostMapping(
            value = "/v3/smtp/email",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    BrevoSendEmailResponse sendTransacEmail(
            @RequestHeader("api-key") String apiKey,
            @RequestBody BrevoSendEmailRequest request
    );
}
