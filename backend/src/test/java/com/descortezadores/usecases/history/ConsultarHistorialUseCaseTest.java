package com.descortezadores.usecases.history;

import com.descortezadores.adapters.dto.AnalisisDetalleDTO;
import com.descortezadores.adapters.dto.ArbolDTO;
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
                if ("findAllOrderByFechaCapturaDesc".equals(name)) return new ArrayList<>(analisisList);
                if ("findById".equals(name)) {
                    UUID id = (UUID) args[0];
                    return analisisList.stream().filter(a -> a.idAnalisis().equals(id)).findFirst();
                }
                return null;
            }
        );

        analisisCromaticoRepository = (AnalisisCromaticoRepository) Proxy.newProxyInstance(
            AnalisisCromaticoRepository.class.getClassLoader(),
            new Class<?>[]{AnalisisCromaticoRepository.class},
            (p, m, args) -> {
                if ("findByIdAnalisis".equals(m.getName())) {
                    UUID id = (UUID) args[0];
                    return cromaticos.stream().filter(c -> c.idAnalisis().equals(id)).findFirst();
                }
                return null;
            }
        );

        zonaFollajeRepository = (ZonaFollajeRepository) Proxy.newProxyInstance(
            ZonaFollajeRepository.class.getClassLoader(),
            new Class<?>[]{ZonaFollajeRepository.class},
            (p, m, args) -> {
                if ("findAllOrderByFechaRegistroDesc".equals(m.getName())) return new ArrayList<>(zonas);
                if ("findById".equals(m.getName())) {
                    UUID id = (UUID) args[0];
                    return zonas.stream().filter(z -> z.idZona().equals(id)).findFirst();
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
}
