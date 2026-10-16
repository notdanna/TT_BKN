package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.ModeloDTO;
import com.descortezadores.infrastructure.storage.AlmacenamientoService;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Controlador para la distribución y actualización del modelo TensorFlow Lite.
 * Satisface los puntos de prueba del TT:
 * - PT-15: Descarga de nueva versión del modelo desde Amazon S3 al detectar actualización.
 * - PI-07: Publicación y accesibilidad del modelo .tflite.
 * - PI-08: Consumo y validación por la app móvil.
 */
@Controller("/api/v1/modelos")
@Secured(SecurityRule.IS_ANONYMOUS)
public class ModeloController {

    private static final Logger LOG = LoggerFactory.getLogger(ModeloController.class);

    private final AlmacenamientoService almacenamientoService;
    private final String modelVersion;
    private final String defaultModelName;

    public ModeloController(
            AlmacenamientoService almacenamientoService,
            @Value("${app.models.latest-version:1.0.0}") String modelVersion,
            @Value("${app.models.default-filename:light_classifier.tflite}") String defaultModelName) {
        this.almacenamientoService = almacenamientoService;
        this.modelVersion = modelVersion;
        this.defaultModelName = defaultModelName;
    }

    /**
     * Consulta los metadatos de la versión más reciente del modelo clasificador TFLite.
     */
    @Get("/latest")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<ModeloDTO> obtenerUltimaVersion() {
        String urlDescarga = almacenamientoService.obtenerUrlModelo(defaultModelName);
        byte[] bytes = almacenamientoService.obtenerModelo(defaultModelName);

        long tamano = bytes != null ? bytes.length : 0L;
        String sha256 = bytes != null ? calcularSha256(bytes) : "unknown";

        ModeloDTO dto = new ModeloDTO(
                modelVersion,
                defaultModelName,
                tamano,
                sha256,
                urlDescarga,
                Instant.parse("2026-05-01T00:00:00Z")
        );
        return HttpResponse.ok(dto);
    }

    /**
     * Descarga el archivo binario del modelo (.tflite o .keras).
     * Si el almacenamiento está configurado en S3 y retorna una URL remota, redirige (302) a S3.
     */
    @Get("/descargar/{nombreArchivo}")
    public HttpResponse<?> descargarModelo(@PathVariable String nombreArchivo) {
        String url = almacenamientoService.obtenerUrlModelo(nombreArchivo);

        // Si la URL es externa (Amazon S3 o CloudFront), redirigir directamente
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            return HttpResponse.redirect(URI.create(url));
        }

        // Descarga directa desde almacenamiento local
        byte[] bytes = almacenamientoService.obtenerModelo(nombreArchivo);
        if (bytes == null) {
            return HttpResponse.notFound();
        }

        return HttpResponse.ok(bytes)
                .header("Content-Type", MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");
    }

    private String calcularSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            LOG.warn("No se pudo calcular SHA-256 del modelo: {}", e.getMessage());
            return "error";
        }
    }
}
