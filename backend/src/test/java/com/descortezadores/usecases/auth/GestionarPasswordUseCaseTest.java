package com.descortezadores.usecases.auth;

import com.descortezadores.domain.entities.Cuenta;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.infrastructure.persistence.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Recuperación y Restablecimiento de Contraseñas (CU-G02, CU-G03, RN-04, RN-05, RN-06)")
class GestionarPasswordUseCaseTest {

    private Map<UUID, Cuenta> cuentasDb;
    private CuentaRepository cuentaRepository;
    private GestionarPasswordUseCase useCase;
    private Cuenta cuentaActiva;
    private Cuenta cuentaDesactivada;

    @BeforeEach
    void setUp() {
        cuentasDb = new HashMap<>();

        cuentaActiva = new Cuenta(
            UUID.randomUUID(),
            "experto@chapultepec.gob.mx",
            BCrypt.hashpw("Password123!", BCrypt.gensalt(10)),
            "Experto de campo",
            "Activa",
            Instant.now(),
            "Juan Experto"
        );
        cuentasDb.put(cuentaActiva.idCuenta(), cuentaActiva);

        cuentaDesactivada = new Cuenta(
            UUID.randomUUID(),
            "inactivo@chapultepec.gob.mx",
            BCrypt.hashpw("Password123!", BCrypt.gensalt(10)),
            "Experto de campo",
            "desactivada",
            Instant.now(),
            "Carlos Inactivo"
        );
        cuentasDb.put(cuentaDesactivada.idCuenta(), cuentaDesactivada);

        cuentaRepository = (CuentaRepository) Proxy.newProxyInstance(
            CuentaRepository.class.getClassLoader(),
            new Class<?>[]{CuentaRepository.class},
            (p, m, args) -> {
                if ("findByCorreo".equals(m.getName())) {
                    String correo = (String) args[0];
                    return cuentasDb.values().stream()
                        .filter(c -> c.correo().equalsIgnoreCase(correo))
                        .findFirst();
                }
                if ("findById".equals(m.getName())) {
                    UUID id = (UUID) args[0];
                    return Optional.ofNullable(cuentasDb.get(id));
                }
                if ("update".equals(m.getName())) {
                    Cuenta c = (Cuenta) args[0];
                    cuentasDb.put(c.idCuenta(), c);
                    return c;
                }
                return null;
            }
        );

        useCase = new GestionarPasswordUseCase(cuentaRepository);
    }

    @Test
    @DisplayName("CU-G02: Error MSG2 si el correo electrónico no está registrado")
    void testRecuperacionCorreoNoRegistrado() {
        var ex = assertThrows(IllegalArgumentException.class, () ->
            useCase.solicitarRecuperacion("no_existe@chapultepec.gob.mx")
        );
        assertEquals(MensajesSistema.MSG02.getDescripcion(), ex.getMessage());
    }

    @Test
    @DisplayName("CU-G02: Error MSG4 si la cuenta se encuentra desactivada")
    void testRecuperacionCuentaDesactivada() {
        var ex = assertThrows(IllegalArgumentException.class, () ->
            useCase.solicitarRecuperacion(cuentaDesactivada.correo())
        );
        assertEquals(MensajesSistema.MSG04.getDescripcion(), ex.getMessage());
    }

    @Test
    @DisplayName("CU-G02: Generación exitosa de código de recuperación (RN-04)")
    void testRecuperacionExitosa() {
        String codigo = useCase.solicitarRecuperacion(cuentaActiva.correo());
        assertNotNull(codigo);
        assertEquals(8, codigo.length());

        var tokenOpt = useCase.consultarToken(codigo);
        assertTrue(tokenOpt.isPresent());
        assertTrue(tokenOpt.get().esValido());
        assertFalse(tokenOpt.get().estaExpirado());
    }

    @Test
    @DisplayName("RN-06: Máximo 3 solicitudes de recuperación de contraseña por usuario en 30 días")
    void testLimiteTresSolicitudesRN06() {
        useCase.solicitarRecuperacion(cuentaActiva.correo());
        useCase.solicitarRecuperacion(cuentaActiva.correo());
        useCase.solicitarRecuperacion(cuentaActiva.correo());

        var ex = assertThrows(IllegalStateException.class, () ->
            useCase.solicitarRecuperacion(cuentaActiva.correo())
        );
        assertTrue(ex.getMessage().contains("límite de 3 solicitudes"));
    }

    @Test
    @DisplayName("CU-G03: Validaciones de código inválido (MSG7), no coincidencia (MSG9) e inseguridad (MSG10)")
    void testRestablecerValidaciones() {
        String codigo = useCase.solicitarRecuperacion(cuentaActiva.correo());

        // MSG7: Código inexistente
        var exCodigo = assertThrows(IllegalArgumentException.class, () ->
            useCase.restablecerContrasena("INVALIDO", "NuevaPassword1!", "NuevaPassword1!")
        );
        assertEquals(MensajesSistema.MSG07.getDescripcion(), exCodigo.getMessage());

        // MSG9: Contraseñas no coinciden
        var exMatch = assertThrows(IllegalArgumentException.class, () ->
            useCase.restablecerContrasena(codigo, "NuevaPassword1!", "OtraPassword2?")
        );
        assertEquals(MensajesSistema.MSG09.getDescripcion(), exMatch.getMessage());

        // MSG10: Contraseña insegura (< 8 caracteres o sin número/caracter especial)
        var exInsegura = assertThrows(IllegalArgumentException.class, () ->
            useCase.restablecerContrasena(codigo, "corta", "corta")
        );
        assertEquals(MensajesSistema.MSG10.getDescripcion(), exInsegura.getMessage());
    }

    @Test
    @DisplayName("CU-G03 / RN-05: Restablecimiento exitoso y código invalidado tras uso")
    void testRestablecerExitosoYTokenInvalidado() {
        String codigo = useCase.solicitarRecuperacion(cuentaActiva.correo());
        String nuevaClave = "BosqueVerde2026#";

        useCase.restablecerContrasena(codigo, nuevaClave, nuevaClave);

        Cuenta actualizada = cuentasDb.get(cuentaActiva.idCuenta());
        assertTrue(BCrypt.checkpw(nuevaClave, actualizada.contrasenaHash()));

        // RN-05: Reintentar con el mismo código debe fallar por haber sido usado
        var exReuso = assertThrows(IllegalArgumentException.class, () ->
            useCase.restablecerContrasena(codigo, "OtraClaveMas123!", "OtraClaveMas123!")
        );
        assertEquals(MensajesSistema.MSG07.getDescripcion(), exReuso.getMessage());
    }
}
