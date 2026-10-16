package com.descortezadores.usecases.admin;

import com.descortezadores.adapters.dto.UsuarioDTO;
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

@DisplayName("Pruebas Unitarias - Gestión de Cuentas (CU-A01 a CU-A05)")
class GestionarCuentasUseCaseTest {

    private List<Cuenta> cuentas;
    private CuentaRepository cuentaRepository;
    private GestionarCuentasUseCase useCase;

    @BeforeEach
    void setUp() {
        cuentas = new ArrayList<>();

        cuentaRepository = (CuentaRepository) Proxy.newProxyInstance(
            CuentaRepository.class.getClassLoader(),
            new Class<?>[]{CuentaRepository.class},
            (p, m, args) -> {
                String name = m.getName();
                if ("findAll".equals(name)) {
                    return new ArrayList<>(cuentas);
                } else if ("findById".equals(name)) {
                    UUID id = (UUID) args[0];
                    return cuentas.stream().filter(c -> c.idCuenta().equals(id)).findFirst();
                } else if ("findByCorreo".equals(name)) {
                    String correo = (String) args[0];
                    return cuentas.stream().filter(c -> c.correo().equalsIgnoreCase(correo)).findFirst();
                } else if ("save".equals(name)) {
                    Cuenta c = (Cuenta) args[0];
                    cuentas.add(c);
                    return c;
                } else if ("update".equals(name)) {
                    Cuenta c = (Cuenta) args[0];
                    cuentas.removeIf(existing -> existing.idCuenta().equals(c.idCuenta()));
                    cuentas.add(c);
                    return c;
                }
                return null;
            }
        );

        useCase = new GestionarCuentasUseCase(cuentaRepository);
    }

    @Test
    @DisplayName("CU-A02: Crear cuenta exitosa con contraseña hasheada en BCrypt")
    void testCrearCuentaExitosa() {
        UsuarioDTO dto = new UsuarioDTO(
            null,
            "tecnico@chapultepec.gob.mx",
            "MiClaveSegura123!",
            "Experto de campo",
            "Activa",
            null,
            "Juan Perez"
        );

        Cuenta creada = useCase.crearCuenta(dto);

        assertNotNull(creada.idCuenta());
        assertEquals("tecnico@chapultepec.gob.mx", creada.correo());
        assertEquals("Experto de campo", creada.rol());
        assertTrue(BCrypt.checkpw("MiClaveSegura123!", creada.contrasenaHash()), "La contraseña debe guardarse hasheada con BCrypt");
    }

    @Test
    @DisplayName("CU-A02: Correo duplicado es rechazado con mensaje MSG31")
    void testCrearCuentaCorreoDuplicadoFalla() {
        Cuenta existente = new Cuenta(
            UUID.randomUUID(),
            "admin@chapultepec.gob.mx",
            "hash",
            "Administrador",
            "Activa",
            Instant.now(),
            "Admin"
        );
        cuentas.add(existente);

        UsuarioDTO dto = new UsuarioDTO(
            null,
            "admin@chapultepec.gob.mx",
            "Password123!",
            "Experto de campo",
            "Activa",
            null,
            "Otro Admin"
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> useCase.crearCuenta(dto));
        assertTrue(ex.getMessage().contains(MensajesSistema.MSG31.getCodigo()), "Debe rechazar con código MSG31");
    }

    @Test
    @DisplayName("CU-A04: Desactivación de cuenta cambia estado a 'desactivada'")
    void testDesactivarCuenta() {
        UUID id = UUID.randomUUID();
        Cuenta activa = new Cuenta(
            id,
            "experto@chapultepec.gob.mx",
            "hash",
            "Experto de campo",
            "Activa",
            Instant.now(),
            "Experto"
        );
        cuentas.add(activa);

        Cuenta desactivada = useCase.cambiarEstado(id, "desactivada");

        assertEquals("desactivada", desactivada.estadoCuenta());
    }
}
