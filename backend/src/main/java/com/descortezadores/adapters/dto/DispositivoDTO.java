package com.descortezadores.adapters.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
public record DispositivoDTO(
    @Nullable
    UUID idDispositivo,
    String identificadorUnico,
    @Nullable
    String nombreDispositivo,
    @Nullable
    String tokenDispositivo,
    @Nullable
    String estadoDispositivo,
    @Nullable
    Instant fechaRegistro
) {}
