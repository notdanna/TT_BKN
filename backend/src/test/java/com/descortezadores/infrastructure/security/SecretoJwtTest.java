package com.descortezadores.infrastructure.security;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.context.exceptions.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El secreto que firma los JWT no puede venir en el repositorio: quien lo lea puede fabricar un token
 * de Administrador. Antes application.yml traía un valor por defecto que se usaba en producción.
 */
class SecretoJwtTest {

    private static final String PROPIEDAD = "micronaut.security.token.jwt.signatures.secret.generator.secret";

    /** Valor que estuvo publicado en application.yml (historial de git): queda vetado para siempre. */
    private static final String FILTRADO = "9f8a8b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a";

    @Test
    void laConfiguracionBaseNoTraeUnSecretoPorDefecto() {
        // Sin el entorno "test" (deduceEnvironment(false) evita que Micronaut detecte JUnit).
        try (ApplicationContext ctx = ApplicationContext.builder().deduceEnvironment(false).build()) {
            Environment env = ctx.getEnvironment().start();
            assertThrows(ConfigurationException.class, () -> env.getProperty(PROPIEDAD, String.class),
                    "sin JWT_GENERATOR_SIGNATURE_SECRET no debe haber secreto: el arranque tiene que fallar");
        }
    }

    @Test
    void laVariableDeEntornoSiSeUsa() {
        String secreto = "x".repeat(48);
        try (ApplicationContext ctx = ApplicationContext.builder().deduceEnvironment(false)
                .properties(Map.of("JWT_GENERATOR_SIGNATURE_SECRET", secreto)).build()) {
            Environment env = ctx.getEnvironment().start();
            assertEquals(secreto, env.getProperty(PROPIEDAD, String.class).orElseThrow());
        }
    }

    @Test
    void rechazaSecretosCortosOElFiltrado() {
        assertThrows(IllegalStateException.class, () -> ValidadorSecretoJwt.validar("corto"));
        assertThrows(IllegalStateException.class, () -> ValidadorSecretoJwt.validar(" ".repeat(40)));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ValidadorSecretoJwt.validar(FILTRADO));
        assertTrue(e.getMessage().contains("publicado"), e.getMessage());
        assertDoesNotThrow(() -> ValidadorSecretoJwt.validar("una-frase-larga-y-aleatoria-de-al-menos-32-caracteres"));
    }
}
