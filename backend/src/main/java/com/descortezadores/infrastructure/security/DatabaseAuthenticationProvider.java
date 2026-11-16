package com.descortezadores.infrastructure.security;

import com.descortezadores.infrastructure.persistence.CuentaRepository;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import jakarta.inject.Singleton;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Map;

@Singleton
public class DatabaseAuthenticationProvider implements HttpRequestAuthenticationProvider<Object> {

    private final CuentaRepository cuentaRepository;

    public DatabaseAuthenticationProvider(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public AuthenticationResponse authenticate(
        @Nullable HttpRequest<Object> httpRequest,
        @NonNull AuthenticationRequest<String, String> authenticationRequest
    ) {
        String correo = authenticationRequest.getIdentity();
        String secret = authenticationRequest.getSecret();

        return cuentaRepository.findByCorreo(correo)
            .filter(cuenta -> "Activa".equalsIgnoreCase(cuenta.estadoCuenta()))
            .filter(cuenta -> BCrypt.checkpw(secret, cuenta.contrasenaHash()))
            .map(cuenta -> AuthenticationResponse.success(
                cuenta.correo(),
                List.of(cuenta.rol()),
                Map.of(
                    "id_cuenta", cuenta.idCuenta().toString(),
                    "nombre_completo", cuenta.nombreCompleto(),
                    "rol", cuenta.rol()
                )
            ))
            .orElseGet(() -> AuthenticationResponse.failure("Credenciales incorrectas o cuenta inactiva"));
    }
}
