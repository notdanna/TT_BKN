package com.descortezadores.usecases.sync;

import com.descortezadores.adapters.dto.SyncBatchResponse;
import com.descortezadores.adapters.dto.SyncItemDTO;
import com.descortezadores.domain.entities.Analisis;
import com.descortezadores.domain.entities.AnalisisCromatico;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.domain.rules.ReglasNegocioImagen;
import com.descortezadores.infrastructure.persistence.AnalisisCromaticoRepository;
import com.descortezadores.infrastructure.persistence.AnalisisRepository;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
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
 */
@Singleton
public class SyncDeteccionUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyncDeteccionUseCase.class);
    private static final String UPLOAD_DIR = "uploads/evidencias/";

    private final ArbolRepository arbolRepository;
    private final AnalisisRepository analisisRepository;
    private final AnalisisCromaticoRepository analisisCromaticoRepository;

    public SyncDeteccionUseCase(
        ArbolRepository arbolRepository,
        AnalisisRepository analisisRepository,
        AnalisisCromaticoRepository analisisCromaticoRepository
    ) {
        this.arbolRepository = arbolRepository;
        this.analisisRepository = analisisRepository;
        this.analisisCromaticoRepository = analisisCromaticoRepository;

        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (Exception e) {
            LOG.warn("No se pudo crear el directorio de subidas: {}", e.getMessage());
        }
    }

    public SyncBatchResponse sincronizar(List<SyncItemDTO> items, UUID idCuenta) {
        List<UUID> sincronizados = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        for (SyncItemDTO item : items) {
            try {
                UUID idArbolFinal = item.idArbol();

                // RN-37 / RN-38: Crear árbol nuevo si no viene asociado uno existente
                if (idArbolFinal == null && item.nombreArbolNuevo() != null && !item.nombreArbolNuevo().isBlank()) {
                    Arbol nuevoArbol = new Arbol(
                        UUID.randomUUID(),
                        item.nombreArbolNuevo(),
                        item.especieArbolNuevo() != null ? item.especieArbolNuevo() : "Fresno (Fraxinus uhdei)",
                        item.longitud(),
                        item.latitud(),
                        Instant.now(),
                        "Registrado en campo durante sincronización",
                        idCuenta,
                        item.idDispositivo()
                    );
                    Arbol arbolCreado = arbolRepository.save(nuevoArbol);
                    idArbolFinal = arbolCreado.idArbol();
                }

                // Procesamiento y guardado de imagen con validación RN-07, RN-08 y RN-09
                String rutaEvidencia = "sin_imagen";
                if (item.imagenBase64() != null && !item.imagenBase64().isBlank()) {
                    byte[] imageBytes = Base64.getDecoder().decode(item.imagenBase64());

                    // Validación RN-09 (resolución) y RN-08 (tamaño)
                    var validacion = ReglasNegocioImagen.validarResolucion(imageBytes);
                    if (!validacion.valida()) {
                        throw new IllegalArgumentException(validacion.mensaje());
                    }

                    String filename = UUID.randomUUID() + ".jpg";
                    Path targetPath = Paths.get(UPLOAD_DIR, filename);
                    Files.write(targetPath, imageBytes);
                    rutaEvidencia = "/api/v1/evidencias/" + filename;
                }

                // Guardar análisis (corteza o follaje)
                Analisis analisis = new Analisis(
                    item.idAnalisisLocal() != null ? item.idAnalisisLocal() : UUID.randomUUID(),
                    rutaEvidencia,
                    item.latitud(),
                    item.longitud(),
                    item.fechaCaptura() != null ? item.fechaCaptura() : Instant.now(),
                    Instant.now(),
                    "sincronizado",
                    item.tipoImagen() != null ? item.tipoImagen() : "corteza",
                    item.resultadoClasificacion() != null ? item.resultadoClasificacion() : "sano",
                    item.confianza(),
                    item.observaciones(),
                    idArbolFinal,
                    null
                );
                Analisis analisisGuardado = analisisRepository.save(analisis);

                // Guardar análisis cromático asociado
                if (item.pctDanado() != null || item.pctSano() != null || item.pctRegular() != null) {
                    AnalisisCromatico cromatico = AnalisisCromatico.nuevo(
                        item.pctIncierto(),
                        item.pctDanado(),
                        item.pctRegular(),
                        item.pctSano(),
                        analisisGuardado.idAnalisis()
                    );
                    analisisCromaticoRepository.save(cromatico);
                }

                sincronizados.add(item.idAnalisisLocal());
            } catch (Exception e) {
                LOG.error("Error sincronizando elemento {}: {}", item.idAnalisisLocal(), e.getMessage());
                errores.add(MensajesSistema.MSG25.getCodigo() + ": " + MensajesSistema.MSG25.getDescripcion() + " (" + e.getMessage() + ")");
            }
        }

        return new SyncBatchResponse(items.size(), sincronizados.size(), sincronizados, errores);
    }
}
