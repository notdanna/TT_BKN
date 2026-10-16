package com.descortezadores.adapters.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.UUID;

/**
 * Representa un Foco de Infestación Activo según NOM-019-SEMARNAT-2017:
 * 3 o más árboles clasificados como infestados dentro de una superficie de 1,000 m² (RN-25).
 */
@Serdeable
public record FocoInfestacionDTO(
    String idFoco,
    Double latitudCentro,
    Double longitudCentro,
    Double radioMetros,
    int cantidadArbolesInfectados,
    List<UUID> idsArboles,
    List<String> nombresArboles
) {}
