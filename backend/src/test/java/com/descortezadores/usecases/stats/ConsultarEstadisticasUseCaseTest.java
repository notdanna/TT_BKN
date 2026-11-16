package com.descortezadores.usecases.stats;

import com.descortezadores.adapters.dto.DashboardEstadisticasDTO;
import com.descortezadores.adapters.dto.FocoInfestacionDTO;
import com.descortezadores.domain.entities.Analisis;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.domain.entities.ZonaFollaje;
import com.descortezadores.infrastructure.persistence.AnalisisRepository;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
import com.descortezadores.infrastructure.persistence.ZonaFollajeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Estadísticas y Focos NOM-019 (RF-21, RN-25, RN-27)")
class ConsultarEstadisticasUseCaseTest {

    @Test
    @DisplayName("RF-21: Cálculo correcto de distribución por estado y especie en Dashboard")
    void testCalculoEstadisticasDashboard() {
        UUID cuentaId = UUID.randomUUID();
        UUID arbol1Id = UUID.randomUUID();
        UUID arbol2Id = UUID.randomUUID();
        UUID arbol3Id = UUID.randomUUID();

        Arbol a1 = new Arbol(arbol1Id, "Fresno-01", "Fraxinus uhdei", -99.182, 19.421, Instant.now(), null, cuentaId, null);
        Arbol a2 = new Arbol(arbol2Id, "Cedro-01", "Cupressus lusitanica", -99.183, 19.422, Instant.now(), null, cuentaId, null);
        Arbol a3 = new Arbol(arbol3Id, "Fresno-02", "Fraxinus uhdei", -99.184, 19.423, Instant.now(), null, cuentaId, null);

        List<Arbol> arboles = List.of(a1, a2, a3);

        // a1 es infestado, a2 es sano, a3 no tiene análisis (no_determinado)
        Analisis an1 = new Analisis(UUID.randomUUID(), "/img1.jpg", 19.421, -99.182, Instant.now(), Instant.now(), "sincronizado", "corteza", "infestado", 0.95, null, arbol1Id, null);
        Analisis an2 = new Analisis(UUID.randomUUID(), "/img2.jpg", 19.422, -99.183, Instant.now(), Instant.now(), "sincronizado", "corteza", "sano", 0.88, null, arbol2Id, null);

        List<Analisis> analisisList = List.of(an1, an2);
        List<ZonaFollaje> zonas = List.of();

        ConsultarEstadisticasUseCase useCase = crearUseCase(arboles, analisisList, zonas);
        DashboardEstadisticasDTO stats = useCase.obtenerDashboardEstadisticas();

        assertEquals(3, stats.totalArboles(), "Total de árboles debe ser 3");
        assertEquals(1, stats.totalSanos(), "Total sanos debe ser 1");
        assertEquals(1, stats.totalInfestados(), "Total infestados debe ser 1");
        assertEquals(1, stats.totalNoDeterminado(), "Total sin revisar debe ser 1");
        assertEquals(2, stats.totalAnalisis(), "Total de análisis debe ser 2");
        assertEquals(2L, stats.distribucionPorEspecie().get("Fraxinus uhdei"));
        assertEquals(1L, stats.distribucionPorEspecie().get("Cupressus lusitanica"));
    }

    @Test
    @DisplayName("RN-25: Detección de Foco Activo con 3+ árboles infectados en <= 1,000 m2")
    void testDeteccionFocoActivoNOM019() {
        UUID cuentaId = UUID.randomUUID();
        UUID a1Id = UUID.randomUUID();
        UUID a2Id = UUID.randomUUID();
        UUID a3Id = UUID.randomUUID();

        // Coordenadas en Chapultepec muy cercanas (< 15 metros entre sí)
        Arbol a1 = new Arbol(a1Id, "Fresno-Foco-1", "Fraxinus uhdei", -99.18200, 19.42100, Instant.now(), null, cuentaId, null);
        Arbol a2 = new Arbol(a2Id, "Fresno-Foco-2", "Fraxinus uhdei", -99.18205, 19.42105, Instant.now(), null, cuentaId, null);
        Arbol a3 = new Arbol(a3Id, "Fresno-Foco-3", "Fraxinus uhdei", -99.18208, 19.42108, Instant.now(), null, cuentaId, null);

        List<Arbol> arboles = List.of(a1, a2, a3);

        Analisis an1 = new Analisis(UUID.randomUUID(), "/img1.jpg", 19.42100, -99.18200, Instant.now(), Instant.now(), "sincronizado", "corteza", "infestado", 0.92, null, a1Id, null);
        Analisis an2 = new Analisis(UUID.randomUUID(), "/img2.jpg", 19.42105, -99.18205, Instant.now(), Instant.now(), "sincronizado", "corteza", "infestado", 0.90, null, a2Id, null);
        Analisis an3 = new Analisis(UUID.randomUUID(), "/img3.jpg", 19.42108, -99.18208, Instant.now(), Instant.now(), "sincronizado", "corteza", "infestado", 0.94, null, a3Id, null);

        List<Analisis> analisisList = List.of(an1, an2, an3);

        ConsultarEstadisticasUseCase useCase = crearUseCase(arboles, analisisList, List.of());
        List<FocoInfestacionDTO> focos = useCase.calcularFocosActivos();

        assertEquals(1, focos.size(), "Debe detectarse exactamente 1 foco activo según NOM-019");
        FocoInfestacionDTO foco = focos.get(0);
        assertEquals(3, foco.cantidadArbolesInfectados());
        assertTrue(foco.idsArboles().contains(a1Id));
        assertTrue(foco.idsArboles().contains(a2Id));
        assertTrue(foco.idsArboles().contains(a3Id));
    }

    @Test
    @DisplayName("RN-25: Menos de 3 árboles infectados NO forman un foco activo")
    void testMenosDeTresArbolesNoFormanFoco() {
        UUID cuentaId = UUID.randomUUID();
        UUID a1Id = UUID.randomUUID();
        UUID a2Id = UUID.randomUUID();

        // Solo 2 árboles infectados
        Arbol a1 = new Arbol(a1Id, "Fresno-1", "Fraxinus uhdei", -99.18200, 19.42100, Instant.now(), null, cuentaId, null);
        Arbol a2 = new Arbol(a2Id, "Fresno-2", "Fraxinus uhdei", -99.18205, 19.42105, Instant.now(), null, cuentaId, null);

        List<Arbol> arboles = List.of(a1, a2);

        Analisis an1 = new Analisis(UUID.randomUUID(), "/img1.jpg", 19.42100, -99.18200, Instant.now(), Instant.now(), "sincronizado", "corteza", "infestado", 0.92, null, a1Id, null);
        Analisis an2 = new Analisis(UUID.randomUUID(), "/img2.jpg", 19.42105, -99.18205, Instant.now(), Instant.now(), "sincronizado", "corteza", "infestado", 0.90, null, a2Id, null);

        ConsultarEstadisticasUseCase useCase = crearUseCase(arboles, List.of(an1, an2), List.of());
        List<FocoInfestacionDTO> focos = useCase.calcularFocosActivos();

        assertTrue(focos.isEmpty(), "Con 2 árboles no debe considerarse foco activo bajo NOM-019");
    }

    @SuppressWarnings("unchecked")
    private ConsultarEstadisticasUseCase crearUseCase(List<Arbol> arboles, List<Analisis> analisis, List<ZonaFollaje> zonas) {
        ArbolRepository arbolRepo = (ArbolRepository) Proxy.newProxyInstance(
            ArbolRepository.class.getClassLoader(),
            new Class<?>[]{ArbolRepository.class},
            (p, m, args) -> "findAll".equals(m.getName()) ? new ArrayList<>(arboles) : null
        );

        AnalisisRepository analisisRepo = (AnalisisRepository) Proxy.newProxyInstance(
            AnalisisRepository.class.getClassLoader(),
            new Class<?>[]{AnalisisRepository.class},
            (p, m, args) -> "findAll".equals(m.getName()) ? new ArrayList<>(analisis) : null
        );

        ZonaFollajeRepository zonaRepo = (ZonaFollajeRepository) Proxy.newProxyInstance(
            ZonaFollajeRepository.class.getClassLoader(),
            new Class<?>[]{ZonaFollajeRepository.class},
            (p, m, args) -> "findAll".equals(m.getName()) ? new ArrayList<>(zonas) : null
        );

        return new ConsultarEstadisticasUseCase(arbolRepo, analisisRepo, zonaRepo);
    }
}
