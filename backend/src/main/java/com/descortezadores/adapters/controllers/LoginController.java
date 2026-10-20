package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.AuthDTOs;
import com.descortezadores.domain.entities.Cuenta;
import com.descortezadores.domain.entities.CuentaDispositivo;
import com.descortezadores.domain.entities.Dispositivo;
import com.descortezadores.infrastructure.persistence.CuentaDispositivoRepository;
import com.descortezadores.infrastructure.persistence.CuentaRepository;
import com.descortezadores.infrastructure.persistence.DispositivoRepository;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.handlers.LoginHandler;
import io.micronaut.security.rules.SecurityRule;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Controlador de Inicio de Sesión personalizado.
 * Expone POST /login y POST /api/v1/auth/login.
 * 
 * Flujo de ejecución:
 * 1. Valida correo y contraseña contra la base de datos (BCrypt + cuenta Activa).
 * 2. Si las credenciales son incorrectas, falla con 401 Unauthorized.
 * 3. Si son correctas y se proporciona idDispositivo (UUID):
 *    - Revisa si el dispositivo ya existe en la tabla DISPOSITIVO.
 *    - Si no existe, lo inserta en DISPOSITIVO (usando idDispositivo como identificador_unico si no se envía uno de hardware).
 *    - Si ya existe, actualiza su nombre si cambió.
 *    - Revisa si está enlazado a la cuenta en CUENTA_DISPOSITIVO; si no lo está, lo asocia en estado 'Activa'.
 * 4. Genera y retorna el token JWT estándar con id_cuenta, nombre_completo, rol e id_dispositivo.
 */
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
public class LoginController {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    private final CuentaRepository cuentaRepository;
    private final DispositivoRepository dispositivoRepository;
    private final CuentaDispositivoRepository cuentaDispositivoRepository;
    private final LoginHandler<HttpRequest<?>, MutableHttpResponse<?>> loginHandler;

    public LoginController(
            CuentaRepository cuentaRepository,
            DispositivoRepository dispositivoRepository,
            CuentaDispositivoRepository cuentaDispositivoRepository,
            LoginHandler<HttpRequest<?>, MutableHttpResponse<?>> loginHandler) {
        this.cuentaRepository = cuentaRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.cuentaDispositivoRepository = cuentaDispositivoRepository;
        this.loginHandler = loginHandler;
    }

    @Post("/login")
    public MutableHttpResponse<?> loginRoot(@Body AuthDTOs.LoginRequestDTO dto, HttpRequest<?> httpRequest) {
        return procesarLogin(dto, httpRequest);
    }

    @Post("/api/v1/auth/login")
    public MutableHttpResponse<?> loginApi(@Body AuthDTOs.LoginRequestDTO dto, HttpRequest<?> httpRequest) {
        return procesarLogin(dto, httpRequest);
    }

    private MutableHttpResponse<?> procesarLogin(AuthDTOs.LoginRequestDTO dto, HttpRequest<?> httpRequest) {
        if (dto == null) {
            return loginHandler.loginFailed(AuthenticationResponse.failure("Dato requerido faltante"), httpRequest);
        }

        String email = dto.getEmail();
        String secret = dto.getSecret();

        if (email == null || secret == null) {
            return loginHandler.loginFailed(AuthenticationResponse.failure("Usuario o contraseña no proporcionados"), httpRequest);
        }

        // 1. Validar que las credenciales sean correctas
        Optional<Cuenta> cuentaOpt = cuentaRepository.findByCorreo(email)
                .filter(c -> "Activa".equalsIgnoreCase(c.estadoCuenta()))
                .filter(c -> BCrypt.checkpw(secret, c.contrasenaHash()));

        if (cuentaOpt.isEmpty()) {
            LOG.warn("Intento de inicio de sesión fallido para el correo: {}", email);
            return loginHandler.loginFailed(AuthenticationResponse.failure("Credenciales incorrectas o cuenta inactiva"), httpRequest);
        }

        Cuenta cuenta = cuentaOpt.get();

        // 2. Ya confirmado eso, gestionar el dispositivo si viene en la petición
        UUID idDispositivoFinal = null;
        if (dto.idDispositivo() != null) {
            idDispositivoFinal = gestionarDispositivo(cuenta.idCuenta(), dto);
        }

        // 3. Flujo normal de login: emisión de token JWT con atributos de sesión
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id_cuenta", cuenta.idCuenta().toString());
        attributes.put("nombre_completo", cuenta.nombreCompleto());
        attributes.put("rol", cuenta.rol());
        if (idDispositivoFinal != null) {
            attributes.put("id_dispositivo", idDispositivoFinal.toString());
        }

        Authentication authentication = Authentication.build(
                cuenta.correo(),
                List.of(cuenta.rol()),
                attributes
        );

        LOG.info("Inicio de sesión exitoso para usuario: {} [Dispositivo: {}]", cuenta.correo(), idDispositivoFinal);
        return loginHandler.loginSuccess(authentication, httpRequest);
    }

    private UUID gestionarDispositivo(UUID idCuenta, AuthDTOs.LoginRequestDTO dto) {
        UUID idDisp = dto.idDispositivo();
        String nombreDisp = (dto.nombreDispositivo() != null && !dto.nombreDispositivo().isBlank())
                ? dto.nombreDispositivo().trim()
                : "Dispositivo Móvil";

        // Si no viene identificadorUnico de hardware, usamos el idDispositivo como identificador_unico
        String identUnico = (dto.identificadorUnico() != null && !dto.identificadorUnico().isBlank())
                ? dto.identificadorUnico().trim()
                : idDisp.toString();

        // A. Verificar existencia en la tabla DISPOSITIVO
        Optional<Dispositivo> dispOpt = dispositivoRepository.findById(idDisp);
        if (dispOpt.isEmpty()) {
            // Verificar si existe por identificadorUnico
            Optional<Dispositivo> porIdent = dispositivoRepository.findByIdentificadorUnico(identUnico);
            if (porIdent.isPresent()) {
                Dispositivo existente = porIdent.get();
                dispositivoRepository.update(new Dispositivo(
                        existente.idDispositivo(),
                        existente.identificadorUnico(),
                        nombreDisp,
                        existente.tokenDispositivo(),
                        "Activo",
                        existente.fechaRegistro()
                ));
                idDisp = existente.idDispositivo();
            } else {
                // Agregar nuevo dispositivo a la tabla
                Dispositivo nuevoDisp = new Dispositivo(
                        idDisp,
                        identUnico,
                        nombreDisp,
                        null,
                        "Activo",
                        Instant.now()
                );
                dispositivoRepository.save(nuevoDisp);
                LOG.info("Nuevo dispositivo registrado automáticamente: {} ({})", idDisp, nombreDisp);
            }
        } else {
            // Dispositivo existente: actualizar nombre si cambió
            Dispositivo existente = dispOpt.get();
            if (dto.nombreDispositivo() != null && !dto.nombreDispositivo().equals(existente.nombreDispositivo())) {
                dispositivoRepository.update(new Dispositivo(
                        existente.idDispositivo(),
                        existente.identificadorUnico(),
                        nombreDisp,
                        existente.tokenDispositivo(),
                        existente.estadoDispositivo(),
                        existente.fechaRegistro()
                ));
            }
        }

        // B. Verificar y registrar enlace en CUENTA_DISPOSITIVO (RN-02, RN-03)
        Optional<CuentaDispositivo> relOpt = cuentaDispositivoRepository.findByIdCuentaAndIdDispositivo(idCuenta, idDisp);
        if (relOpt.isEmpty()) {
            cuentaDispositivoRepository.save(CuentaDispositivo.asociar(idCuenta, idDisp));
            LOG.info("Dispositivo {} enlazado automáticamente con la cuenta {}", idDisp, idCuenta);
        } else if ("desasociada".equalsIgnoreCase(relOpt.get().estadoCuentaDispositivo())) {
            CuentaDispositivo rel = relOpt.get();
            cuentaDispositivoRepository.update(new CuentaDispositivo(
                    rel.idCuentaDispositivo(),
                    rel.idCuenta(),
                    rel.idDispositivo(),
                    Instant.now(),
                    null,
                    "Activa"
            ));
            LOG.info("Enlace de dispositivo {} con cuenta {} reactivado a 'Activa'", idDisp, idCuenta);
        }

        return idDisp;
    }
}
