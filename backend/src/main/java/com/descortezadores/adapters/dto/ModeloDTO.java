package com.descortezadores.adapters.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

/**
 * DTO con los metadatos de la versión del modelo de clasificación TFLite.
 * Utilizado por los dispositivos móviles para comprobar actualizaciones (PT-15, PI-07, PI-08).
 */
@Serdeable
public record ModeloDTO(
    String version,
    String nombreArchivo,
    Long tamanoBytes,
    String sha256,
    String urlDescarga,
    Instant fechaPublicacion
) {}
