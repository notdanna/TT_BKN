package com.descortezadores.adapters.controllers;

import com.descortezadores.domain.entities.Cuenta;
import com.descortezadores.domain.entities.CuentaDispositivo;
import com.descortezadores.domain.entities.Dispositivo;
import com.descortezadores.infrastructure.persistence.CuentaDispositivoRepository;
import com.descortezadores.infrastructure.persistence.CuentaRepository;
import com.descortezadores.infrastructure.persistence.DispositivoRepository;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(environments = "test")
public class CustomLoginTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    CuentaRepository cuentaRepository;

    @Inject
    DispositivoRepository dispositivoRepository;

    @Inject
    CuentaDispositivoRepository cuentaDispositivoRepository;

    private UUID cuentaId;

    @BeforeEach
    void setup() {
        Optional<Cuenta> opt = cuentaRepository.findByCorreo("experto_test@chapultepec.gob.mx");
        if (opt.isEmpty()) {
            Cuenta c = cuentaRepository.save(new Cuenta(
                    UUID.randomUUID(),
                    "experto_test@chapultepec.gob.mx",
                    BCrypt.hashpw("Password123!", BCrypt.gensalt(10)),
                    "Experto de campo",
                    "Activa",
                    Instant.now(),
                    "Biólogo de Campo Test"
            ));
            cuentaId = c.idCuenta();
        } else {
            cuentaId = opt.get().idCuenta();
        }
    }

    @Test
    void testLoginConCredencialesInvalidas() {
        HttpRequest<Map<String, Object>> request = HttpRequest.POST("/login", Map.of(
                "username", "experto_test@chapultepec.gob.mx",
                "password", "WrongPassword!"
        ));

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request, Map.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void testLoginWebSinDispositivo() {
        HttpRequest<Map<String, Object>> request = HttpRequest.POST("/login", Map.of(
                "username", "experto_test@chapultepec.gob.mx",
                "password", "Password123!"
        ));

        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body().get("access_token"));
        assertEquals("Bearer", response.body().get("token_type"));
    }

    @Test
    void testLoginMovilConAutoRegistroDeDispositivo() {
        UUID nuevoDeviceId = UUID.randomUUID();

        // 1. Antes del login, el dispositivo NO existe en la base de datos
        assertTrue(dispositivoRepository.findById(nuevoDeviceId).isEmpty());

        // 2. Realizar login enviando correo, password, idDispositivo y nombreDispositivo (Pixel7pro)
        HttpRequest<Map<String, Object>> request = HttpRequest.POST("/login", Map.of(
                "username", "experto_test@chapultepec.gob.mx",
                "password", "Password123!",
                "idDispositivo", nuevoDeviceId.toString(),
                "nombreDispositivo", "Pixel7pro"
        ));

        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body().get("access_token"));

        // 3. Ya confirmado el login, verificar que el dispositivo se agregó automáticamente
        Optional<Dispositivo> dispOpt = dispositivoRepository.findById(nuevoDeviceId);
        assertTrue(dispOpt.isPresent(), "El dispositivo debe haberse creado automáticamente en la tabla DISPOSITIVO");
        assertEquals("Pixel7pro", dispOpt.get().nombreDispositivo());
        assertEquals("Activo", dispOpt.get().estadoDispositivo());

        // 4. Verificar que se enlazó a la cuenta en CUENTA_DISPOSITIVO
        Optional<CuentaDispositivo> relOpt = cuentaDispositivoRepository.findByIdCuentaAndIdDispositivo(cuentaId, nuevoDeviceId);
        assertTrue(relOpt.isPresent(), "El dispositivo debe estar enlazado a la cuenta en CUENTA_DISPOSITIVO");
        assertEquals("Activa", relOpt.get().estadoCuentaDispositivo());

        // 5. Segundo login con el mismo dispositivo (debe fluir normalmente sin error)
        HttpRequest<Map<String, Object>> request2 = HttpRequest.POST("/api/v1/auth/login", Map.of(
                "correo", "experto_test@chapultepec.gob.mx",
                "contrasena", "Password123!",
                "idDispositivo", nuevoDeviceId.toString(),
                "nombreDispositivo", "Pixel7pro"
        ));

        HttpResponse<Map> response2 = client.toBlocking().exchange(request2, Map.class);
        assertEquals(HttpStatus.OK, response2.getStatus());
        assertNotNull(response2.body().get("access_token"));
    }
}
