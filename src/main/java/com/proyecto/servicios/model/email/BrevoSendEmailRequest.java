package com.proyecto.servicios.model.email;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrevoSendEmailRequest {
    private BrevoEmailContact sender;
    private List<BrevoEmailContact> to;
    private String subject;
    private String htmlContent;
}
