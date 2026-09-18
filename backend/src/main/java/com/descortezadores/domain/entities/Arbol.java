package com.descortezadores.domain.entities;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
@MappedEntity("arbol")
public record Arbol(
    @Id
    @MappedProperty("id_arbol")
    UUID idArbol,

    @MappedProperty("nombre_especifico")
    String nombreEspecifico,

    @MappedProperty("especie")
    String especie,

    @MappedProperty("longitud")
    @Nullable
    Double longitud,

    @MappedProperty("latitud")
    @Nullable
    Double latitud,

    @MappedProperty("fecha_registro")
    @Nullable
    Instant fechaRegistro,

    @MappedProperty("descripcion")
    @Nullable
    String descripcion,

    @MappedProperty("id_cuenta")
    UUID idCuenta,

    @MappedProperty("id_dispositivo")
    @Nullable
    UUID idDispositivo
) {
    public static Arbol nuevo(
        String nombreEspecifico,
        String especie,
        Double latitud,
        Double longitud,
        String descripcion,
        UUID idCuenta,
        UUID idDispositivo
    ) {
        return new Arbol(
            UUID.randomUUID(),
            nombreEspecifico,
            especie,
            longitud,
            latitud,
            Instant.now(),
            descripcion,
            idCuenta,
            idDispositivo
        );
    }
}
