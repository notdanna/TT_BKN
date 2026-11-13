package com.descortezadores.usecases.history;

import com.descortezadores.adapters.dto.AnalisisCromaticoDTO;
import com.descortezadores.adapters.dto.AnalisisDetalleDTO;
import com.descortezadores.adapters.dto.ArbolDTO;
import com.descortezadores.adapters.dto.ZonaFollajeDTO;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public List<Arbol> listarArbolesPorCuenta(UUID idCuenta) {
        return arbolRepository.findByIdCuenta(idCuenta).stream()
            .sorted(Comparator.comparing(Arbol::fechaRegistro, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
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

    /**
     * Borra un árbol y sus análisis. Solo su dueño o un Administrador.
     * @throws IllegalArgumentException si no existe (404)
     * @throws AccesoDenegadoException si el solicitante no puede borrarlo (403)
     */
    public void eliminarArbol(UUID idArbol, Solicitante solicitante) {
        Arbol arbol = arbolRepository.findById(idArbol)
            .orElseThrow(() -> new IllegalArgumentException("Árbol no encontrado: " + idArbol));
        if (!solicitante.puedeModificar(arbol.idCuenta())) {
            throw new AccesoDenegadoException("Solo el dueño del registro o un Administrador puede eliminarlo.");
        }
        analisisRepository.findByIdArbol(idArbol).forEach(a -> {
            analisisCromaticoRepository.findByIdAnalisis(a.idAnalisis())
                .ifPresent(c -> analisisCromaticoRepository.deleteById(c.idAnalisisCromatico()));
            analisisRepository.delete(a);
        });
        arbolRepository.deleteById(idArbol);
    }

    /** Vacía el catálogo de árboles. El controlador lo restringe a Administrador. */
    public void eliminarTodosArboles() {
        analisisCromaticoRepository.deleteAll();
        analisisRepository.deleteAll();
        arbolRepository.deleteAll();
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

    public List<AnalisisDetalleDTO> obtenerTodosLosAnalisisPorCuenta(UUID idCuenta) {
        Set<UUID> arbolIds = arbolRepository.findByIdCuenta(idCuenta).stream()
            .map(Arbol::idArbol)
            .collect(Collectors.toSet());
        Set<UUID> zonaIds = zonaFollajeRepository.findByIdCuenta(idCuenta).stream()
            .map(ZonaFollaje::idZona)
            .collect(Collectors.toSet());

        return analisisRepository.findAllOrderByFechaCapturaDesc().stream()
            .filter(a -> (a.idArbol() != null && arbolIds.contains(a.idArbol()))
                || (a.idZona() != null && zonaIds.contains(a.idZona())))
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

    public List<AnalisisDetalleDTO> obtenerAnalisisPorRangoYCuenta(Instant inicio, Instant fin, UUID idCuenta) {
        Set<UUID> arbolIds = arbolRepository.findByIdCuenta(idCuenta).stream()
            .map(Arbol::idArbol)
            .collect(Collectors.toSet());
        Set<UUID> zonaIds = zonaFollajeRepository.findByIdCuenta(idCuenta).stream()
            .map(ZonaFollaje::idZona)
            .collect(Collectors.toSet());

        return analisisRepository.findByFechaCapturaBetween(inicio, fin).stream()
            .filter(a -> (a.idArbol() != null && arbolIds.contains(a.idArbol()))
                || (a.idZona() != null && zonaIds.contains(a.idZona())))
            .map(this::toDetalleDTO)
            .toList();
    }

    public List<ZonaFollaje> listarTodasZonasFollaje() {
        return zonaFollajeRepository.findAllOrderByFechaRegistroDesc();
    }

    public List<ZonaFollaje> listarZonasFollajePorCuenta(UUID idCuenta) {
        return zonaFollajeRepository.findByIdCuenta(idCuenta).stream()
            .sorted(Comparator.comparing(ZonaFollaje::fechaRegistro, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    public Optional<ZonaFollaje> obtenerZonaFollajePorId(UUID idZona) {
        return zonaFollajeRepository.findById(idZona);
    }

    public ZonaFollaje registrarZonaFollaje(ZonaFollajeDTO dto, UUID idCuenta) {
        UUID id = dto.idZona() != null ? dto.idZona() : UUID.randomUUID();
        var existente = zonaFollajeRepository.findById(id);
        if (existente.isPresent()) {
            return existente.get();
        }
        ZonaFollaje nueva = new ZonaFollaje(
            id,
            dto.latitudCentro() != null ? dto.latitudCentro() : 19.4200,
            dto.longitudCentro() != null ? dto.longitudCentro() : -99.1800,
            dto.fechaRegistro() != null ? dto.fechaRegistro() : Instant.now(),
            dto.descripcion(),
            dto.nombreZona() != null ? dto.nombreZona() : "Zona-" + id.toString().substring(0, 5),
            dto.radioMetros() != null ? dto.radioMetros() : 50.0,
            dto.idDispositivo(),
            idCuenta
        );
        return zonaFollajeRepository.save(nueva);
    }

    public ZonaFollaje actualizarZonaFollaje(UUID idZona, ZonaFollajeDTO dto) {
        ZonaFollaje existente = zonaFollajeRepository.findById(idZona)
            .orElseThrow(() -> new IllegalArgumentException("Zona de follaje no encontrada con ID: " + idZona));

        ZonaFollaje actualizada = new ZonaFollaje(
            existente.idZona(),
            dto.latitudCentro() != null ? dto.latitudCentro() : existente.latitudCentro(),
            dto.longitudCentro() != null ? dto.longitudCentro() : existente.longitudCentro(),
            existente.fechaRegistro(),
            dto.descripcion() != null ? dto.descripcion() : existente.descripcion(),
            dto.nombreZona() != null ? dto.nombreZona() : existente.nombreZona(),
            dto.radioMetros() != null ? dto.radioMetros() : existente.radioMetros(),
            existente.idDispositivo(),
            existente.idCuenta()
        );
        return zonaFollajeRepository.update(actualizada);
    }

    /**
     * Borra una zona y sus análisis. Solo su dueño o un Administrador.
     * @throws IllegalArgumentException si no existe (404)
     * @throws AccesoDenegadoException si el solicitante no puede borrarla (403)
     */
    public void eliminarZonaFollaje(UUID idZona, Solicitante solicitante) {
        ZonaFollaje zona = zonaFollajeRepository.findById(idZona)
            .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada: " + idZona));
        if (!solicitante.puedeModificar(zona.idCuenta())) {
            throw new AccesoDenegadoException("Solo el dueño del registro o un Administrador puede eliminarlo.");
        }
        analisisRepository.findByIdZona(idZona).forEach(a -> {
            analisisCromaticoRepository.findByIdAnalisis(a.idAnalisis())
                .ifPresent(c -> analisisCromaticoRepository.deleteById(c.idAnalisisCromatico()));
            analisisRepository.delete(a);
        });
        zonaFollajeRepository.deleteById(idZona);
    }

    /** Vacía el catálogo de zonas. El controlador lo restringe a Administrador. */
    public void eliminarTodasZonasFollaje() {
        analisisRepository.findAll().stream()
            .filter(a -> a.idZona() != null)
            .forEach(a -> {
                analisisCromaticoRepository.findByIdAnalisis(a.idAnalisis())
                    .ifPresent(c -> analisisCromaticoRepository.deleteById(c.idAnalisisCromatico()));
                analisisRepository.delete(a);
            });
        zonaFollajeRepository.deleteAll();
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
