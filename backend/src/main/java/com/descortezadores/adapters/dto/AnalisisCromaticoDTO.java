package com.descortezadores.adapters.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record AnalisisCromaticoDTO(
    UUID idAnalisisCromatico,
    Double pctIncierto,
    Double pctDanado,
    Double pctRegular,
    Double pctSano
) {
    public AnalisisCromaticoDTO(Double pctIncierto, Double pctDanado, Double pctRegular, Double pctSano) {
        this(null, pctIncierto, pctDanado, pctRegular, pctSano);
    }
}
