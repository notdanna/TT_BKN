package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.AnalisisDetalleDTO;
import com.descortezadores.usecases.history.ConsultarHistorialUseCase;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Controlador de Análisis y Evidencias
 * RF-10, RF-20, RF-22, CU-W06, CU-W10-CU-W15
 */
@Controller("/api/v1/analisis")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class AnalisisController {

    private final ConsultarHistorialUseCase consultarHistorialUseCase;

    public AnalisisController(ConsultarHistorialUseCase consultarHistorialUseCase) {
        this.consultarHistorialUseCase = consultarHistorialUseCase;
    }

    @Get
    public List<AnalisisDetalleDTO> listarTodos() {
        return consultarHistorialUseCase.obtenerTodosLosAnalisis();
    }

    @Get("/{id}")
    public HttpResponse<AnalisisDetalleDTO> obtenerPorId(@PathVariable UUID id) {
        return consultarHistorialUseCase.obtenerAnalisisPorId(id)
            .map(HttpResponse::ok)
            .orElseGet(HttpResponse::notFound);
    }

    /**
     * RN-30, RN-31, RF-12, CU-M05: Resumen de capturas de la jornada actual (00:00 - 23:59)
     */
    @Get("/jornada")
    public List<AnalisisDetalleDTO> listarJornadaActual() {
        Instant inicio = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant fin = inicio.plus(1, ChronoUnit.DAYS);
        return consultarHistorialUseCase.obtenerAnalisisPorRango(inicio, fin);
    }
}
