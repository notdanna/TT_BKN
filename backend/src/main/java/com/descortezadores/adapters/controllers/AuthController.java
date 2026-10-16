package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.AuthDTOs;
import com.descortezadores.adapters.dto.ErrorResponseDTO;
import com.descortezadores.domain.entities.Cuenta;
import com.descortezadores.infrastructure.persistence.CuentaRepository;
import com.descortezadores.usecases.auth.GestionarPasswordUseCase;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;

import java.util.Map;
import java.util.Optional;

/**
 * Controlador de Autenticación, Recuperación de Acceso y Perfil de Usuario.
 * Satisface los casos de uso:
 * - CU-G01: Inicio de sesión (conducido conjuntamente con Micronaut Security).
 * - CU-G02: Recuperar contraseña (RN-04, RN-05, RN-06).
 * - CU-G03: Restablecer contraseña (MSG07, MSG08, MSG09, MSG10).
 * - CU-G04: Cerrar sesión (MSG11, MSG12).
 * - CU-W09 / CU-M11: Consulta de perfil de usuario.
 */
@Controller("/api/v1/auth")
public class AuthController {

    private final GestionarPasswordUseCase gestionarPasswordUseCase;
    private final CuentaRepository cuentaRepository;

    public AuthController(GestionarPasswordUseCase gestionarPasswordUseCase, CuentaRepository cuentaRepository) {
        this.gestionarPasswordUseCase = gestionarPasswordUseCase;
        this.cuentaRepository = cuentaRepository;
    }

    /**
     * CU-G02: Solicitar recuperación de contraseña
     */
    @Post("/recuperar-password")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<?> solicitarRecuperacion(@Body AuthDTOs.RecuperarPasswordDTO dto) {
        try {
            if (dto == null || dto.correo() == null || dto.correo().isBlank()) {
                return HttpResponse.badRequest(new ErrorResponseDTO("MSG1", "Dato requerido faltante."));
            }
            String codigo = gestionarPasswordUseCase.solicitarRecuperacion(dto.correo());
            return HttpResponse.ok(Map.of(
                "mensaje", "Se ha enviado el enlace o código de recuperación a su correo electrónico.",
                "codigoRecuperacion", codigo,
                "vigenciaHoras", 24
            ));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(new ErrorResponseDTO("ERROR_RECUPERACION", e.getMessage()));
        } catch (IllegalStateException e) {
            return HttpResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponseDTO("RN-06", e.getMessage()));
        }
    }

    /**
     * CU-G03: Restablecer contraseña con código/token
     */
    @Post("/restablecer-password")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<?> restablecerContrasena(@Body AuthDTOs.RestablecerPasswordDTO dto) {
        try {
            if (dto == null) {
                return HttpResponse.badRequest(new ErrorResponseDTO("MSG1", "Dato requerido faltante."));
            }
            gestionarPasswordUseCase.restablecerContrasena(
                dto.token(),
                dto.nuevaContrasena(),
                dto.confirmacionContrasena()
            );
            return HttpResponse.ok(Map.of(
                "mensaje", "La contraseña fue restablecida correctamente."
            ));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(new ErrorResponseDTO("ERROR_RESTABLECER", e.getMessage()));
        }
    }

    /**
     * CU-G04: Cerrar sesión
     */
    @Post("/logout")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<?> cerrarSesion(Authentication authentication) {
        if (authentication == null) {
            return HttpResponse.badRequest(new ErrorResponseDTO("MSG11", "No existe una sesión activa."));
        }
        return HttpResponse.ok(Map.of(
            "mensaje", "Sesión finalizada correctamente.",
            "usuario", authentication.getName()
        ));
    }

    /**
     * CU-W09 / CU-M11: Consultar perfil del usuario autenticado
     */
    @Get("/perfil")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<?> obtenerPerfil(Authentication authentication) {
        if (authentication == null) {
            return HttpResponse.unauthorized();
        }

        Optional<Cuenta> cuentaOpt = cuentaRepository.findByCorreo(authentication.getName());
        if (cuentaOpt.isEmpty()) {
            return HttpResponse.notFound();
        }

        Cuenta c = cuentaOpt.get();
        AuthDTOs.PerfilUsuarioDTO perfil = new AuthDTOs.PerfilUsuarioDTO(
            c.idCuenta(),
            c.correo(),
            c.nombreCompleto(),
            c.rol(),
            c.estadoCuenta(),
            c.fechaCreacion()
        );

        return HttpResponse.ok(perfil);
    }
}
