package com.descortezadores.domain.entities;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de dominio para solicitudes de recuperación de contraseña.
 * Cumple con:
 * - RN-04: Vigencia máxima de 24 horas.
 * - RN-05: Enlace de un solo uso; una vez utilizado queda invalidado.
 * - RN-06: Máximo 3 solicitudes en un periodo de 30 días.
 */
@Serdeable
public record TokenRecuperacion(
    UUID idToken,
    UUID idCuenta,
    String correo,
    String token,
    Instant fechaCreacion,
    Instant fechaExpiracion,
    boolean usado
) {
    public boolean estaExpirado() {
        return Instant.now().isAfter(fechaExpiracion);
    }

    public boolean esValido() {
        return !usado && !estaExpirado();
    }
}
