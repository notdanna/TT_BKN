package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.AnalisisDetalleDTO;
import com.descortezadores.adapters.dto.ZonaFollajeDTO;
import com.descortezadores.domain.entities.ZonaFollaje;
import com.descortezadores.usecases.history.ConsultarHistorialUseCase;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

@Controller("/api/v1/zonas-follaje")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ZonaFollajeController {

    private final ConsultarHistorialUseCase consultarHistorialUseCase;

    public ZonaFollajeController(ConsultarHistorialUseCase consultarHistorialUseCase) {
        this.consultarHistorialUseCase = consultarHistorialUseCase;
    }

    @Get
    public List<ZonaFollajeDTO> listar() {
        return consultarHistorialUseCase.listarTodasZonasFollaje().stream()
            .map(this::toDTO)
            .toList();
    }

    @Get("/{id}")
    public HttpResponse<ZonaFollajeDTO> obtenerPorId(@PathVariable UUID id) {
        return consultarHistorialUseCase.obtenerZonaFollajePorId(id)
            .map(this::toDTO)
            .map(HttpResponse::ok)
            .orElseGet(HttpResponse::notFound);
    }

    @Get("/{id}/analisis")
    public List<AnalisisDetalleDTO> obtenerAnalisisPorZona(@PathVariable UUID id) {
        return consultarHistorialUseCase.obtenerHistorialPorZona(id);
    }

    private ZonaFollajeDTO toDTO(ZonaFollaje z) {
        return new ZonaFollajeDTO(
            z.idZona(),
            z.latitudCentro(),
            z.longitudCentro(),
            z.fechaRegistro(),
            z.descripcion(),
            z.nombreZona(),
            z.radioMetros(),
            z.idDispositivo(),
            z.idCuenta()
        );
    }
}
