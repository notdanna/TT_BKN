package com.descortezadores.adapters.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
public record ZonaFollajeDTO(
    @Nullable UUID idZona,
    Double latitudCentro,
    Double longitudCentro,
    @Nullable Instant fechaRegistro,
    @Nullable String descripcion,
    String nombreZona,
    @Nullable Double radioMetros,
    @Nullable UUID idDispositivo,
    @Nullable UUID idCuenta
) {}
