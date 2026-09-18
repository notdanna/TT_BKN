package com.descortezadores.domain.entities;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

@Serdeable
@MappedEntity("cuenta_dispositivo")
public record CuentaDispositivo(
    @Id
    @MappedProperty("id_cuenta_dispositivo")
    UUID idCuentaDispositivo,

    @MappedProperty("id_cuenta")
    UUID idCuenta,

    @MappedProperty("id_dispositivo")
    UUID idDispositivo,

    @MappedProperty("fecha_asociacion")
    Instant fechaAsociacion,

    @MappedProperty("fecha_desasociacion")
    @Nullable
    Instant fechaDesasociacion,

    @MappedProperty("estado_cuenta_dispositivo")
    String estadoCuentaDispositivo
) {
    public static CuentaDispositivo asociar(UUID idCuenta, UUID idDispositivo) {
        return new CuentaDispositivo(
            UUID.randomUUID(),
            idCuenta,
            idDispositivo,
            Instant.now(),
            null,
            "Activa"
        );
    }
}
