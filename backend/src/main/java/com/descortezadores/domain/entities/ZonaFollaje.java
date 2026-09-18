package com.descortezadores.domain.entities;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
@MappedEntity("zona_follaje")
public record ZonaFollaje(
    @Id
    @MappedProperty("id_zona")
    UUID idZona,

    @MappedProperty("latitud_centro")
    Double latitudCentro,

    @MappedProperty("longitud_centro")
    Double longitudCentro,

    @MappedProperty("fecha_registro")
    @Nullable
    Instant fechaRegistro,

    @MappedProperty("descripcion")
    @Nullable
    String descripcion,

    @MappedProperty("nombre_zona")
    String nombreZona,

    @MappedProperty("radio_metros")
    @Nullable
    Double radioMetros,

    @MappedProperty("id_dispositivo")
    @Nullable
    UUID idDispositivo,

    @MappedProperty("id_cuenta")
    UUID idCuenta
) {}
