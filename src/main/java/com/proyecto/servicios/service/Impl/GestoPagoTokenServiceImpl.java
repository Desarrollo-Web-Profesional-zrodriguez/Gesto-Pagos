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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private static final long HORAS_VIGENCIA_SEGURA = 20;

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;
    private final AtomicReference<String> tokenEnMemoria = new AtomicReference<>();

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

    /**
     * De acuerdo con la especificación técnica de PuntoRed, cada JWT tiene una vigencia de 24 horas
     * y no se debe solicitar un nuevo token en cada petición si ya existe uno vigente.
     */
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}", initialDelay = 0)
    public void verificarTokenProgramado() {
        String dispLimpio = codigoDispositivo != null ? codigoDispositivo.trim() : "";
        try {
            Optional<GestoPagoToken> tokenOpt = obtenerTokenActivo(idDistribuidor, dispLimpio);
            if (tokenOpt.isPresent()) {
                GestoPagoToken actual = tokenOpt.get();
                boolean tieneToken = actual.getToken() != null && !actual.getToken().trim().isEmpty();
                boolean vigente = actual.getFechaActualizacion() != null
                        && actual.getFechaActualizacion().isAfter(LocalDateTime.now().minusHours(HORAS_VIGENCIA_SEGURA));
                if (tieneToken && Boolean.TRUE.equals(actual.getActivo()) && vigente) {
                    tokenEnMemoria.set(actual.getToken().trim());
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo verificar el token en BD durante la revision programada ({})", e.getClass().getSimpleName());
        }
        renovarToken();
    }

    @Override
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

            String tokenLimpio = response.getToken().trim();
            response.setToken(tokenLimpio);
            tokenEnMemoria.set(tokenLimpio);

            GestoPagoToken tokenEntity = tokenRepository
                    .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, dispLimpio)
                    .map(existing -> {
                        if (tokenMapper != null) {
                            tokenMapper.updateEntity(response, existing);
                        }
                        existing.setToken(tokenLimpio);
                        existing.setTokenType(response.getTokenType());
                        existing.setExpiresIn(response.getExpiresIn());
                        existing.setActivo(true);
                        return existing;
                    })
                    .orElseGet(() -> {
                        GestoPagoToken nuevo = tokenMapper != null ? tokenMapper.toEntity(response) : new GestoPagoToken();
                        if (nuevo == null) {
                            nuevo = new GestoPagoToken();
                        }
                        nuevo.setToken(tokenLimpio);
                        nuevo.setTokenType(response.getTokenType());
                        nuevo.setExpiresIn(response.getExpiresIn());
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
            String tokenBd = tokenOpt.get().getToken().trim();
            tokenEnMemoria.set(tokenBd);
            return tokenBd;
        }

        log.info("No se encontro un token activo en BD. Renovando token para distribuidor={}...", idDistribuidor);
        renovarToken();

        Optional<String> recienGuardado = obtenerTokenActivo(idDistribuidor, dispLimpio)
                .map(GestoPagoToken::getToken)
                .filter(t -> !t.trim().isEmpty())
                .map(String::trim);
        if (recienGuardado.isPresent()) {
            return recienGuardado.get();
        }
        String enMemoria = tokenEnMemoria.get();
        return (enMemoria != null && !enMemoria.isBlank()) ? enMemoria.trim() : null;
    }
}
