package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.DispositivoDTO;
import com.descortezadores.adapters.dto.ErrorResponseDTO;
import com.descortezadores.adapters.dto.UsuarioDTO;
import com.descortezadores.domain.entities.Cuenta;
import com.descortezadores.domain.entities.CuentaDispositivo;
import com.descortezadores.domain.entities.Dispositivo;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.usecases.admin.GestionarCuentasUseCase;
import com.descortezadores.usecases.admin.GestionarDispositivosUseCase;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;

import java.util.List;
import java.util.UUID;

@Controller("/api/v1/admin")
@Secured({"Administrador"})
public class AdminController {

    private final GestionarCuentasUseCase gestionarCuentasUseCase;
    private final GestionarDispositivosUseCase gestionarDispositivosUseCase;

    public AdminController(
        GestionarCuentasUseCase gestionarCuentasUseCase,
        GestionarDispositivosUseCase gestionarDispositivosUseCase
    ) {
        this.gestionarCuentasUseCase = gestionarCuentasUseCase;
        this.gestionarDispositivosUseCase = gestionarDispositivosUseCase;
    }

    // CU-A01: Listar cuentas
    @Get("/cuentas")
    public List<UsuarioDTO> listarCuentas() {
        return gestionarCuentasUseCase.listarCuentas().stream()
            .map(this::toUsuarioDTO)
            .toList();
    }

    // CU-A02: Crear cuenta de usuario
    @Post("/cuentas")
    public HttpResponse<?> crearCuenta(@Body UsuarioDTO dto) {
        try {
            Cuenta creada = gestionarCuentasUseCase.crearCuenta(dto);
            return HttpResponse.created(toUsuarioDTO(creada));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(new ErrorResponseDTO(MensajesSistema.MSG31.getCodigo(), e.getMessage()));
        }
    }

    // CU-A03 / CU-A05: Editar datos y rol de cuenta
    @Put("/cuentas/{id}")
    public HttpResponse<?> actualizarCuenta(@PathVariable UUID id, @Body UsuarioDTO dto) {
        try {
            Cuenta actualizada = gestionarCuentasUseCase.actualizarCuenta(id, dto);
            return HttpResponse.ok(toUsuarioDTO(actualizada));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(new ErrorResponseDTO(MensajesSistema.MSG32.getCodigo(), e.getMessage()));
        }
    }

    // CU-A04: Desactivar cuenta
    @Patch("/cuentas/{id}/desactivar")
    public HttpResponse<?> desactivarCuenta(@PathVariable UUID id) {
        try {
            Cuenta desactivada = gestionarCuentasUseCase.cambiarEstado(id, "desactivada");
            return HttpResponse.ok(toUsuarioDTO(desactivada));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(new ErrorResponseDTO(MensajesSistema.MSG32.getCodigo(), e.getMessage()));
        }
    }

    // CU-A06: Gestión de dispositivos
    @Get("/dispositivos")
    public List<Dispositivo> listarDispositivos() {
        return gestionarDispositivosUseCase.listarTodosDispositivos();
    }

    @Post("/dispositivos")
    public HttpResponse<Dispositivo> registrarDispositivo(@Body DispositivoDTO dto) {
        return HttpResponse.created(gestionarDispositivosUseCase.registrarDispositivo(dto));
    }

    // CU-A07: Asociar dispositivo a cuenta
    @Post("/cuentas/{idCuenta}/dispositivos/{idDispositivo}")
    public HttpResponse<?> asociarDispositivo(@PathVariable UUID idCuenta, @PathVariable UUID idDispositivo) {
        try {
            CuentaDispositivo asociacion = gestionarDispositivosUseCase.asociarDispositivoACuenta(idCuenta, idDispositivo);
            return HttpResponse.ok(asociacion);
        } catch (IllegalStateException e) {
            return HttpResponse.badRequest(new ErrorResponseDTO(MensajesSistema.MSG33.getCodigo(), e.getMessage()));
        }
    }

    // CU-A08: Desasociar dispositivo
    @Delete("/cuentas/{idCuenta}/dispositivos/{idDispositivo}")
    public HttpResponse<?> desasociarDispositivo(@PathVariable UUID idCuenta, @PathVariable UUID idDispositivo) {
        try {
            gestionarDispositivosUseCase.desasociarDispositivo(idCuenta, idDispositivo);
            return HttpResponse.noContent();
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(new ErrorResponseDTO(MensajesSistema.MSG35.getCodigo(), e.getMessage()));
        }
    }

    // CU-A09: Consultar dispositivos asociados a una cuenta
    @Get("/cuentas/{idCuenta}/dispositivos")
    public List<CuentaDispositivo> listarDispositivosDeCuenta(@PathVariable UUID idCuenta) {
        return gestionarDispositivosUseCase.listarDispositivosPorCuenta(idCuenta);
    }

    private UsuarioDTO toUsuarioDTO(Cuenta cuenta) {
        return new UsuarioDTO(
            cuenta.idCuenta(),
            cuenta.correo(),
            null,
            cuenta.rol(),
            cuenta.estadoCuenta(),
            cuenta.fechaCreacion(),
            cuenta.nombreCompleto()
        );
    }
}
