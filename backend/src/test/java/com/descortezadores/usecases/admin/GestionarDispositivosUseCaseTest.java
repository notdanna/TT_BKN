package com.descortezadores.usecases.admin;

import com.descortezadores.adapters.dto.DispositivoDTO;
import com.descortezadores.domain.entities.CuentaDispositivo;
import com.descortezadores.domain.entities.Dispositivo;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.infrastructure.persistence.CuentaDispositivoRepository;
import com.descortezadores.infrastructure.persistence.DispositivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Gestión de Dispositivos (CU-A06 a CU-A09)")
class GestionarDispositivosUseCaseTest {

    private List<Dispositivo> dispositivos;
    private List<CuentaDispositivo> relaciones;

    private DispositivoRepository dispositivoRepository;
    private CuentaDispositivoRepository cuentaDispositivoRepository;
    private GestionarDispositivosUseCase useCase;

    @BeforeEach
    void setUp() {
        dispositivos = new ArrayList<>();
        relaciones = new ArrayList<>();

        dispositivoRepository = (DispositivoRepository) Proxy.newProxyInstance(
            DispositivoRepository.class.getClassLoader(),
            new Class<?>[]{DispositivoRepository.class},
            (p, m, args) -> {
                String name = m.getName();
                if ("findAll".equals(name)) {
                    return new ArrayList<>(dispositivos);
                } else if ("findByIdentificadorUnico".equals(name)) {
                    String uid = (String) args[0];
                    return dispositivos.stream().filter(d -> d.identificadorUnico().equals(uid)).findFirst();
                } else if ("save".equals(name)) {
                    Dispositivo d = (Dispositivo) args[0];
                    dispositivos.add(d);
                    return d;
                }
                return null;
            }
        );

        cuentaDispositivoRepository = (CuentaDispositivoRepository) Proxy.newProxyInstance(
            CuentaDispositivoRepository.class.getClassLoader(),
            new Class<?>[]{CuentaDispositivoRepository.class},
            (p, m, args) -> {
                String name = m.getName();
                if ("findByIdDispositivoAndEstadoCuentaDispositivo".equals(name)) {
                    UUID idDisp = (UUID) args[0];
                    String estado = (String) args[1];
                    return relaciones.stream()
                        .filter(cd -> cd.idDispositivo().equals(idDisp) && cd.estadoCuentaDispositivo().equalsIgnoreCase(estado))
                        .toList();
                } else if ("findByIdCuentaAndIdDispositivo".equals(name)) {
                    UUID idCuenta = (UUID) args[0];
                    UUID idDisp = (UUID) args[1];
                    return relaciones.stream()
                        .filter(cd -> cd.idCuenta().equals(idCuenta) && cd.idDispositivo().equals(idDisp))
                        .findFirst();
                } else if ("findByIdCuenta".equals(name)) {
                    UUID idCuenta = (UUID) args[0];
                    return relaciones.stream()
                        .filter(cd -> cd.idCuenta().equals(idCuenta))
                        .toList();
                } else if ("save".equals(name)) {
                    CuentaDispositivo cd = (CuentaDispositivo) args[0];
                    relaciones.add(cd);
                    return cd;
                } else if ("update".equals(name)) {
                    CuentaDispositivo cd = (CuentaDispositivo) args[0];
                    relaciones.removeIf(r -> r.idCuentaDispositivo().equals(cd.idCuentaDispositivo()));
                    relaciones.add(cd);
                    return cd;
                }
                return null;
            }
        );

        useCase = new GestionarDispositivosUseCase(dispositivoRepository, cuentaDispositivoRepository);
    }

    @Test
    @DisplayName("CU-A06: Registro de nuevo dispositivo móvil")
    void testRegistrarDispositivo() {
        DispositivoDTO dto = new DispositivoDTO(
            null,
            "IMEI-1234567890",
            "Pixel 7 Pro",
            "token_push",
            "Activo",
            null
        );

        Dispositivo d = useCase.registrarDispositivo(dto);

        assertNotNull(d.idDispositivo());
        assertEquals("IMEI-1234567890", d.identificadorUnico());
        assertEquals("Pixel 7 Pro", d.nombreDispositivo());
    }

    @Test
    @DisplayName("CU-A07: Asociar dispositivo a una cuenta exitosamente")
    void testAsociarDispositivoExitosa() {
        UUID idCuenta = UUID.randomUUID();
        UUID idDispositivo = UUID.randomUUID();

        CuentaDispositivo cd = useCase.asociarDispositivoACuenta(idCuenta, idDispositivo);

        assertNotNull(cd.idCuentaDispositivo());
        assertEquals(idCuenta, cd.idCuenta());
        assertEquals(idDispositivo, cd.idDispositivo());
        assertEquals("Activa", cd.estadoCuentaDispositivo());
    }

    @Test
    @DisplayName("CU-A07: Dispositivo ya asociado a otra cuenta activa lanza MSG33")
    void testAsociarDispositivoDuplicadoFalla() {
        UUID cuentaA = UUID.randomUUID();
        UUID cuentaB = UUID.randomUUID();
        UUID idDispositivo = UUID.randomUUID();

        // Asociar a cuenta A
        useCase.asociarDispositivoACuenta(cuentaA, idDispositivo);

        // Intentar asociar el mismo dispositivo a cuenta B mientras sigue activo
        var ex = assertThrows(IllegalStateException.class, () -> useCase.asociarDispositivoACuenta(cuentaB, idDispositivo));
        assertTrue(ex.getMessage().contains(MensajesSistema.MSG33.getCodigo()), "Debe rechazar con código MSG33");
    }

    @Test
    @DisplayName("CU-A08: Desasociar dispositivo cambia estado a 'desasociada'")
    void testDesasociarDispositivo() {
        UUID idCuenta = UUID.randomUUID();
        UUID idDispositivo = UUID.randomUUID();

        useCase.asociarDispositivoACuenta(idCuenta, idDispositivo);
        useCase.desasociarDispositivo(idCuenta, idDispositivo);

        List<CuentaDispositivo> lista = useCase.listarDispositivosPorCuenta(idCuenta);
        assertEquals(1, lista.size());
        assertEquals("desasociada", lista.get(0).estadoCuentaDispositivo());
    }
}
