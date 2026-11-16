package com.descortezadores.domain.entities;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
@MappedEntity("analisis")
public record Analisis(
    @Id
    @MappedProperty("id_analisis")
    UUID idAnalisis,

    @MappedProperty("ruta_evidencia")
    String rutaEvidencia,

    @MappedProperty("latitud_captura")
    @Nullable
    Double latitudCaptura,

    @MappedProperty("longitud_captura")
    @Nullable
    Double longitudCaptura,

    @MappedProperty("fecha_captura")
    Instant fechaCaptura,

    @MappedProperty("fecha_sincronizacion")
    @Nullable
    Instant fechaSincronizacion,

    @MappedProperty("estado_sincronizacion")
    String estadoSincronizacion,

    @MappedProperty("tipo_imagen")
    String tipoImagen,

    @MappedProperty("resultado_clasificacion")
    String resultadoClasificacion,

    @MappedProperty("confianza")
    @Nullable
    Double confianza,

    @MappedProperty("observaciones")
    @Nullable
    String observaciones,

    @MappedProperty("id_arbol")
    @Nullable
    UUID idArbol,

    @MappedProperty("id_zona")
    @Nullable
    UUID idZona
) {
    public static Analisis nuevoCorteza(
        String rutaEvidencia,
        Double latitud,
        Double longitud,
        Instant fechaCaptura,
        String resultadoClasificacion,
        Double confianza,
        String observaciones,
        UUID idArbol
    ) {
        return new Analisis(
            UUID.randomUUID(),
            rutaEvidencia,
            latitud,
            longitud,
            fechaCaptura,
            Instant.now(),
            "sincronizado",
            "corteza",
            resultadoClasificacion,
            confianza,
            observaciones,
            idArbol,
            null
        );
    }
}
