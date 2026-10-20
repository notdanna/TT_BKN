package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.AnalisisDetalleDTO;
import com.descortezadores.adapters.dto.ErrorResponseDTO;
import com.descortezadores.adapters.dto.ZonaFollajeDTO;
import com.descortezadores.domain.entities.ZonaFollaje;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.usecases.history.ConsultarHistorialUseCase;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
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

    @Post
    public HttpResponse<ZonaFollajeDTO> registrar(@Body ZonaFollajeDTO dto, Authentication authentication) {
        UUID idCuenta = UUID.fromString("a0000000-0000-0000-0000-000000000002");
        if (authentication != null && authentication.getAttributes().containsKey("id_cuenta")) {
            idCuenta = UUID.fromString((String) authentication.getAttributes().get("id_cuenta"));
        } else if (dto.idCuenta() != null) {
            idCuenta = dto.idCuenta();
        }

        ZonaFollaje guardada = consultarHistorialUseCase.registrarZonaFollaje(dto, idCuenta);
        return HttpResponse.created(toDTO(guardada));
    }

    @Put("/{id}")
    public HttpResponse<?> actualizar(@PathVariable UUID id, @Body ZonaFollajeDTO dto) {
        try {
            ZonaFollaje actualizada = consultarHistorialUseCase.actualizarZonaFollaje(id, dto);
            return HttpResponse.ok(toDTO(actualizada));
        } catch (IllegalArgumentException e) {
            return HttpResponse.notFound(new ErrorResponseDTO(MensajesSistema.MSG14.getCodigo(), e.getMessage()));
        }
    }

    @Delete("/{id}")
    public HttpResponse<?> eliminarZona(@PathVariable UUID id) {
        consultarHistorialUseCase.eliminarZonaFollaje(id);
        return HttpResponse.noContent();
    }

    @Delete
    public HttpResponse<?> eliminarTodasZonas() {
        consultarHistorialUseCase.eliminarTodasZonasFollaje();
        return HttpResponse.noContent();
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
