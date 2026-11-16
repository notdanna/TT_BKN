package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.SyncBatchResponse;
import com.descortezadores.adapters.dto.SyncItemDTO;
import com.descortezadores.usecases.sync.SyncDeteccionUseCase;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;

import java.util.List;
import java.util.UUID;

/**
 * Controlador de sincronización (SyncController)
 * Expone POST /sync y POST /api/v1/sync/detecciones según PI-01 y §8.2.5.
 */
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
public class SyncController {

    private final SyncDeteccionUseCase syncDeteccionUseCase;

    public SyncController(SyncDeteccionUseCase syncDeteccionUseCase) {
        this.syncDeteccionUseCase = syncDeteccionUseCase;
    }

    @Post("/sync")
    public HttpResponse<SyncBatchResponse> sincronizar(@Body List<SyncItemDTO> items, Authentication authentication) {
        return procesarSincronizacion(items, authentication);
    }

    @Post("/api/v1/sync/detecciones")
    public HttpResponse<SyncBatchResponse> sincronizarV1(@Body List<SyncItemDTO> items, Authentication authentication) {
        return procesarSincronizacion(items, authentication);
    }

    private HttpResponse<SyncBatchResponse> procesarSincronizacion(List<SyncItemDTO> items, Authentication authentication) {
        UUID idCuenta = UUID.fromString("a0000000-0000-0000-0000-000000000002");
        if (authentication != null && authentication.getAttributes().containsKey("id_cuenta")) {
            idCuenta = UUID.fromString((String) authentication.getAttributes().get("id_cuenta"));
        }

        SyncBatchResponse response = syncDeteccionUseCase.sincronizar(items, idCuenta);
        return HttpResponse.ok(response);
    }
}
