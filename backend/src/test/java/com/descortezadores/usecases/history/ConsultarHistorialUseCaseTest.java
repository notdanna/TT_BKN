package com.descortezadores.usecases.history;

import com.descortezadores.adapters.dto.AnalisisDetalleDTO;
import com.descortezadores.adapters.dto.ArbolDTO;
import com.descortezadores.adapters.dto.ZonaFollajeDTO;
import com.descortezadores.domain.entities.*;
import com.descortezadores.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Consultas de Historial y Actualización de Árboles (RF-10, RF-20, RF-23)")
class ConsultarHistorialUseCaseTest {

    private List<Arbol> arboles;
    private List<Analisis> analisisList;
    private List<AnalisisCromatico> cromaticos;
    private List<ZonaFollaje> zonas;

    private ArbolRepository arbolRepository;
    private AnalisisRepository analisisRepository;
    private AnalisisCromaticoRepository analisisCromaticoRepository;
    private ZonaFollajeRepository zonaFollajeRepository;

    private ConsultarHistorialUseCase useCase;

    @BeforeEach
    void setUp() {
        arboles = new ArrayList<>();
        analisisList = new ArrayList<>();
        cromaticos = new ArrayList<>();
        zonas = new ArrayList<>();

        arbolRepository = (ArbolRepository) Proxy.newProxyInstance(
            ArbolRepository.class.getClassLoader(),
            new Class<?>[]{ArbolRepository.class},
            (p, m, args) -> {
                String name = m.getName();
                if ("findAllOrderByFechaRegistroDesc".equals(name)) return new ArrayList<>(arboles);
                if ("findById".equals(name)) {
                    UUID id = (UUID) args[0];
                    return arboles.stream().filter(a -> a.idArbol().equals(id)).findFirst();
                }
                if ("update".equals(name)) {
                    Arbol a = (Arbol) args[0];
                    arboles.removeIf(existing -> existing.idArbol().equals(a.idArbol()));
                    arboles.add(a);
                    return a;
                }
                if ("deleteById".equals(name)) {
                    UUID id = (UUID) args[0];
                    arboles.removeIf(existing -> existing.idArbol().equals(id));
                    return null;
                }
                if ("deleteAll".equals(name)) {
                    arboles.clear();
                    return null;
                }
                return null;
            }
        );

        analisisRepository = (AnalisisRepository) Proxy.newProxyInstance(
            AnalisisRepository.class.getClassLoader(),
            new Class<?>[]{AnalisisRepository.class},
            (p, m, args) -> {
                String name = m.getName();
                if ("findByIdArbol".equals(name)) {
                    UUID id = (UUID) args[0];
                    return analisisList.stream().filter(a -> id.equals(a.idArbol())).toList();
                }
                if ("findByIdZona".equals(name)) {
                    UUID id = (UUID) args[0];
                    return analisisList.stream().filter(a -> id.equals(a.idZona())).toList();
                }
                if ("findAll".equals(name) || "findAllOrderByFechaCapturaDesc".equals(name)) return new ArrayList<>(analisisList);
                if ("findById".equals(name)) {
                    UUID id = (UUID) args[0];
                    return analisisList.stream().filter(a -> a.idAnalisis().equals(id)).findFirst();
                }
                if ("delete".equals(name)) {
                    Analisis a = (Analisis) args[0];
                    analisisList.removeIf(existing -> existing.idAnalisis().equals(a.idAnalisis()));
                    return null;
                }
                if ("deleteAll".equals(name)) {
                    analisisList.clear();
                    return null;
                }
                return null;
            }
        );

        analisisCromaticoRepository = (AnalisisCromaticoRepository) Proxy.newProxyInstance(
            AnalisisCromaticoRepository.class.getClassLoader(),
            new Class<?>[]{AnalisisCromaticoRepository.class},
            (p, m, args) -> {
                String name = m.getName();
                if ("findByIdAnalisis".equals(name)) {
                    UUID id = (UUID) args[0];
                    return cromaticos.stream().filter(c -> c.idAnalisis().equals(id)).findFirst();
                }
                if ("deleteById".equals(name)) {
                    UUID id = (UUID) args[0];
                    cromaticos.removeIf(c -> c.idAnalisisCromatico().equals(id));
                    return null;
                }
                if ("deleteAll".equals(name)) {
                    cromaticos.clear();
                    return null;
                }
                return null;
            }
        );

        zonaFollajeRepository = (ZonaFollajeRepository) Proxy.newProxyInstance(
            ZonaFollajeRepository.class.getClassLoader(),
            new Class<?>[]{ZonaFollajeRepository.class},
            (p, m, args) -> {
                String name = m.getName();
                if ("findAllOrderByFechaRegistroDesc".equals(name)) return new ArrayList<>(zonas);
                if ("findById".equals(name)) {
                    UUID id = (UUID) args[0];
                    return zonas.stream().filter(z -> z.idZona().equals(id)).findFirst();
                }
                if ("save".equals(name)) {
                    ZonaFollaje z = (ZonaFollaje) args[0];
                    zonas.add(z);
                    return z;
                }
                if ("update".equals(name)) {
                    ZonaFollaje z = (ZonaFollaje) args[0];
                    zonas.removeIf(old -> old.idZona().equals(z.idZona()));
                    zonas.add(z);
                    return z;
                }
                if ("deleteById".equals(name)) {
                    UUID id = (UUID) args[0];
                    zonas.removeIf(z -> z.idZona().equals(id));
                    return null;
                }
                if ("deleteAll".equals(name)) {
                    zonas.clear();
                    return null;
                }
                return null;
            }
        );

        useCase = new ConsultarHistorialUseCase(
            arbolRepository,
            analisisRepository,
            analisisCromaticoRepository,
            zonaFollajeRepository
        );
    }

    @Test
    @DisplayName("RF-23 / RN-41: Actualización exitosa de observaciones y datos de un árbol")
    void testActualizarArbol() {
        UUID idArbol = UUID.randomUUID();
        UUID idCuenta = UUID.randomUUID();
        Arbol original = new Arbol(
            idArbol,
            "Fresno-Antiguo",
            "Fraxinus uhdei",
            -99.182, 19.421,
            Instant.now(),
            "Sin observaciones",
            idCuenta,
            null
        );
        arboles.add(original);

        ArbolDTO actualizacion = new ArbolDTO(
            idArbol,
            "Fresno-Actualizado-01",
            "Fraxinus uhdei",
            19.4215, -99.1825,
            Instant.now(),
            "Se detectó resina activa en fuste",
            idCuenta,
            null
        );

        Arbol resultado = useCase.actualizarArbol(idArbol, actualizacion);

        assertEquals("Fresno-Actualizado-01", resultado.nombreEspecifico());
        assertEquals("Se detectó resina activa en fuste", resultado.descripcion());
    }

    @Test
    @DisplayName("RF-10 / RF-20: Consulta de historial de capturas por árbol con análisis cromático")
    void testHistorialPorArbolConCromatico() {
        UUID idArbol = UUID.randomUUID();
        UUID idAnalisis = UUID.randomUUID();

        Analisis an = new Analisis(
            idAnalisis, "/img.jpg", 19.42, -99.18, Instant.now(), Instant.now(),
            "sincronizado", "corteza", "infestado", 0.91, "Daño visible",
            idArbol, null
        );
        analisisList.add(an);

        AnalisisCromatico cr = new AnalisisCromatico(
            UUID.randomUUID(), 0.05, 0.65, 0.20, 0.10, idAnalisis
        );
        cromaticos.add(cr);

        List<AnalisisDetalleDTO> historial = useCase.obtenerHistorialPorArbol(idArbol);

        assertEquals(1, historial.size());
        AnalisisDetalleDTO detalle = historial.get(0);
        assertEquals("infestado", detalle.resultadoClasificacion());
        assertNotNull(detalle.cromatico(), "Debe incluir los porcentajes del análisis cromático");
        assertEquals(0.65, detalle.cromatico().pctDanado());
        assertEquals(0.10, detalle.cromatico().pctSano());
    }

    @Test
    @DisplayName("Eliminar árbol individual y limpiar todos los árboles con sus análisis")
    void testEliminarArbolYEliminarTodos() {
        UUID idArbol = UUID.randomUUID();
        UUID idAnalisis = UUID.randomUUID();

        Arbol a = new Arbol(
            idArbol, "ArbolParaBorrar", "Cedro blanco (Cupressus lusitanica)",
            -99.18, 19.42, Instant.now(), "Prueba borrado", UUID.randomUUID(), UUID.randomUUID()
        );
        arboles.add(a);

        Analisis an = new Analisis(
            idAnalisis, "/img.jpg", 19.42, -99.18, Instant.now(), Instant.now(),
            "sincronizado", "corteza", "infestado", 0.91, "Daño visible",
            idArbol, null
        );
        analisisList.add(an);

        AnalisisCromatico cr = new AnalisisCromatico(
            UUID.randomUUID(), 0.05, 0.65, 0.20, 0.10, idAnalisis
        );
        cromaticos.add(cr);

        useCase.eliminarArbol(idArbol);
        assertTrue(arboles.isEmpty(), "El árbol individual debió ser eliminado");
        assertTrue(analisisList.isEmpty(), "Los análisis asociados al árbol debieron ser eliminados");
        assertTrue(cromaticos.isEmpty(), "Los análisis cromáticos asociados debieron ser eliminados");

        // Prueba eliminar todos
        arboles.add(a);
        analisisList.add(an);
        cromaticos.add(cr);
        useCase.eliminarTodosArboles();
        assertTrue(arboles.isEmpty());
        assertTrue(analisisList.isEmpty());
        assertTrue(cromaticos.isEmpty());
    }

    @Test
    @DisplayName("Zona Follaje: Registro, actualización, eliminación individual y eliminación masiva")
    void testZonaFollajeCrud() {
        UUID idCuenta = UUID.randomUUID();
        ZonaFollajeDTO dto = new ZonaFollajeDTO(
            null,
            19.4200,
            -99.1800,
            Instant.now(),
            "Zona inicial",
            "Zona Norte",
            75.0,
            null,
            null
        );

        ZonaFollaje creada = useCase.registrarZonaFollaje(dto, idCuenta);
        assertNotNull(creada.idZona());
        assertEquals("Zona Norte", creada.nombreZona());
        assertEquals("Zona inicial", creada.descripcion());

        // Actualización
        ZonaFollajeDTO dtoUpdate = new ZonaFollajeDTO(
            creada.idZona(),
            19.4250,
            -99.1850,
            null,
            "Zona modificada con nuevas observaciones",
            "Zona Norte Actualizada",
            80.0,
            null,
            null
        );
        ZonaFollaje actualizada = useCase.actualizarZonaFollaje(creada.idZona(), dtoUpdate);
        assertEquals("Zona modificada con nuevas observaciones", actualizada.descripcion());
        assertEquals("Zona Norte Actualizada", actualizada.nombreZona());

        // Análisis asociado a la zona
        UUID idAnalisis = UUID.randomUUID();
        Analisis an = new Analisis(
            idAnalisis, "/img_follaje.jpg", 19.4250, -99.1850, Instant.now(), Instant.now(),
            "sincronizado", "follaje", "sano", 0.88, "Follaje denso",
            null, creada.idZona()
        );
        analisisList.add(an);

        AnalisisCromatico cr = new AnalisisCromatico(
            UUID.randomUUID(), 0.05, 0.05, 0.10, 0.80, idAnalisis
        );
        cromaticos.add(cr);

        // Eliminación individual
        useCase.eliminarZonaFollaje(creada.idZona());
        assertTrue(zonas.isEmpty(), "La zona debió ser eliminada");
        assertTrue(analisisList.isEmpty(), "Los análisis asociados a la zona debieron ser eliminados");
        assertTrue(cromaticos.isEmpty(), "Los análisis cromáticos de la zona debieron ser eliminados");

        // Eliminación masiva
        useCase.registrarZonaFollaje(dto, idCuenta);
        assertFalse(zonas.isEmpty());
        useCase.eliminarTodasZonasFollaje();
        assertTrue(zonas.isEmpty());
    }
}
