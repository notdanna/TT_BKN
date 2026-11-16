package com.descortezadores.usecases.stats;

import com.descortezadores.adapters.dto.ReporteDTOs;
import com.descortezadores.domain.entities.Analisis;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.domain.entities.Cuenta;
import com.descortezadores.infrastructure.persistence.AnalisisRepository;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
import com.descortezadores.infrastructure.persistence.CuentaRepository;
import jakarta.inject.Singleton;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Caso de Uso: Generación y Descarga de Reportes (RF-16, RF-17, RN-33, RN-34, PT-12, CU-W23).
 *
 * Cumple con los requerimientos:
 * - RN-33: Incluye el historial sincronizado de un periodo de tiempo o de una zona específica.
 * - RN-34: Incluye obligatoriamente: ID árbol, nombre del árbol, fecha/hora de captura,
 *          resultado de clasificación, coordenadas GPS y nombre del experto.
 * - PT-12: Genera y permite descargar el archivo PDF.
 */
@Singleton
public class GenerarReporteUseCase {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("America/Mexico_City"));

    private final AnalisisRepository analisisRepository;
    private final ArbolRepository arbolRepository;
    private final CuentaRepository cuentaRepository;

    public GenerarReporteUseCase(
            AnalisisRepository analisisRepository,
            ArbolRepository arbolRepository,
            CuentaRepository cuentaRepository) {
        this.analisisRepository = analisisRepository;
        this.arbolRepository = arbolRepository;
        this.cuentaRepository = cuentaRepository;
    }

    /**
     * Obtiene los datos tabulares del reporte aplicando filtros.
     */
    public ReporteDTOs.ReporteGeneralDTO obtenerDatosReporte(
            Instant fechaInicio,
            Instant fechaFin,
            UUID idZona,
            String estadoSanitario) {

        List<Analisis> todos = new ArrayList<>();
        analisisRepository.findAll().forEach(todos::add);

        // Mapas de cache para evitar n+1
        Map<UUID, Arbol> arbolesMap = new HashMap<>();
        arbolRepository.findAll().forEach(a -> arbolesMap.put(a.idArbol(), a));

        Map<UUID, Cuenta> cuentasMap = new HashMap<>();
        cuentaRepository.findAll().forEach(c -> cuentasMap.put(c.idCuenta(), c));

        List<ReporteDTOs.ReporteItemDTO> items = new ArrayList<>();

        for (Analisis a : todos) {
            // Filtro de fecha inicio
            if (fechaInicio != null && a.fechaCaptura().isBefore(fechaInicio)) {
                continue;
            }
            // Filtro de fecha fin
            if (fechaFin != null && a.fechaCaptura().isAfter(fechaFin)) {
                continue;
            }
            // Filtro de zona
            if (idZona != null && (a.idZona() == null || !a.idZona().equals(idZona))) {
                continue;
            }
            // Filtro de estado sanitario
            if (estadoSanitario != null && !estadoSanitario.isBlank()
                    && !estadoSanitario.equalsIgnoreCase(a.resultadoClasificacion())) {
                continue;
            }

            Arbol arbol = a.idArbol() != null ? arbolesMap.get(a.idArbol()) : null;
            String nombreArbol = arbol != null ? arbol.nombreEspecifico() : "Zona Follaje Macro";
            UUID idArbol = arbol != null ? arbol.idArbol() : null;

            String nombreExperto = "Experto Forestal";
            if (arbol != null && arbol.idCuenta() != null) {
                Cuenta c = cuentasMap.get(arbol.idCuenta());
                if (c != null) {
                    nombreExperto = c.nombreCompleto();
                }
            }

            items.add(new ReporteDTOs.ReporteItemDTO(
                    idArbol,
                    nombreArbol,
                    a.fechaCaptura(),
                    a.resultadoClasificacion(),
                    a.latitudCaptura() != null ? a.latitudCaptura() : 0.0,
                    a.longitudCaptura() != null ? a.longitudCaptura() : 0.0,
                    nombreExperto,
                    a.tipoImagen()
            ));
        }

        // Ordenar por fecha descendente
        items.sort(Comparator.comparing(ReporteDTOs.ReporteItemDTO::fechaCaptura).reversed());

        int total = items.size();
        int sanos = (int) items.stream().filter(i -> "sano".equalsIgnoreCase(i.resultadoClasificacion())).count();
        int infestados = (int) items.stream().filter(i -> "infestado".equalsIgnoreCase(i.resultadoClasificacion())).count();
        int noDeterminados = total - (sanos + infestados);

        return new ReporteDTOs.ReporteGeneralDTO(
                "Reporte de Monitoreo Sanitario - Bosque de Chapultepec (TT 2026-B161)",
                Instant.now(),
                fechaInicio,
                fechaFin,
                total,
                sanos,
                infestados,
                noDeterminados,
                items
        );
    }

    /**
     * Genera un documento PDF estándar conforme a la norma ISO 32000-1 (PDF-1.4).
     */
    public byte[] generarPdf(Instant fechaInicio, Instant fechaFin, UUID idZona, String estadoSanitario) {
        ReporteDTOs.ReporteGeneralDTO datos = obtenerDatosReporte(fechaInicio, fechaFin, idZona, estadoSanitario);

        StringBuilder sb = new StringBuilder();
        sb.append("BT\n");
        sb.append("/F1 16 Tf\n");
        sb.append("50 750 Td\n");
        sb.append("(SISTEMA DE DETECCION DE INSECTOS DESCORTEZADORES) Tj\n");
        sb.append("/F1 12 Tf\n");
        sb.append("0 -20 Td\n");
        sb.append("(CASO DE ESTUDIO: BOSQUE DE CHAPULTEPEC - TT 2026-B161) Tj\n");
        sb.append("0 -20 Td\n");
        sb.append("(Fecha emision: ").append(FORMATTER.format(datos.fechaGeneracion())).append(") Tj\n");
        sb.append("0 -15 Td\n");
        sb.append("(Total muestras: ").append(datos.totalCapturas())
          .append(" | Sanos: ").append(datos.totalSanos())
          .append(" | Infestados: ").append(datos.totalInfestados())
          .append(" | No determinados: ").append(datos.totalNoDeterminados()).append(") Tj\n");
        sb.append("0 -25 Td\n");

        sb.append("/F1 10 Tf\n");
        sb.append("(-----------------------------------------------------------------------------------------) Tj\n");
        sb.append("0 -15 Td\n");
        sb.append("(ARBOL                    | FECHA/HORA       | ESTADO        | COORDENADAS GPS      | EXPERTO) Tj\n");
        sb.append("0 -12 Td\n");
        sb.append("(-----------------------------------------------------------------------------------------) Tj\n");

        int lineCount = 0;
        for (ReporteDTOs.ReporteItemDTO item : datos.items()) {
            if (lineCount >= 30) {
                sb.append("0 -15 Td\n");
                sb.append("(... y ").append(datos.items().size() - lineCount).append(" registros adicionales en la base de datos ...) Tj\n");
                break;
            }

            String arbolNom = padRight(item.nombreArbol(), 24);
            String fecha = padRight(FORMATTER.format(item.fechaCaptura()), 16);
            String estado = padRight(item.resultadoClasificacion().toUpperCase(), 13);
            String coords = padRight(String.format(Locale.US, "%.5f, %.5f", item.latitud(), item.longitud()), 21);
            String experto = padRight(item.nombreExperto(), 15);

            String linea = String.format("%s | %s | %s | %s | %s", arbolNom, fecha, estado, coords, experto);
            linea = sanitizarTextoPdf(linea);

            sb.append("0 -14 Td\n");
            sb.append("(").append(linea).append(") Tj\n");
            lineCount++;
        }

        sb.append("0 -20 Td\n");
        sb.append("(-----------------------------------------------------------------------------------------) Tj\n");
        sb.append("0 -15 Td\n");
        sb.append("/F1 8 Tf\n");
        sb.append("(Reporte oficial generado conforme a NOM-019-SEMARNAT-2017 y especificaciones TT 2026-B161) Tj\n");
        sb.append("ET\n");

        byte[] streamBytes = sb.toString().getBytes(StandardCharsets.ISO_8859_1);

        return ensamblarPdf14(streamBytes);
    }

    private byte[] ensamblarPdf14(byte[] streamBytes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<>();

            out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));

            // Objeto 1: Catalog
            offsets.add(out.size());
            out.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Objeto 2: Pages
            offsets.add(out.size());
            out.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Objeto 3: Page
            offsets.add(out.size());
            out.write("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Objeto 4: Font
            offsets.add(out.size());
            out.write("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // Objeto 5: Stream
            offsets.add(out.size());
            out.write(String.format("5 0 obj\n<< /Length %d >>\nstream\n", streamBytes.length).getBytes(StandardCharsets.ISO_8859_1));
            out.write(streamBytes);
            out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            // xref
            int startXref = out.size();
            out.write(String.format("xref\n0 %d\n0000000000 65535 f \n", offsets.size() + 1).getBytes(StandardCharsets.ISO_8859_1));
            for (int offset : offsets) {
                out.write(String.format(Locale.US, "%010d 00000 n \n", offset).getBytes(StandardCharsets.ISO_8859_1));
            }

            // trailer
            out.write(String.format("trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n",
                    offsets.size() + 1, startXref).getBytes(StandardCharsets.ISO_8859_1));

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al ensamblar documento PDF: " + e.getMessage(), e);
        }
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        if (s.length() > n) {
            return s.substring(0, n);
        }
        return String.format("%-" + n + "s", s);
    }

    private String sanitizarTextoPdf(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("(", "\\(")
                   .replace(")", "\\)")
                   .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                   .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
                   .replace("ñ", "n").replace("Ñ", "N");
    }
}
