package com.descortezadores.adapters.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
public record SyncItemDTO(
    UUID idAnalisisLocal,
    @Nullable
    UUID idArbol,
    @Nullable
    String nombreArbolNuevo,
    @Nullable
    String especieArbolNuevo,
    Double latitud,
    Double longitud,
    Instant fechaCaptura,
    String tipoImagen, // "corteza" o "follaje"
    String resultadoClasificacion, // "sano", "infestado", "no_determinado"
    Double confianza,
    @Nullable
    String observaciones,
    @Nullable
    Double pctIncierto,
    @Nullable
    Double pctDanado,
    @Nullable
    Double pctRegular,
    @Nullable
    Double pctSano,
    @Nullable
    String imagenBase64, // imagen codificada en base64 para sincronización directa
    @Nullable
    UUID idDispositivo
) {}
