package com.descortezadores.usecases.stats;

import com.descortezadores.adapters.dto.DashboardEstadisticasDTO;
import com.descortezadores.adapters.dto.FocoInfestacionDTO;
import com.descortezadores.domain.entities.Analisis;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.domain.entities.ZonaFollaje;
import com.descortezadores.infrastructure.persistence.AnalisisRepository;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
import com.descortezadores.infrastructure.persistence.ZonaFollajeRepository;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Caso de uso: ConsultarEstadisticasUseCase
 * Estadísticas generales para Dashboard web (RF-21, CU-W08, CU-W19-CU-W23)
 * y cálculo geoespacial de Focos de Infestación activos según NOM-019-SEMARNAT-2017 (RN-25, RN-27).
 */
@Singleton
public class ConsultarEstadisticasUseCase {

    private static final double RADIO_TIERRA_METROS = 6371000.0;
    // Círculo de área 1,000 m2 tiene radio aproximado de 17.84 m (diámetro ~35.7 m)
    private static final double DISTANCIA_MAX_FOCO_METROS = 35.7;

    private final ArbolRepository arbolRepository;
    private final AnalisisRepository analisisRepository;
    private final ZonaFollajeRepository zonaFollajeRepository;

    public ConsultarEstadisticasUseCase(
        ArbolRepository arbolRepository,
        AnalisisRepository analisisRepository,
        ZonaFollajeRepository zonaFollajeRepository
    ) {
        this.arbolRepository = arbolRepository;
        this.analisisRepository = analisisRepository;
        this.zonaFollajeRepository = zonaFollajeRepository;
    }

    public DashboardEstadisticasDTO obtenerDashboardEstadisticas() {
        List<Arbol> arboles = (List<Arbol>) arbolRepository.findAll();
        List<Analisis> analisisList = (List<Analisis>) analisisRepository.findAll();
        List<ZonaFollaje> zonas = (List<ZonaFollaje>) zonaFollajeRepository.findAll();

        // Determinar el último estado de cada árbol a partir del análisis más reciente
        Map<UUID, List<Analisis>> analisisPorArbol = analisisList.stream()
            .filter(a -> a.idArbol() != null)
            .collect(Collectors.groupingBy(Analisis::idArbol));

        long sanos = 0;
        long infestados = 0;
        long noDeterminado = 0;

        for (Arbol a : arboles) {
            List<Analisis> historial = analisisPorArbol.get(a.idArbol());
            if (historial == null || historial.isEmpty()) {
                noDeterminado++;
                continue;
            }
            // Tomar el análisis más reciente
            Analisis masReciente = historial.stream()
                .max(Comparator.comparing(Analisis::fechaCaptura))
                .orElse(historial.get(0));

            String res = masReciente.resultadoClasificacion() != null ? masReciente.resultadoClasificacion().toLowerCase() : "no_determinado";
            if ("sano".equals(res)) {
                sanos++;
            } else if ("infestado".equals(res)) {
                infestados++;
            } else {
                noDeterminado++;
            }
        }

        // Distribución por especie
        Map<String, Long> distribucionEspecie = arboles.stream()
            .collect(Collectors.groupingBy(
                a -> (a.especie() != null && !a.especie().isBlank()) ? a.especie() : "No especificada",
                Collectors.counting()
            ));

        Map<String, Long> distribucionEstado = new LinkedHashMap<>();
        distribucionEstado.put("sano", sanos);
        distribucionEstado.put("infestado", infestados);
        distribucionEstado.put("no_determinado", noDeterminado);

        List<FocoInfestacionDTO> focos = calcularFocosActivos();

        // RN-44: Periodos de mayor riesgo biológico (enero a mayo y septiembre a noviembre) según NOM-019
        java.time.LocalDate hoy = java.time.LocalDate.now(java.time.ZoneId.of("America/Mexico_City"));
        int mes = hoy.getMonthValue();
        boolean alertaEstacional = (mes >= 1 && mes <= 5) || (mes >= 9 && mes <= 11);
        String mensajeAlerta = alertaEstacional
            ? "ALERTA ESTACIONAL NOM-019: Periodo de alto riesgo biologico para proliferacion de insectos descortezadores (enero-mayo / septiembre-noviembre)."
            : "Periodo estacional de riesgo moderado segun NOM-019.";

        return new DashboardEstadisticasDTO(
            arboles.size(),
            sanos,
            infestados,
            noDeterminado,
            analisisList.size(),
            zonas.size(),
            focos.size(),
            distribucionEspecie,
            distribucionEstado,
            alertaEstacional,
            mensajeAlerta
        );
    }

    /**
     * RN-25: 3 o más árboles clasificados como infestados dentro de una superficie de 1,000 m2.
     */
    public List<FocoInfestacionDTO> calcularFocosActivos() {
        List<Arbol> arboles = (List<Arbol>) arbolRepository.findAll();
        List<Analisis> analisisList = (List<Analisis>) analisisRepository.findAll();

        Map<UUID, List<Analisis>> analisisPorArbol = analisisList.stream()
            .filter(a -> a.idArbol() != null)
            .collect(Collectors.groupingBy(Analisis::idArbol));

        // Filtrar árboles cuyo estado actual es infestado y tienen coordenadas válidas
        List<Arbol> arbolesInfestados = arboles.stream()
            .filter(a -> a.latitud() != null && a.longitud() != null)
            .filter(a -> {
                List<Analisis> historial = analisisPorArbol.get(a.idArbol());
                if (historial == null || historial.isEmpty()) return false;
                Analisis rec = historial.stream()
                    .max(Comparator.comparing(Analisis::fechaCaptura))
                    .orElse(historial.get(0));
                return "infestado".equalsIgnoreCase(rec.resultadoClasificacion());
            })
            .toList();

        List<FocoInfestacionDTO> focos = new ArrayList<>();
        Set<UUID> procesados = new HashSet<>();
        int contadorFoco = 1;

        for (Arbol base : arbolesInfestados) {
            if (procesados.contains(base.idArbol())) continue;

            List<Arbol> grupo = new ArrayList<>();
            grupo.add(base);

            for (Arbol candidato : arbolesInfestados) {
                if (!candidato.idArbol().equals(base.idArbol()) && !procesados.contains(candidato.idArbol())) {
                    double dist = calcularDistanciaMetros(base.latitud(), base.longitud(), candidato.latitud(), candidato.longitud());
                    if (dist <= DISTANCIA_MAX_FOCO_METROS) {
                        grupo.add(candidato);
                    }
                }
            }

            // RN-25: Debe tener 3 o más árboles infectados
            if (grupo.size() >= 3) {
                double latCentro = grupo.stream().mapToDouble(Arbol::latitud).average().orElse(base.latitud());
                double lonCentro = grupo.stream().mapToDouble(Arbol::longitud).average().orElse(base.longitud());
                List<UUID> ids = grupo.stream().map(Arbol::idArbol).toList();
                List<String> nombres = grupo.stream().map(Arbol::nombreEspecifico).toList();

                focos.add(new FocoInfestacionDTO(
                    "FOCO-" + contadorFoco++,
                    latCentro,
                    lonCentro,
                    17.84, // radio para 1,000 m2
                    grupo.size(),
                    ids,
                    nombres
                ));

                grupo.forEach(a -> procesados.add(a.idArbol()));
            }
        }

        return focos;
    }

    private double calcularDistanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_METROS * c;
    }
}
