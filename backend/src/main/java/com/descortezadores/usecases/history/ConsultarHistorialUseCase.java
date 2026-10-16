package com.descortezadores.usecases.history;

import com.descortezadores.adapters.dto.AnalisisCromaticoDTO;
import com.descortezadores.adapters.dto.AnalisisDetalleDTO;
import com.descortezadores.adapters.dto.ArbolDTO;
import com.descortezadores.domain.entities.Analisis;
import com.descortezadores.domain.entities.AnalisisCromatico;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.domain.entities.ZonaFollaje;
import com.descortezadores.infrastructure.persistence.AnalisisCromaticoRepository;
import com.descortezadores.infrastructure.persistence.AnalisisRepository;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
import com.descortezadores.infrastructure.persistence.ZonaFollajeRepository;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: ConsultarHistorialUseCase
 * Consultas para dashboard, historial y mapas web/móvil (§8.8.2 y §8.8.3, CU-W06, CU-W10-CU-W15).
 */
@Singleton
public class ConsultarHistorialUseCase {

    private final ArbolRepository arbolRepository;
    private final AnalisisRepository analisisRepository;
    private final AnalisisCromaticoRepository analisisCromaticoRepository;
    private final ZonaFollajeRepository zonaFollajeRepository;

    public ConsultarHistorialUseCase(
        ArbolRepository arbolRepository,
        AnalisisRepository analisisRepository,
        AnalisisCromaticoRepository analisisCromaticoRepository,
        ZonaFollajeRepository zonaFollajeRepository
    ) {
        this.arbolRepository = arbolRepository;
        this.analisisRepository = analisisRepository;
        this.analisisCromaticoRepository = analisisCromaticoRepository;
        this.zonaFollajeRepository = zonaFollajeRepository;
    }

    public List<Arbol> listarTodosArboles() {
        return arbolRepository.findAllOrderByFechaRegistroDesc();
    }

    public Optional<Arbol> obtenerArbolPorId(UUID idArbol) {
        return arbolRepository.findById(idArbol);
    }

    public Arbol actualizarArbol(UUID idArbol, ArbolDTO dto) {
        Arbol existente = arbolRepository.findById(idArbol)
            .orElseThrow(() -> new IllegalArgumentException("Árbol no encontrado con ID: " + idArbol));

        Arbol actualizado = new Arbol(
            existente.idArbol(),
            dto.nombreEspecifico() != null ? dto.nombreEspecifico() : existente.nombreEspecifico(),
            dto.especie() != null ? dto.especie() : existente.especie(),
            dto.longitud() != null ? dto.longitud() : existente.longitud(),
            dto.latitud() != null ? dto.latitud() : existente.latitud(),
            existente.fechaRegistro(),
            dto.descripcion() != null ? dto.descripcion() : existente.descripcion(),
            existente.idCuenta(),
            existente.idDispositivo()
        );

        return arbolRepository.update(actualizado);
    }

    public List<AnalisisDetalleDTO> obtenerHistorialPorArbol(UUID idArbol) {
        return analisisRepository.findByIdArbol(idArbol).stream()
            .map(this::toDetalleDTO)
            .toList();
    }

    public List<AnalisisDetalleDTO> obtenerHistorialPorZona(UUID idZona) {
        return analisisRepository.findByIdZona(idZona).stream()
            .map(this::toDetalleDTO)
            .toList();
    }

    public List<AnalisisDetalleDTO> obtenerTodosLosAnalisis() {
        return analisisRepository.findAllOrderByFechaCapturaDesc().stream()
            .map(this::toDetalleDTO)
            .toList();
    }

    public Optional<AnalisisDetalleDTO> obtenerAnalisisPorId(UUID idAnalisis) {
        return analisisRepository.findById(idAnalisis)
            .map(this::toDetalleDTO);
    }

    public List<AnalisisDetalleDTO> obtenerAnalisisPorRango(Instant inicio, Instant fin) {
        return analisisRepository.findByFechaCapturaBetween(inicio, fin).stream()
            .map(this::toDetalleDTO)
            .toList();
    }

    public List<ZonaFollaje> listarTodasZonasFollaje() {
        return zonaFollajeRepository.findAllOrderByFechaRegistroDesc();
    }

    public Optional<ZonaFollaje> obtenerZonaFollajePorId(UUID idZona) {
        return zonaFollajeRepository.findById(idZona);
    }

    private AnalisisDetalleDTO toDetalleDTO(Analisis a) {
        AnalisisCromaticoDTO cromaticoDTO = analisisCromaticoRepository.findByIdAnalisis(a.idAnalisis())
            .map(c -> new AnalisisCromaticoDTO(
                c.idAnalisisCromatico(),
                c.pctIncierto(),
                c.pctDanado(),
                c.pctRegular(),
                c.pctSano()
            ))
            .orElse(null);

        return new AnalisisDetalleDTO(
            a.idAnalisis(),
            a.rutaEvidencia(),
            a.latitudCaptura(),
            a.longitudCaptura(),
            a.fechaCaptura(),
            a.fechaSincronizacion(),
            a.estadoSincronizacion(),
            a.tipoImagen(),
            a.resultadoClasificacion(),
            a.confianza(),
            a.observaciones(),
            a.idArbol(),
            a.idZona(),
            cromaticoDTO
        );
    }
}
