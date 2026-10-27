package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.util.DatosSensibles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}", initialDelay = 0)
    public void renovarToken() {
        String dispLimpio = codigoDispositivo != null ? codigoDispositivo.trim() : "";
        String passLimpio = password != null ? password.trim() : "";
        log.info("Renovando token GestoPago para distribuidor={}", idDistribuidor);
        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, dispLimpio, passLimpio);

            if (response == null || response.getToken() == null || response.getToken().trim().isEmpty()) {
                log.error("La respuesta de GestoPago no contiene un token valido (status={}, message={})",
                        response != null ? response.getStatus() : null,
                        response != null ? DatosSensibles.sanitizarTextoProveedor(response.getMessage()) : "sin respuesta");
                return;
            }

            response.setToken(response.getToken().trim());

            GestoPagoToken tokenEntity = tokenRepository
                    .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, dispLimpio)
                    .map(existing -> {
                        tokenMapper.updateEntity(response, existing);
                        existing.setActivo(true);
                        return existing;
                    })
                    .orElseGet(() -> {
                        GestoPagoToken nuevo = tokenMapper.toEntity(response);
                        nuevo.setIdDistribuidor(idDistribuidor);
                        nuevo.setCodigoDispositivo(dispLimpio);
                        nuevo.setActivo(true);
                        return nuevo;
                    });

            tokenRepository.save(tokenEntity);
            log.info("Token GestoPago renovado correctamente");

        } catch (Exception e) {
            log.error("Error al renovar token GestoPago ({})", e.getClass().getSimpleName());
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        String dispLimpio = codigoDispositivo != null ? codigoDispositivo.trim() : "";
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, dispLimpio);
    }

    @Override
    public String obtenerTokenValido() {
        String dispLimpio = codigoDispositivo != null ? codigoDispositivo.trim() : "";
        Optional<GestoPagoToken> tokenOpt = obtenerTokenActivo(idDistribuidor, dispLimpio);
        if (tokenOpt.isPresent()
                && tokenOpt.get().getToken() != null
                && !tokenOpt.get().getToken().trim().isEmpty()
                && Boolean.TRUE.equals(tokenOpt.get().getActivo())) {
            return tokenOpt.get().getToken().trim();
        }

        log.info("No se encontro un token activo en BD. Renovando token para distribuidor={}...", idDistribuidor);
        renovarToken();

        return obtenerTokenActivo(idDistribuidor, dispLimpio)
                .map(GestoPagoToken::getToken)
                .filter(t -> !t.trim().isEmpty())
                .map(String::trim)
                .orElse(null);
    }
}
