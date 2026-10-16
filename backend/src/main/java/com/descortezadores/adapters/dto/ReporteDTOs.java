package com.descortezadores.adapters.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReporteDTOs {

    /**
     * Elemento del reporte cumpliendo con las columnas mínimas de RN-34:
     * identificador del árbol, nombre del árbol, fecha y hora de captura,
     * resultado de clasificación, coordenadas GPS y nombre del experto.
     */
    @Serdeable
    public record ReporteItemDTO(
        UUID idArbol,
        String nombreArbol,
        Instant fechaCaptura,
        String resultadoClasificacion,
        Double latitud,
        Double longitud,
        String nombreExperto,
        String tipoImagen
    ) {}

    @Serdeable
    public record ReporteGeneralDTO(
        String titulo,
        Instant fechaGeneracion,
        Instant filtroFechaInicio,
        Instant filtroFechaFin,
        int totalCapturas,
        int totalSanos,
        int totalInfestados,
        int totalNoDeterminados,
        List<ReporteItemDTO> items
    ) {}
}
