package com.descortezadores.adapters.controllers;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Controller("/api/v1/evidencias")
public class EvidenciaController {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenciaController.class);
    private final com.descortezadores.infrastructure.storage.AlmacenamientoService almacenamientoService;

    public EvidenciaController(com.descortezadores.infrastructure.storage.AlmacenamientoService almacenamientoService) {
        this.almacenamientoService = almacenamientoService;
    }

    /**
     * Endpoint público para visualizar o descargar la evidencia fotográfica
     */
    @Get("/{filename}")
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Produces({MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.ALL})
    public HttpResponse<byte[]> obtenerEvidencia(@PathVariable String filename) {
        try {
            byte[] bytes = almacenamientoService.obtenerEvidencia(filename);
            if (bytes == null) {
                return HttpResponse.notFound();
            }

            String contentType = filename.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
            return HttpResponse.ok(bytes).header("Content-Type", contentType);
        } catch (Exception e) {
            LOG.error("Error al servir imagen {}: {}", filename, e.getMessage());
            return HttpResponse.serverError();
        }
    }

    /**
     * Subida directa de imagen individual vía multipart/form-data
     */
    @Post(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Map<String, String>> subirImagen(@Part("file") CompletedFileUpload file) {
        try {
            String extension = ".jpg";
            String contentType = "image/jpeg";
            if (file.getFilename() != null && file.getFilename().toLowerCase().endsWith(".png")) {
                extension = ".png";
                contentType = "image/png";
            }
            String filename = UUID.randomUUID() + extension;
            byte[] bytes = file.getBytes();

            String url = almacenamientoService.guardarEvidencia(bytes, filename, contentType);
            return HttpResponse.ok(Map.of(
                "filename", filename,
                "url", url,
                "sizeBytes", String.valueOf(bytes.length)
            ));
        } catch (Exception e) {
            LOG.error("Error al subir archivo: {}", e.getMessage());
            return HttpResponse.serverError();
        }
    }
}
