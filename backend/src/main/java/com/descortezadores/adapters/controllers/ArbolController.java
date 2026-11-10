package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.AnalisisDetalleDTO;
import com.descortezadores.adapters.dto.ArbolDTO;
import com.descortezadores.adapters.dto.ErrorResponseDTO;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.usecases.detection.RegistrarDeteccionUseCase;
import com.descortezadores.usecases.history.AccesoDenegadoException;
import com.descortezadores.usecases.history.ConsultarHistorialUseCase;
import com.descortezadores.usecases.history.Solicitante;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

@Controller("/api/v1/arboles")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ArbolController {

    /** id_cuenta y rol del JWT que emite LoginController. */
    static Solicitante solicitante(Authentication authentication) {
        Object id = authentication.getAttributes().get("id_cuenta");
        return new Solicitante(id == null ? null : UUID.fromString(id.toString()),
            authentication.getRoles().contains(Solicitante.ROL_ADMINISTRADOR));
    }

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

    /**
     * RF-10, RF-20, CU-W06, CU-W11: Historial de capturas y análisis sanitarios de un árbol
     */
    @Get("/{id}/analisis")
    public List<AnalisisDetalleDTO> obtenerAnalisisDeArbol(@PathVariable UUID id) {
        return consultarHistorialUseCase.obtenerHistorialPorArbol(id);
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

    /**
     * RF-23, RN-41: Edición de datos de un árbol y actualización de observaciones
     */
    @Put("/{id}")
    public HttpResponse<?> actualizar(@PathVariable UUID id, @Body ArbolDTO dto) {
        try {
            Arbol actualizado = consultarHistorialUseCase.actualizarArbol(id, dto);
            return HttpResponse.ok(toDTO(actualizado));
        } catch (IllegalArgumentException e) {
            return HttpResponse.notFound(new ErrorResponseDTO(MensajesSistema.MSG14.getCodigo(), e.getMessage()));
        }
    }

    /** Solo el dueño del registro o un Administrador (403 para los demás). */
    @Delete("/{id}")
    public HttpResponse<?> eliminarArbol(@PathVariable UUID id, Authentication authentication) {
        try {
            consultarHistorialUseCase.eliminarArbol(id, solicitante(authentication));
            return HttpResponse.noContent();
        } catch (IllegalArgumentException e) {
            return HttpResponse.notFound(new ErrorResponseDTO(MensajesSistema.MSG14.getCodigo(), e.getMessage()));
        } catch (AccesoDenegadoException e) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDTO(MensajesSistema.MSG29.getCodigo(), e.getMessage()));
        }
    }

    /**
     * Vacía todo el catálogo: solo Administrador. La app móvil lo llama al limpiar su caché local
     * (envuelto en runCatching), así que para un experto el 403 no rompe esa limpieza.
     */
    @Delete
    @Secured({Solicitante.ROL_ADMINISTRADOR})
    public HttpResponse<?> eliminarTodosArboles() {
        consultarHistorialUseCase.eliminarTodosArboles();
        return HttpResponse.noContent();
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
