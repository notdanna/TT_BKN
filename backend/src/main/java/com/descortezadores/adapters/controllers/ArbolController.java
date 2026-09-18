package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.ArbolDTO;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.usecases.detection.RegistrarDeteccionUseCase;
import com.descortezadores.usecases.history.ConsultarHistorialUseCase;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

@Controller("/api/v1/arboles")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ArbolController {

    private final RegistrarDeteccionUseCase registrarDeteccionUseCase;
    private final ConsultarHistorialUseCase consultarHistorialUseCase;

    public ArbolController(
        RegistrarDeteccionUseCase registrarDeteccionUseCase,
        ConsultarHistorialUseCase consultarHistorialUseCase
    ) {
        this.registrarDeteccionUseCase = registrarDeteccionUseCase;
        this.consultarHistorialUseCase = consultarHistorialUseCase;
    }

    @Get
    public List<ArbolDTO> listar() {
        return consultarHistorialUseCase.listarTodosArboles().stream()
            .map(this::toDTO)
            .toList();
    }

    @Get("/{id}")
    public HttpResponse<ArbolDTO> obtenerPorId(@PathVariable UUID id) {
        return consultarHistorialUseCase.obtenerArbolPorId(id)
            .map(this::toDTO)
            .map(HttpResponse::ok)
            .orElseGet(HttpResponse::notFound);
    }

    @Post
    public HttpResponse<ArbolDTO> registrar(@Body ArbolDTO dto, Authentication authentication) {
        UUID idCuenta = UUID.fromString("a0000000-0000-0000-0000-000000000002");
        if (authentication != null && authentication.getAttributes().containsKey("id_cuenta")) {
            idCuenta = UUID.fromString((String) authentication.getAttributes().get("id_cuenta"));
        } else if (dto.idCuenta() != null) {
            idCuenta = dto.idCuenta();
        }

        Arbol guardado = registrarDeteccionUseCase.registrarArbol(dto, idCuenta);
        return HttpResponse.created(toDTO(guardado));
    }

    private ArbolDTO toDTO(Arbol entity) {
        return new ArbolDTO(
            entity.idArbol(),
            entity.nombreEspecifico(),
            entity.especie(),
            entity.latitud(),
            entity.longitud(),
            entity.fechaRegistro(),
            entity.descripcion(),
            entity.idCuenta(),
            entity.idDispositivo()
        );
    }
}
