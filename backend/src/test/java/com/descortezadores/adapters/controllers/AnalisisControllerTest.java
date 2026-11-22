package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.AnalisisDetalleDTO;
import com.descortezadores.domain.entities.Analisis;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.domain.entities.Cuenta;
import com.descortezadores.infrastructure.persistence.AnalisisRepository;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
import com.descortezadores.infrastructure.persistence.CuentaRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@MicronautTest(environments = "test", transactional = false)
class AnalisisControllerTest {

    private static final String CLAVE = "Password123!";

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    CuentaRepository cuentas;

    @Inject
    ArbolRepository arboles;

    @Inject
    AnalisisRepository analisisRepository;

    private UUID dueno;

    @BeforeEach
    void setup() {
        dueno = cuenta("dueno_analisis@test.mx", "Experto de campo");
        cuenta("otro_analisis@test.mx", "Experto de campo");
        cuenta("admin_analisis@test.mx", "Administrador");
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

    private Analisis crearAnalisisDelDueno() {
        Arbol arbol = arboles.save(new Arbol(
            UUID.randomUUID(), "Fresno " + UUID.randomUUID(), "Fraxinus uhdei",
            -99.18, 19.42, Instant.now(), "Descripción general", dueno, null));
        return analisisRepository.save(new Analisis(
            UUID.randomUUID(), "/api/v1/evidencias/test.jpg", 19.42, -99.18,
            Instant.now(), Instant.now(), "sincronizado", "corteza", "infestado",
            0.93, "Nota original", arbol.idArbol(), null, dueno, null));
    }

    @Test
    void duenoYAdminPuedenActualizarObservacionesIndividuales() {
        Analisis analisis = crearAnalisisDelDueno();

        HttpResponse<AnalisisDetalleDTO> respDueno = client.toBlocking().exchange(
            HttpRequest.PUT("/api/v1/analisis/" + analisis.idAnalisis(), Map.of("observaciones", "Resina en cara sur"))
                .bearerAuth(token("dueno_analisis@test.mx")),
            AnalisisDetalleDTO.class
        );
        assertEquals(HttpStatus.OK, respDueno.getStatus());
        assertEquals("Resina en cara sur", respDueno.body().observaciones());

        HttpResponse<AnalisisDetalleDTO> respAdmin = client.toBlocking().exchange(
            HttpRequest.PUT("/api/v1/analisis/" + analisis.idAnalisis(), Map.of("observaciones", ""))
                .bearerAuth(token("admin_analisis@test.mx")),
            AnalisisDetalleDTO.class
        );
        assertEquals(HttpStatus.OK, respAdmin.getStatus());
        assertNull(respAdmin.body().observaciones());
    }

    @Test
    void otroExpertoRecibe403YIdInexistenteRecibe404() {
        Analisis analisis = crearAnalisisDelDueno();

        HttpStatus statusAjeno;
        try {
            statusAjeno = client.toBlocking().exchange(
                HttpRequest.PUT("/api/v1/analisis/" + analisis.idAnalisis(), Map.of("observaciones", "Hack"))
                    .bearerAuth(token("otro_analisis@test.mx"))
            ).getStatus();
        } catch (HttpClientResponseException e) {
            statusAjeno = e.getStatus();
        }
        assertEquals(HttpStatus.FORBIDDEN, statusAjeno);

        HttpStatus statusNotFound;
        try {
            statusNotFound = client.toBlocking().exchange(
                HttpRequest.PUT("/api/v1/analisis/" + UUID.randomUUID(), Map.of("observaciones", "Nada"))
                    .bearerAuth(token("dueno_analisis@test.mx"))
            ).getStatus();
        } catch (HttpClientResponseException e) {
            statusNotFound = e.getStatus();
        }
        assertEquals(HttpStatus.NOT_FOUND, statusNotFound);
    }
}
