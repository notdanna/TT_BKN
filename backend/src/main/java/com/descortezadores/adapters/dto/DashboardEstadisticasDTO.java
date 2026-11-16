package com.descortezadores.adapters.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.Map;

/**
 * DTO para el resumen del dashboard web (RF-21, CU-W08, RN-44).
 */
@Serdeable
public record DashboardEstadisticasDTO(
    long totalArboles,
    long totalSanos,
    long totalInfestados,
    long totalNoDeterminado,
    long totalAnalisis,
    long totalZonasFollaje,
    long totalFocosActivos,
    Map<String, Long> distribucionPorEspecie,
    Map<String, Long> distribucionPorEstado,
    boolean alertaEstacionalActiva,
    String mensajeAlertaEstacional
) {}
