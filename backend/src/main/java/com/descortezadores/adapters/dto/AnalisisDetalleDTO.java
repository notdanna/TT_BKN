package com.descortezadores.adapters.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
public record AnalisisDetalleDTO(
    UUID idAnalisis,
    String rutaEvidencia,
    @Nullable Double latitudCaptura,
    @Nullable Double longitudCaptura,
    Instant fechaCaptura,
    @Nullable Instant fechaSincronizacion,
    String estadoSincronizacion,
    String tipoImagen,
    String resultadoClasificacion,
    @Nullable Double confianza,
    @Nullable String observaciones,
    @Nullable UUID idArbol,
    @Nullable UUID idZona,
    @Nullable AnalisisCromaticoDTO cromatico
) {}
