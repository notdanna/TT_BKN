package com.descortezadores.adapters.controllers;

import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.domain.entities.Cuenta;
import com.descortezadores.domain.entities.ZonaFollaje;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
import com.descortezadores.infrastructure.persistence.CuentaRepository;
import com.descortezadores.infrastructure.persistence.ZonaFollajeRepository;
import io.micronaut.http.HttpRequest;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quién puede borrar árboles y zonas. Antes cualquier cuenta autenticada podía borrar registros
 * ajenos y, con DELETE sin id, vaciar todo el catálogo (la app móvil lo llama al limpiar su caché).
 * Regla: el borrado masivo es solo de Administrador; el individual, del dueño o de un Administrador.
 */
// transactional = false: con la transacción de prueba, lo que guarda el test no lo vería el servidor
// (otra conexión) y un DELETE "exitoso" no borraría nada.
@MicronautTest(environments = "test", transactional = false)
class AutorizacionBorradoTest {

    private static final String CLAVE = "Password123!";

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    CuentaRepository cuentas;

    @Inject
    ArbolRepository arboles;

    @Inject
    ZonaFollajeRepository zonas;

    private UUID dueno;

    @BeforeEach
    void setup() {
        dueno = cuenta("dueno_borrado@test.mx", "Experto de campo");
        cuenta("otro_borrado@test.mx", "Experto de campo");
        cuenta("admin_borrado@test.mx", "Administrador");
    }

    private UUID cuenta(String correo, String rol) {
        return cuentas.findByCorreo(correo).map(Cuenta::idCuenta).orElseGet(() -> cuentas.save(new Cuenta(
                UUID.randomUUID(), correo, BCrypt.hashpw(CLAVE, BCrypt.gensalt(4)), rol, "Activa",
                Instant.now(), correo)).idCuenta());
    }

    private String token(String correo) {
        Map<?, ?> body = client.toBlocking().retrieve(
                HttpRequest.POST("/login", Map.of("username", correo, "password", CLAVE)), Map.class);
        return (String) body.get("access_token");
    }

    private Arbol arbolDelDueno() {
        return arboles.save(new Arbol(UUID.randomUUID(), "Oyamel " + UUID.randomUUID(), "Abies religiosa",
                -99.18, 19.42, Instant.now(), null, dueno, null));
    }

    private ZonaFollaje zonaDelDueno() {
        return zonas.save(new ZonaFollaje(UUID.randomUUID(), 19.42, -99.18, Instant.now(), null,
                "Zona " + UUID.randomUUID(), 50.0, null, dueno));
    }

    private HttpStatus borrar(String ruta, String correo) {
        try {
            return client.toBlocking().exchange(HttpRequest.DELETE(ruta).bearerAuth(token(correo))).getStatus();
        } catch (HttpClientResponseException e) {
            return e.getStatus();
        }
    }

    // ---- borrado masivo ----------------------------------------------------------------------------

    @Test
    void unExpertoNoPuedeVaciarElCatalogoDeArboles() {
        Arbol a = arbolDelDueno();
        assertEquals(HttpStatus.FORBIDDEN, borrar("/api/v1/arboles", "dueno_borrado@test.mx"));
        assertTrue(arboles.findById(a.idArbol()).isPresent(), "el árbol no debe borrarse");
    }

    @Test
    void unExpertoNoPuedeVaciarElCatalogoDeZonas() {
        ZonaFollaje z = zonaDelDueno();
        assertEquals(HttpStatus.FORBIDDEN, borrar("/api/v1/zonas-follaje", "dueno_borrado@test.mx"));
        assertTrue(zonas.findById(z.idZona()).isPresent(), "la zona no debe borrarse");
    }

    @Test
    void unAdministradorSiPuedeVaciarlo() {
        arbolDelDueno();
        zonaDelDueno();
        assertEquals(HttpStatus.NO_CONTENT, borrar("/api/v1/arboles", "admin_borrado@test.mx"));
        assertEquals(HttpStatus.NO_CONTENT, borrar("/api/v1/zonas-follaje", "admin_borrado@test.mx"));
        assertEquals(0, arboles.count());
        assertEquals(0, zonas.count());
    }

    // ---- borrado individual ------------------------------------------------------------------------

    @Test
    void otroExpertoNoPuedeBorrarUnArbolAjeno() {
        Arbol a = arbolDelDueno();
        assertEquals(HttpStatus.FORBIDDEN, borrar("/api/v1/arboles/" + a.idArbol(), "otro_borrado@test.mx"));
        assertTrue(arboles.findById(a.idArbol()).isPresent());
    }

    @Test
    void otroExpertoNoPuedeBorrarUnaZonaAjena() {
        ZonaFollaje z = zonaDelDueno();
        assertEquals(HttpStatus.FORBIDDEN, borrar("/api/v1/zonas-follaje/" + z.idZona(), "otro_borrado@test.mx"));
        assertTrue(zonas.findById(z.idZona()).isPresent());
    }

    @Test
    void elDuenoBorraLoSuyo() {
        Arbol a = arbolDelDueno();
        ZonaFollaje z = zonaDelDueno();
        assertEquals(HttpStatus.NO_CONTENT, borrar("/api/v1/arboles/" + a.idArbol(), "dueno_borrado@test.mx"));
        assertEquals(HttpStatus.NO_CONTENT, borrar("/api/v1/zonas-follaje/" + z.idZona(), "dueno_borrado@test.mx"));
        assertTrue(arboles.findById(a.idArbol()).isEmpty());
        assertTrue(zonas.findById(z.idZona()).isEmpty());
    }

    @Test
    void unAdministradorBorraRegistrosAjenos() {
        Arbol a = arbolDelDueno();
        assertEquals(HttpStatus.NO_CONTENT, borrar("/api/v1/arboles/" + a.idArbol(), "admin_borrado@test.mx"));
        assertTrue(arboles.findById(a.idArbol()).isEmpty());
    }

    @Test
    void borrarAlgoQueNoExisteEs404() {
        assertEquals(HttpStatus.NOT_FOUND, borrar("/api/v1/arboles/" + UUID.randomUUID(), "dueno_borrado@test.mx"));
        assertEquals(HttpStatus.NOT_FOUND, borrar("/api/v1/zonas-follaje/" + UUID.randomUUID(), "dueno_borrado@test.mx"));
    }

    @Test
    void sinTokenEs401() {
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.DELETE("/api/v1/arboles")));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }
}
