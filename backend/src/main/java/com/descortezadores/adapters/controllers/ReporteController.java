package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.ReporteDTOs;
import com.descortezadores.usecases.stats.GenerarReporteUseCase;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.time.Instant;
import java.util.UUID;

/**
 * Controlador de Generación y Descarga de Reportes (RF-16, RF-17, RN-33, RN-34, PT-12, CU-W23).
 */
@Controller("/api/v1/reportes")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ReporteController {

    private final GenerarReporteUseCase generarReporteUseCase;

    public ReporteController(GenerarReporteUseCase generarReporteUseCase) {
        this.generarReporteUseCase = generarReporteUseCase;
    }

    /**
     * RF-16 y RF-17: Generar y descargar el reporte oficial en formato PDF.
     */
    @Get("/pdf")
    @Produces("application/pdf")
    public HttpResponse<byte[]> descargarReportePdf(
            @Nullable @QueryValue Instant fechaInicio,
            @Nullable @QueryValue Instant fechaFin,
            @Nullable @QueryValue UUID idZona,
            @Nullable @QueryValue String estadoSanitario) {

        byte[] pdfBytes = generarReporteUseCase.generarPdf(fechaInicio, fechaFin, idZona, estadoSanitario);

        return HttpResponse.ok(pdfBytes)
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"reporte_monitoreo_chapultepec.pdf\"");
    }

    /**
     * CU-W23: Consultar los datos estructurados del reporte estadístico para visualización en el dashboard web.
     */
    @Get("/datos")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<ReporteDTOs.ReporteGeneralDTO> obtenerDatosReporte(
            @Nullable @QueryValue Instant fechaInicio,
            @Nullable @QueryValue Instant fechaFin,
            @Nullable @QueryValue UUID idZona,
            @Nullable @QueryValue String estadoSanitario) {

        ReporteDTOs.ReporteGeneralDTO datos = generarReporteUseCase.obtenerDatosReporte(
                fechaInicio, fechaFin, idZona, estadoSanitario);

        return HttpResponse.ok(datos);
    }
}
