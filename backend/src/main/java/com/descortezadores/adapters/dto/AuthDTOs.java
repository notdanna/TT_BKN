package com.descortezadores.adapters.dto;

import io.micronaut.core.annotation.Nullable;
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
    public record LoginRequestDTO(
        @Nullable String username,
        @Nullable String correo,
        @Nullable String password,
        @Nullable String contrasena,
        @Nullable UUID idDispositivo,
        @Nullable String nombreDispositivo,
        @Nullable String identificadorUnico
    ) {
        public String getEmail() {
            if (username != null && !username.isBlank()) return username.trim();
            if (correo != null && !correo.isBlank()) return correo.trim();
            return null;
        }

        public String getSecret() {
            if (password != null && !password.isBlank()) return password;
            if (contrasena != null && !contrasena.isBlank()) return contrasena;
            return null;
        }
    }

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
