package com.descortezadores.adapters.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

public class AuthDTOs {

    @Serdeable
    public record RecuperarPasswordDTO(String correo) {}

    @Serdeable
    public record RestablecerPasswordDTO(
        String token,
        String nuevaContrasena,
        String confirmacionContrasena
    ) {}

    @Serdeable
    public record LoginCustomRequestDTO(
        String correo,
        String contrasena,
        UUID idDispositivo
    ) {}

    @Serdeable
    public record PerfilUsuarioDTO(
        UUID idCuenta,
        String correo,
        String nombreCompleto,
        String rol,
        String estadoCuenta,
        Instant fechaCreacion
    ) {}
}
