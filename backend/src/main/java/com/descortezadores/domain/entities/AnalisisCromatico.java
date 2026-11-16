package com.descortezadores.domain.entities;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
@MappedEntity("analisis_cromatico")
public record AnalisisCromatico(
    @Id
    @MappedProperty("id_analisis_cromatico")
    UUID idAnalisisCromatico,

    @MappedProperty("pct_incierto")
    Double pctIncierto,

    @MappedProperty("pct_danado")
    Double pctDanado,

    @MappedProperty("pct_regular")
    Double pctRegular,

    @MappedProperty("pct_sano")
    Double pctSano,

    @MappedProperty("id_analisis")
    UUID idAnalisis
) {
    public static AnalisisCromatico nuevo(
        Double pctIncierto,
        Double pctDanado,
        Double pctRegular,
        Double pctSano,
        UUID idAnalisis
    ) {
        return new AnalisisCromatico(
            UUID.randomUUID(),
            pctIncierto != null ? pctIncierto : 0.0,
            pctDanado != null ? pctDanado : 0.0,
            pctRegular != null ? pctRegular : 0.0,
            pctSano != null ? pctSano : 0.0,
            idAnalisis
        );
    }
}
