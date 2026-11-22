package com.descortezadores.adapters.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ActualizarAnalisisObservacionesDTO(
    @Nullable String observaciones
) {}
