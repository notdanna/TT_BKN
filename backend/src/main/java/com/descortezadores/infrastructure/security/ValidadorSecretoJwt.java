package com.descortezadores.infrastructure.security;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;

import java.nio.charset.StandardCharsets;

/**
 * Detiene el arranque si el secreto HS256 de los JWT es débil o es el que estuvo publicado en el
 * repositorio. Con ese secreto cualquiera podría firmar un token de Administrador.
 */
@Context
public class ValidadorSecretoJwt {

    /** Mínimo para HS256: 256 bits. */
    static final int BYTES_MINIMOS = 32;

    /** Estuvo como valor por defecto en application.yml; sigue en el historial de git. */
    private static final String PUBLICADO = "9f8a8b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a";

    public ValidadorSecretoJwt(
            @Value("${micronaut.security.token.jwt.signatures.secret.generator.secret}") String secreto) {
        validar(secreto);
    }

    public static void validar(String secreto) {
        if (secreto == null || secreto.isBlank() || secreto.getBytes(StandardCharsets.UTF_8).length < BYTES_MINIMOS) {
            throw new IllegalStateException("JWT_GENERATOR_SIGNATURE_SECRET debe tener al menos " + BYTES_MINIMOS
                    + " bytes (openssl rand -base64 48).");
        }
        if (PUBLICADO.equals(secreto.trim())) {
            throw new IllegalStateException("JWT_GENERATOR_SIGNATURE_SECRET es el secreto que estuvo publicado en el "
                    + "repositorio: genera uno nuevo.");
        }
    }
}
