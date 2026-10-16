package com.descortezadores.usecases.sync;

import com.descortezadores.adapters.dto.SyncBatchResponse;
import com.descortezadores.adapters.dto.SyncItemDTO;
import com.descortezadores.domain.entities.Analisis;
import com.descortezadores.domain.entities.AnalisisCromatico;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.domain.entities.ZonaFollaje;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.domain.rules.ReglasNegocioImagen;
import com.descortezadores.infrastructure.persistence.AnalisisCromaticoRepository;
import com.descortezadores.infrastructure.persistence.AnalisisRepository;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
import com.descortezadores.infrastructure.persistence.CuentaDispositivoRepository;
import com.descortezadores.infrastructure.persistence.ZonaFollajeRepository;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: SyncDeteccionUseCase
 * Implementa el flujo de sincronización móvil -> nube (§8.2.5).
 * Soporta capturas de corteza (asociadas a Árbol) y capturas de follaje (asociadas a ZonaFollaje),
 * cumpliendo con la restricción chk_arbol_o_zona y las reglas de negocio RN-07 a RN-52.
 */
@Singleton
public class SyncDeteccionUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyncDeteccionUseCase.class);

    private final ArbolRepository arbolRepository;
    private final ZonaFollajeRepository zonaFollajeRepository;
    private final AnalisisRepository analisisRepository;
    private final AnalisisCromaticoRepository analisisCromaticoRepository;
    private final CuentaDispositivoRepository cuentaDispositivoRepository;
    private final com.descortezadores.infrastructure.storage.AlmacenamientoService almacenamientoService;

    public SyncDeteccionUseCase(
        ArbolRepository arbolRepository,
        ZonaFollajeRepository zonaFollajeRepository,
        AnalisisRepository analisisRepository,
        AnalisisCromaticoRepository analisisCromaticoRepository,
        CuentaDispositivoRepository cuentaDispositivoRepository,
        com.descortezadores.infrastructure.storage.AlmacenamientoService almacenamientoService
    ) {
        this.arbolRepository = arbolRepository;
        this.zonaFollajeRepository = zonaFollajeRepository;
        this.analisisRepository = analisisRepository;
        this.analisisCromaticoRepository = analisisCromaticoRepository;
        this.cuentaDispositivoRepository = cuentaDispositivoRepository;
        this.almacenamientoService = almacenamientoService;
    }

    /**
     * Constructor secundario de conveniencia para pruebas que usan directorio temporal directamente
     */
    public SyncDeteccionUseCase(
        ArbolRepository arbolRepository,
        ZonaFollajeRepository zonaFollajeRepository,
        AnalisisRepository analisisRepository,
        AnalisisCromaticoRepository analisisCromaticoRepository,
        CuentaDispositivoRepository cuentaDispositivoRepository,
        String uploadDir
    ) {
        this(
            arbolRepository,
            zonaFollajeRepository,
            analisisRepository,
            analisisCromaticoRepository,
            cuentaDispositivoRepository,
            new com.descortezadores.infrastructure.storage.LocalAlmacenamientoService(uploadDir, "models/")
        );
    }

    public SyncBatchResponse sincronizar(List<SyncItemDTO> items, UUID idCuenta) {
        List<UUID> sincronizados = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        for (SyncItemDTO item : items) {
            try {
                // Validación de autorización de dispositivo si viene especificado (RN-02, RN-27, MSG5)
                if (item.idDispositivo() != null) {
                    var relacion = cuentaDispositivoRepository.findByIdCuentaAndIdDispositivo(idCuenta, item.idDispositivo());
                    if (relacion.isPresent() && "desasociada".equalsIgnoreCase(relacion.get().estadoCuentaDispositivo())) {
                        throw new IllegalStateException(MensajesSistema.MSG05.getCodigo() + ": " + MensajesSistema.MSG05.getDescripcion());
                    }
                }

                String tipoImagen = (item.tipoImagen() != null && !item.tipoImagen().isBlank())
                    ? item.tipoImagen().toLowerCase().trim()
                    : "corteza";

                UUID idArbolFinal = null;
                UUID idZonaFinal = null;

                if ("follaje".equals(tipoImagen)) {
                    // Flujo para capturas de Follaje (Dron / Zona macro)
                    idZonaFinal = item.idZona();
                    if (idZonaFinal == null) {
                        String nombreZona = (item.nombreZonaNuevo() != null && !item.nombreZonaNuevo().isBlank())
                            ? item.nombreZonaNuevo().trim()
                            : "Zona-" + (item.latitud() != null ? String.format("%.4f", item.latitud()) : UUID.randomUUID().toString().substring(0, 8));

                        ZonaFollaje nuevaZona = new ZonaFollaje(
                            UUID.randomUUID(),
                            item.latitud() != null ? item.latitud() : 19.4200,
                            item.longitud() != null ? item.longitud() : -99.1800,
                            Instant.now(),
                            item.observaciones() != null ? item.observaciones() : "Zona registrada en campo durante captura aérea",
                            nombreZona,
                            item.radioMetrosZona() != null ? item.radioMetrosZona() : 50.0,
                            item.idDispositivo(),
                            idCuenta
                        );
                        ZonaFollaje guardada = zonaFollajeRepository.save(nuevaZona);
                        idZonaFinal = guardada.idZona();
                    }
                } else {
                    // Flujo para capturas de Corteza (Árbol individual)
                    tipoImagen = "corteza";
                    idArbolFinal = item.idArbol();

                    if (idArbolFinal == null && item.nombreArbolNuevo() != null && !item.nombreArbolNuevo().isBlank()) {
                        Arbol nuevoArbol = new Arbol(
                            UUID.randomUUID(),
                            item.nombreArbolNuevo().trim(),
                            item.especieArbolNuevo() != null ? item.especieArbolNuevo() : "Fresno (Fraxinus uhdei)",
                            item.longitud(),
                            item.latitud(),
                            Instant.now(),
                            item.observaciones() != null ? item.observaciones() : "Registrado en campo durante sincronización",
                            idCuenta,
                            item.idDispositivo()
                        );
                        Arbol arbolCreado = arbolRepository.save(nuevoArbol);
                        idArbolFinal = arbolCreado.idArbol();
                    }

                    // RN-37: No puede existir una captura de corteza sin árbol asociado
                    if (idArbolFinal == null) {
                        throw new IllegalArgumentException("RN-37: No puede existir una captura de corteza sin árbol asociado ni nombre de árbol nuevo.");
                    }
                }

                // Procesamiento y guardado de imagen con validación RN-07, RN-08 y RN-09
                String rutaEvidencia = "sin_imagen";
                if (item.imagenBase64() != null && !item.imagenBase64().isBlank()) {
                    String b64 = item.imagenBase64().trim();
                    String extension = ".jpg";

                    // Limpieza de prefijo Data URI si viene presente (ej. "data:image/png;base64,...")
                    int commaIndex = b64.indexOf(",");
                    if (commaIndex != -1 && b64.substring(0, commaIndex).contains("base64")) {
                        String header = b64.substring(0, commaIndex).toLowerCase();
                        if (header.contains("image/png")) {
                            extension = ".png";
                        } else if (header.contains("image/webp")) {
                            extension = ".webp";
                        }
                        b64 = b64.substring(commaIndex + 1);
                    }

                    b64 = b64.replaceAll("\\s+", "");
                    byte[] imageBytes = Base64.getDecoder().decode(b64);

                    // Validación RN-09 (resolución) y RN-08 (tamaño)
                    var validacion = ReglasNegocioImagen.validarResolucion(imageBytes);
                    if (!validacion.valida()) {
                        throw new IllegalArgumentException(validacion.mensaje());
                    }

                    String filename = UUID.randomUUID() + extension;
                    String contentType = extension.equals(".png") ? "image/png" : (extension.equals(".webp") ? "image/webp" : "image/jpeg");
                    rutaEvidencia = almacenamientoService.guardarEvidencia(imageBytes, filename, contentType);
                }

                // RN-23 y RN-24: Clasificación en dos categorías únicamente (sano / infestado)
                // Si la confianza es menor a 75% (0.75), el resultado se marca como 'no_determinado'
                String resultado = item.resultadoClasificacion() != null
                    ? item.resultadoClasificacion().toLowerCase().trim()
                    : "sano";

                if (item.confianza() != null && item.confianza() < 0.75) {
                    resultado = "no_determinado";
                } else if (!"sano".equals(resultado) && !"infestado".equals(resultado) && !"no_determinado".equals(resultado)) {
                    resultado = "no_determinado";
                }

                // Guardar análisis garantizando que cumple chk_arbol_o_zona
                Analisis analisis = new Analisis(
                    item.idAnalisisLocal() != null ? item.idAnalisisLocal() : UUID.randomUUID(),
                    rutaEvidencia,
                    item.latitud(),
                    item.longitud(),
                    item.fechaCaptura() != null ? item.fechaCaptura() : Instant.now(),
                    Instant.now(),
                    "sincronizado",
                    tipoImagen,
                    resultado,
                    item.confianza(),
                    item.observaciones(),
                    idArbolFinal,
                    idZonaFinal
                );
                Analisis analisisGuardado = analisisRepository.save(analisis);

                // Guardar análisis cromático asociado (RN-47 y RN-48)
                if (item.pctDanado() != null || item.pctSano() != null || item.pctRegular() != null || item.pctIncierto() != null) {
                    AnalisisCromatico cromatico = AnalisisCromatico.nuevo(
                        item.pctIncierto(),
                        item.pctDanado(),
                        item.pctRegular(),
                        item.pctSano(),
                        analisisGuardado.idAnalisis()
                    );
                    analisisCromaticoRepository.save(cromatico);
                }

                sincronizados.add(item.idAnalisisLocal() != null ? item.idAnalisisLocal() : analisisGuardado.idAnalisis());
            } catch (Exception e) {
                LOG.error("Error sincronizando elemento {}: {}", item.idAnalisisLocal(), e.getMessage());
                errores.add(MensajesSistema.MSG25.getCodigo() + ": " + MensajesSistema.MSG25.getDescripcion() + " (" + e.getMessage() + ")");
            }
        }

        return new SyncBatchResponse(items.size(), sincronizados.size(), sincronizados, errores);
    }
}
