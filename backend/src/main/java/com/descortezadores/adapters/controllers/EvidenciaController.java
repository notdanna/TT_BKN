package com.descortezadores.adapters.controllers;

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
    private static final String UPLOAD_DIR = "uploads/evidencias/";

    public EvidenciaController() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (Exception e) {
            LOG.warn("No se pudo crear el directorio de evidencias: {}", e.getMessage());
        }
    }

    /**
     * Endpoint público para visualizar o descargar la evidencia fotográfica
     */
    @Get("/{filename}")
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Produces({MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.ALL})
    public HttpResponse<byte[]> obtenerEvidencia(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR, filename).normalize();
            if (!Files.exists(filePath)) {
                return HttpResponse.notFound();
            }
            byte[] bytes = Files.readAllBytes(filePath);
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
            if (file.getFilename().toLowerCase().endsWith(".png")) {
                extension = ".png";
            }
            String filename = UUID.randomUUID() + extension;
            Path targetPath = Paths.get(UPLOAD_DIR, filename);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = "/api/v1/evidencias/" + filename;
            return HttpResponse.ok(Map.of(
                "filename", filename,
                "url", url,
                "sizeBytes", String.valueOf(file.getSize())
            ));
        } catch (Exception e) {
            LOG.error("Error al subir archivo: {}", e.getMessage());
            return HttpResponse.serverError();
        }
    }
}
