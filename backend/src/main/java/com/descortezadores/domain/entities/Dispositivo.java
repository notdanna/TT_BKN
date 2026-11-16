package com.descortezadores.domain.entities;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
@MappedEntity("dispositivo")
public record Dispositivo(
    @Id
    @MappedProperty("id_dispositivo")
    UUID idDispositivo,

    @MappedProperty("identificador_unico")
    String identificadorUnico,

    @MappedProperty("nombre_dispositivo")
    @Nullable
    String nombreDispositivo,

    @MappedProperty("token_dispositivo")
    @Nullable
    String tokenDispositivo,

    @MappedProperty("estado_dispositivo")
    String estadoDispositivo,

    @MappedProperty("fecha_registro")
    @Nullable
    Instant fechaRegistro
) {
    public static Dispositivo nuevo(String identificadorUnico, String nombreDispositivo) {
        return new Dispositivo(
            UUID.randomUUID(),
            identificadorUnico,
            nombreDispositivo,
            null,
            "Activo",
            Instant.now()
        );
    }
}
