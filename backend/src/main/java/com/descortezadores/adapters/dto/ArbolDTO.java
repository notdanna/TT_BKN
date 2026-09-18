package com.descortezadores.adapters.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
public record ArbolDTO(
    @Nullable
    UUID idArbol,
    String nombreEspecifico,
    String especie,
    @Nullable
    Double latitud,
    @Nullable
    Double longitud,
    @Nullable
    Instant fechaRegistro,
    @Nullable
    String descripcion,
    @Nullable
    UUID idCuenta,
    @Nullable
    UUID idDispositivo
) {}
