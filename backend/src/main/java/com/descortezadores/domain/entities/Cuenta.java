package com.descortezadores.domain.entities;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
@MappedEntity("cuenta")
public record Cuenta(
    @Id
    @MappedProperty("id_cuenta")
    UUID idCuenta,

    @MappedProperty("correo")
    String correo,

    @MappedProperty("contrasena_hash")
    String contrasenaHash,

    @MappedProperty("rol")
    String rol,

    @MappedProperty("estado_cuenta")
    String estadoCuenta,

    @MappedProperty("fecha_creacion")
    @Nullable
    Instant fechaCreacion,

    @MappedProperty("nombre_completo")
    String nombreCompleto
) {
    public static Cuenta nuevo(String correo, String contrasenaHash, String rol, String nombreCompleto) {
        return new Cuenta(
            UUID.randomUUID(),
            correo,
            contrasenaHash,
            rol,
            "Activa",
            Instant.now(),
            nombreCompleto
        );
    }
}
