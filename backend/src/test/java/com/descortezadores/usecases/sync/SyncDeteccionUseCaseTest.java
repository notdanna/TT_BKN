package com.descortezadores.usecases.sync;

import com.descortezadores.adapters.dto.SyncBatchResponse;
import com.descortezadores.adapters.dto.SyncItemDTO;
import com.descortezadores.domain.entities.*;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Flujo de Sincronización Móvil Offline-First (SyncDeteccionUseCase)")
class SyncDeteccionUseCaseTest {

    @TempDir
    Path tempUploadDir;

    private List<Arbol> arbolesGuardados;
    private List<ZonaFollaje> zonasGuardadas;
    private List<Analisis> analisisGuardados;
    private List<AnalisisCromatico> cromaticosGuardados;
    private List<CuentaDispositivo> relacionesDispositivo;

    private ArbolRepository arbolRepository;
    private ZonaFollajeRepository zonaFollajeRepository;
    private AnalisisRepository analisisRepository;
    private AnalisisCromaticoRepository analisisCromaticoRepository;
    private CuentaDispositivoRepository cuentaDispositivoRepository;

    private SyncDeteccionUseCase useCase;
    private UUID idCuentaTest;

    @BeforeEach
    void setUp() {
        arbolesGuardados = new ArrayList<>();
        zonasGuardadas = new ArrayList<>();
        analisisGuardados = new ArrayList<>();
        cromaticosGuardados = new ArrayList<>();
        relacionesDispositivo = new ArrayList<>();
        idCuentaTest = UUID.randomUUID();

        arbolRepository = (ArbolRepository) Proxy.newProxyInstance(
            ArbolRepository.class.getClassLoader(),
            new Class<?>[]{ArbolRepository.class},
            (p, m, args) -> {
                if ("save".equals(m.getName())) {
                    Arbol a = (Arbol) args[0];
                    arbolesGuardados.add(a);
                    return a;
                }
                if ("findById".equals(m.getName())) {
                    UUID id = (UUID) args[0];
                    return arbolesGuardados.stream().filter(a -> a.idArbol().equals(id)).findFirst();
                }
                return null;
            }
        );

        zonaFollajeRepository = (ZonaFollajeRepository) Proxy.newProxyInstance(
            ZonaFollajeRepository.class.getClassLoader(),
            new Class<?>[]{ZonaFollajeRepository.class},
            (p, m, args) -> {
                if ("save".equals(m.getName())) {
                    ZonaFollaje z = (ZonaFollaje) args[0];
                    zonasGuardadas.add(z);
                    return z;
                }
                if ("findById".equals(m.getName())) {
                    UUID id = (UUID) args[0];
                    return zonasGuardadas.stream().filter(z -> z.idZona().equals(id)).findFirst();
                }
                return null;
            }
        );

        analisisRepository = (AnalisisRepository) Proxy.newProxyInstance(
            AnalisisRepository.class.getClassLoader(),
            new Class<?>[]{AnalisisRepository.class},
            (p, m, args) -> {
                if ("save".equals(m.getName())) {
                    Analisis a = (Analisis) args[0];
                    analisisGuardados.add(a);
                    return a;
                }
                return null;
            }
        );

        analisisCromaticoRepository = (AnalisisCromaticoRepository) Proxy.newProxyInstance(
            AnalisisCromaticoRepository.class.getClassLoader(),
            new Class<?>[]{AnalisisCromaticoRepository.class},
            (p, m, args) -> {
                if ("save".equals(m.getName())) {
                    AnalisisCromatico c = (AnalisisCromatico) args[0];
                    cromaticosGuardados.add(c);
                    return c;
                }
                return null;
            }
        );

        cuentaDispositivoRepository = (CuentaDispositivoRepository) Proxy.newProxyInstance(
            CuentaDispositivoRepository.class.getClassLoader(),
            new Class<?>[]{CuentaDispositivoRepository.class},
            (p, m, args) -> {
                if ("findByIdCuentaAndIdDispositivo".equals(m.getName())) {
                    UUID cuenta = (UUID) args[0];
                    UUID disp = (UUID) args[1];
                    return relacionesDispositivo.stream()
                        .filter(cd -> cd.idCuenta().equals(cuenta) && cd.idDispositivo().equals(disp))
                        .findFirst();
                }
                return null;
            }
        );

        useCase = new SyncDeteccionUseCase(
            arbolRepository,
            zonaFollajeRepository,
            analisisRepository,
            analisisCromaticoRepository,
            cuentaDispositivoRepository,
            tempUploadDir.toString()
        );
    }

    @Test
    @DisplayName("Sincronización exitosa de captura de corteza con árbol existente")
    void testSyncCortezaArbolExistente() {
        UUID idArbol = UUID.randomUUID();
        UUID idAnalisisLocal = UUID.randomUUID();

        SyncItemDTO item = new SyncItemDTO(
            idAnalisisLocal,
            idArbol,
            null,
            null,
            19.421,
            -99.182,
            Instant.now(),
            "corteza",
            "sano",
            0.92,
            "Corteza sana sin galerías",
            0.05, 0.05, 0.10, 0.80,
            null,
            null,
            null, null, null
        );

        SyncBatchResponse resp = useCase.sincronizar(List.of(item), idCuentaTest);

        assertEquals(1, resp.totalRecibidos());
        assertEquals(1, resp.totalSincronizados());
        assertTrue(resp.errores().isEmpty());
        assertEquals(1, analisisGuardados.size());

        Analisis guardado = analisisGuardados.get(0);
        assertEquals(idArbol, guardado.idArbol());
        assertNull(guardado.idZona(), "idZona debe ser null para corteza (chk_arbol_o_zona)");
        assertEquals("sano", guardado.resultadoClasificacion());
    }

    @Test
    @DisplayName("RN-37 / RN-38: Sincronización crea árbol nuevo si no existe y viene nombre")
    void testSyncCortezaCreaArbolNuevo() {
        UUID idAnalisisLocal = UUID.randomUUID();

        SyncItemDTO item = new SyncItemDTO(
            idAnalisisLocal,
            null,
            "Fresno-Campo-01",
            "Fraxinus uhdei",
            19.4205,
            -99.1815,
            Instant.now(),
            "corteza",
            "infestado",
            0.88,
            "Resina fresca y aserrín",
            0.05, 0.70, 0.15, 0.10,
            null,
            null,
            null, null, null
        );

        SyncBatchResponse resp = useCase.sincronizar(List.of(item), idCuentaTest);

        assertEquals(1, resp.totalSincronizados());
        assertEquals(1, arbolesGuardados.size(), "Debe crearse 1 árbol nuevo");
        assertEquals("Fresno-Campo-01", arbolesGuardados.get(0).nombreEspecifico());
        assertEquals(arbolesGuardados.get(0).idArbol(), analisisGuardados.get(0).idArbol());
    }

    @Test
    @DisplayName("RN-37: Captura de corteza sin árbol ni nombre de árbol es rechazada")
    void testSyncCortezaSinArbolFalla() {
        UUID idAnalisisLocal = UUID.randomUUID();

        SyncItemDTO item = new SyncItemDTO(
            idAnalisisLocal,
            null,
            null, // Sin árbol ni nombre
            null,
            19.42,
            -99.18,
            Instant.now(),
            "corteza",
            "sano",
            0.90,
            null, null, null, null, null, null, null, null, null, null
        );

        SyncBatchResponse resp = useCase.sincronizar(List.of(item), idCuentaTest);

        assertEquals(0, resp.totalSincronizados());
        assertEquals(1, resp.errores().size());
        assertTrue(resp.errores().get(0).contains("RN-37"));
    }

    @Test
    @DisplayName("Sincronización exitosa de Follaje: Asocia a Zona y respeta chk_arbol_o_zona")
    void testSyncFollajeCreaZona() {
        UUID idAnalisisLocal = UUID.randomUUID();

        SyncItemDTO item = new SyncItemDTO(
            idAnalisisLocal,
            null,
            null,
            null,
            19.425,
            -99.185,
            Instant.now(),
            "follaje",
            "sano",
            0.85,
            "Captura aérea con dron de Sección I",
            0.10, 0.20, 0.30, 0.40,
            null,
            null,
            null,
            "Zona-Follaje-Norte",
            75.0
        );

        SyncBatchResponse resp = useCase.sincronizar(List.of(item), idCuentaTest);

        assertEquals(1, resp.totalSincronizados());
        assertEquals(1, zonasGuardadas.size(), "Debe crearse la ZonaFollaje");
        assertEquals("Zona-Follaje-Norte", zonasGuardadas.get(0).nombreZona());

        Analisis guardado = analisisGuardados.get(0);
        assertNull(guardado.idArbol(), "idArbol debe ser null para follaje (chk_arbol_o_zona)");
        assertNotNull(guardado.idZona(), "idZona debe estar presente para follaje");
        assertEquals(zonasGuardadas.get(0).idZona(), guardado.idZona());
    }

    @Test
    @DisplayName("RN-24: Confianza < 75% marca resultado como no_determinado")
    void testConfianzaMenorA75MarcaNoDeterminado() {
        UUID idArbol = UUID.randomUUID();

        SyncItemDTO item = new SyncItemDTO(
            UUID.randomUUID(),
            idArbol,
            null, null,
            19.42, -99.18,
            Instant.now(),
            "corteza",
            "infestado",
            0.65, // < 0.75
            null, null, null, null, null, null, null, null, null, null
        );

        useCase.sincronizar(List.of(item), idCuentaTest);

        assertEquals(1, analisisGuardados.size());
        assertEquals("no_determinado", analisisGuardados.get(0).resultadoClasificacion(),
            "RN-24 exige marcar 'no_determinado' si la confianza es < 0.75");
    }

    @Test
    @DisplayName("Decodificación segura de Base64 con prefijo Data URI")
    void testBase64ConPrefijoDataURI() throws Exception {
        UUID idArbol = UUID.randomUUID();

        // Generar una imagen válida de 300x300 px
        BufferedImage img = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        String rawB64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        String b64ConDataURI = "data:image/jpeg;base64," + rawB64;

        SyncItemDTO item = new SyncItemDTO(
            UUID.randomUUID(),
            idArbol,
            null, null,
            19.42, -99.18,
            Instant.now(),
            "corteza",
            "sano",
            0.85,
            null, null, null, null, null,
            b64ConDataURI, // Con Data URI
            null, null, null, null
        );

        SyncBatchResponse resp = useCase.sincronizar(List.of(item), idCuentaTest);

        assertEquals(1, resp.totalSincronizados());
        assertTrue(resp.errores().isEmpty());
        assertFalse(analisisGuardados.get(0).rutaEvidencia().contains("sin_imagen"));
    }

    @Test
    @DisplayName("RN-02 / MSG5: Dispositivo desasociado o bloqueado es rechazado")
    void testDispositivoDesasociadoFalla() {
        UUID idDispositivo = UUID.randomUUID();
        UUID idArbol = UUID.randomUUID();

        // Registrar relación desasociada
        relacionesDispositivo.add(new CuentaDispositivo(
            UUID.randomUUID(),
            idCuentaTest,
            idDispositivo,
            Instant.now(),
            Instant.now(),
            "desasociada"
        ));

        SyncItemDTO item = new SyncItemDTO(
            UUID.randomUUID(),
            idArbol,
            null, null,
            19.42, -99.18,
            Instant.now(),
            "corteza",
            "sano",
            0.90,
            null, null, null, null, null, null,
            idDispositivo,
            null, null, null
        );

        SyncBatchResponse resp = useCase.sincronizar(List.of(item), idCuentaTest);

        assertEquals(0, resp.totalSincronizados());
        assertEquals(1, resp.errores().size());
        assertTrue(resp.errores().get(0).contains(MensajesSistema.MSG05.getCodigo()));
    }
}
