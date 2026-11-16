package com.descortezadores.adapters.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
public record UsuarioDTO(
    @Nullable
    UUID idCuenta,
    String correo,
    @Nullable
    String contrasena,
    String rol, // "Administrador" o "Experto de campo"
    @Nullable
    String estadoCuenta, // "Activa" o "desactivada"
    @Nullable
    Instant fechaCreacion,
    String nombreCompleto
) {}
