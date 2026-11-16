package com.descortezadores.usecases.stats;

import com.descortezadores.adapters.dto.ReporteDTOs;
import com.descortezadores.domain.entities.Analisis;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.domain.entities.Cuenta;
import com.descortezadores.infrastructure.persistence.AnalisisRepository;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
import com.descortezadores.infrastructure.persistence.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Generación de Reportes PDF (RF-16, RF-17, RN-33, RN-34, PT-12)")
class GenerarReporteUseCaseTest {

    private List<Analisis> analisisList;
    private List<Arbol> arbolesList;
    private List<Cuenta> cuentasList;

    private AnalisisRepository analisisRepository;
    private ArbolRepository arbolRepository;
    private CuentaRepository cuentaRepository;

    private GenerarReporteUseCase useCase;

    @BeforeEach
    void setUp() {
        analisisList = new ArrayList<>();
        arbolesList = new ArrayList<>();
        cuentasList = new ArrayList<>();

        UUID idCuenta = UUID.randomUUID();
        cuentasList.add(new Cuenta(idCuenta, "experto@chapultepec.gob.mx", "hash", "Experto de campo", "Activa", Instant.now(), "Damián Franco"));

        UUID idArbol1 = UUID.randomUUID();
        UUID idArbol2 = UUID.randomUUID();

        arbolesList.add(new Arbol(idArbol1, "Fresno Seccion 2", "Fraxinus uhdei", -99.19, 19.41, Instant.now(), null, idCuenta, null));
        arbolesList.add(new Arbol(idArbol2, "Cedro Castillo", "Cupressus lusitanica", -99.18, 19.42, Instant.now(), null, idCuenta, null));

        analisisList.add(new Analisis(UUID.randomUUID(), "/img1.jpg", 19.41, -99.19, Instant.now(), Instant.now(), "sincronizado", "corteza", "infestado", 0.95, "Grumos resina", idArbol1, null));
        analisisList.add(new Analisis(UUID.randomUUID(), "/img2.jpg", 19.42, -99.18, Instant.now(), Instant.now(), "sincronizado", "corteza", "sano", 0.90, "Sin indicios", idArbol2, null));

        analisisRepository = (AnalisisRepository) Proxy.newProxyInstance(
            AnalisisRepository.class.getClassLoader(),
            new Class<?>[]{AnalisisRepository.class},
            (p, m, args) -> "findAll".equals(m.getName()) ? analisisList : null
        );

        arbolRepository = (ArbolRepository) Proxy.newProxyInstance(
            ArbolRepository.class.getClassLoader(),
            new Class<?>[]{ArbolRepository.class},
            (p, m, args) -> "findAll".equals(m.getName()) ? arbolesList : null
        );

        cuentaRepository = (CuentaRepository) Proxy.newProxyInstance(
            CuentaRepository.class.getClassLoader(),
            new Class<?>[]{CuentaRepository.class},
            (p, m, args) -> "findAll".equals(m.getName()) ? cuentasList : null
        );

        useCase = new GenerarReporteUseCase(analisisRepository, arbolRepository, cuentaRepository);
    }

    @Test
    @DisplayName("RN-34 / RN-33: Consulta estructurada de datos del reporte con columnas mínimas obligatorias")
    void testObtenerDatosReporte() {
        ReporteDTOs.ReporteGeneralDTO reporte = useCase.obtenerDatosReporte(null, null, null, null);

        assertNotNull(reporte);
        assertEquals(2, reporte.totalCapturas());
        assertEquals(1, reporte.totalSanos());
        assertEquals(1, reporte.totalInfestados());
        assertEquals(0, reporte.totalNoDeterminados());

        assertEquals(2, reporte.items().size());
        ReporteDTOs.ReporteItemDTO item = reporte.items().get(0);
        assertNotNull(item.idArbol(), "RN-34: Identificador del árbol obligatorio");
        assertNotNull(item.nombreArbol(), "RN-34: Nombre del árbol obligatorio");
        assertNotNull(item.fechaCaptura(), "RN-34: Fecha de captura obligatoria");
        assertNotNull(item.resultadoClasificacion(), "RN-34: Resultado de clasificación obligatorio");
        assertNotNull(item.latitud(), "RN-34: Coordenadas GPS obligatorias");
        assertNotNull(item.longitud(), "RN-34: Coordenadas GPS obligatorias");
        assertEquals("Damián Franco", item.nombreExperto(), "RN-34: Nombre del experto obligatorio");
    }

    @Test
    @DisplayName("RF-16 / RF-17 / PT-12: Generación exitosa de documento binario en formato estándar PDF 1.4")
    void testGenerarPdfValido() {
        byte[] pdfBytes = useCase.generarPdf(null, null, null, null);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100, "El PDF debe tener un tamaño mínimo de estructura");

        String pdfHeader = new String(Arrays.copyOfRange(pdfBytes, 0, 8), StandardCharsets.ISO_8859_1);
        assertEquals("%PDF-1.4", pdfHeader, "Debe iniciar con cabecera estándar PDF-1.4");

        String pdfContent = new String(pdfBytes, StandardCharsets.ISO_8859_1);
        assertTrue(pdfContent.contains("%%EOF"), "Debe terminar con indicador de cierre %%EOF");
        assertTrue(pdfContent.contains("SISTEMA DE DETECCION DE INSECTOS DESCORTEZADORES"));
        assertTrue(pdfContent.contains("BOSQUE DE CHAPULTEPEC"));
    }

    @Test
    @DisplayName("RN-33: Filtro por rango de fechas en reporte")
    void testFiltroPorFechas() {
        Instant fechaFutura = Instant.now().plus(10, ChronoUnit.DAYS);
        ReporteDTOs.ReporteGeneralDTO vacio = useCase.obtenerDatosReporte(fechaFutura, null, null, null);
        assertEquals(0, vacio.totalCapturas());
    }
}
