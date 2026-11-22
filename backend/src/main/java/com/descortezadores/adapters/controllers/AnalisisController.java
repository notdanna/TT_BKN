package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.ActualizarAnalisisObservacionesDTO;
import com.descortezadores.adapters.dto.AnalisisDetalleDTO;
import com.descortezadores.adapters.dto.ErrorResponseDTO;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.usecases.history.AccesoDenegadoException;
import com.descortezadores.usecases.history.ConsultarHistorialUseCase;
import com.descortezadores.usecases.history.Solicitante;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Put;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
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
    public List<AnalisisDetalleDTO> listarTodos(Authentication authentication) {
        if (authentication != null
            && !authentication.getRoles().contains("Administrador")
            && authentication.getAttributes().containsKey("id_cuenta")) {
            UUID idCuenta = UUID.fromString(authentication.getAttributes().get("id_cuenta").toString());
            return consultarHistorialUseCase.obtenerTodosLosAnalisisPorCuenta(idCuenta);
        }
        return consultarHistorialUseCase.obtenerTodosLosAnalisis();
    }

    @Get("/{id}")
    public HttpResponse<AnalisisDetalleDTO> obtenerPorId(@PathVariable UUID id) {
        return consultarHistorialUseCase.obtenerAnalisisPorId(id)
            .map(HttpResponse::ok)
            .orElseGet(HttpResponse::notFound);
    }

    /**
     * Actualiza las observaciones individuales de una captura/análisis específico.
     */
    @Put("/{id}")
    public HttpResponse<?> actualizarObservaciones(
        @PathVariable UUID id,
        @Body ActualizarAnalisisObservacionesDTO dto,
        Authentication authentication
    ) {
        try {
            Solicitante solicitante = authentication != null ? ArbolController.solicitante(authentication) : null;
            String observaciones = dto != null ? dto.observaciones() : null;
            AnalisisDetalleDTO actualizado = consultarHistorialUseCase.actualizarObservacionesAnalisis(
                id,
                observaciones,
                solicitante
            );
            return HttpResponse.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return HttpResponse.notFound(new ErrorResponseDTO(MensajesSistema.MSG14.getCodigo(), e.getMessage()));
        } catch (AccesoDenegadoException e) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDTO(MensajesSistema.MSG29.getCodigo(), e.getMessage()));
        }
    }

    /**
     * RN-30, RN-31, RF-12, CU-M05: Resumen de capturas de la jornada actual (00:00 - 23:59)
     */
    @Get("/jornada")
    public List<AnalisisDetalleDTO> listarJornadaActual(Authentication authentication) {
        Instant inicio = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant fin = inicio.plus(1, ChronoUnit.DAYS);
        if (authentication != null
            && !authentication.getRoles().contains("Administrador")
            && authentication.getAttributes().containsKey("id_cuenta")) {
            UUID idCuenta = UUID.fromString(authentication.getAttributes().get("id_cuenta").toString());
            return consultarHistorialUseCase.obtenerAnalisisPorRangoYCuenta(inicio, fin, idCuenta);
        }
        return consultarHistorialUseCase.obtenerAnalisisPorRango(inicio, fin);
    }
}
