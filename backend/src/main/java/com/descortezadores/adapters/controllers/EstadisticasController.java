package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.DashboardEstadisticasDTO;
import com.descortezadores.adapters.dto.FocoInfestacionDTO;
import com.descortezadores.usecases.stats.ConsultarEstadisticasUseCase;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;

/**
 * Controlador de Estadísticas y Análisis Geoespacial
 * RF-14, RF-21, RN-25, RN-27, CU-W08, CU-W18, CU-W19-CU-W23
 */
@Controller("/api/v1/estadisticas")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class EstadisticasController {

    private final ConsultarEstadisticasUseCase consultarEstadisticasUseCase;

    public EstadisticasController(ConsultarEstadisticasUseCase consultarEstadisticasUseCase) {
        this.consultarEstadisticasUseCase = consultarEstadisticasUseCase;
    }

    @Get("/dashboard")
    public DashboardEstadisticasDTO obtenerDashboard() {
        return consultarEstadisticasUseCase.obtenerDashboardEstadisticas();
    }

    @Get("/focos")
    public List<FocoInfestacionDTO> listarFocosActivos() {
        return consultarEstadisticasUseCase.calcularFocosActivos();
    }
}
