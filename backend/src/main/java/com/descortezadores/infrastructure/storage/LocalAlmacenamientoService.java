package com.descortezadores.infrastructure.storage;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementación local de almacenamiento en disco para entorno de desarrollo,
 * pruebas unitarias/integración y despliegue on-premise (ej. Docker local, Raspberry Pi).
 * Activa por defecto cuando 'aws.s3.enabled' no es 'true'.
 */
@Singleton
@Requires(property = "aws.s3.enabled", notEquals = "true")
public class LocalAlmacenamientoService implements AlmacenamientoService {

    private static final Logger LOG = LoggerFactory.getLogger(LocalAlmacenamientoService.class);

    private final String uploadDir;
    private final String modelsDir;

    public LocalAlmacenamientoService(
            @Value("${app.upload.dir:uploads/evidencias/}") String uploadDir,
            @Value("${app.models.dir:models/}") String modelsDir) {
        this.uploadDir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        String resolvedModelsDir = modelsDir.endsWith("/") ? modelsDir : modelsDir + "/";
        if (!Files.exists(Paths.get(resolvedModelsDir)) && Files.exists(Paths.get("../" + resolvedModelsDir))) {
            resolvedModelsDir = "../" + resolvedModelsDir;
        }
        this.modelsDir = resolvedModelsDir;

        try {
            Files.createDirectories(Paths.get(this.uploadDir));
            Files.createDirectories(Paths.get(this.modelsDir));
        } catch (IOException e) {
            LOG.warn("No se pudieron crear directorios locales: {}", e.getMessage());
        }
    }

    @Override
    public String guardarEvidencia(byte[] contenido, String nombreArchivo, String contentType) {
        try {
            Path targetPath = Paths.get(uploadDir, nombreArchivo).normalize();
            Path baseDir = Paths.get(uploadDir).normalize().toAbsolutePath();
            if (!targetPath.toAbsolutePath().startsWith(baseDir)) {
                throw new IllegalArgumentException("Intento de path traversal no permitido");
            }
            Files.write(targetPath, contenido);
            return "/api/v1/evidencias/" + nombreArchivo;
        } catch (IOException e) {
            LOG.error("Error al escribir archivo local {}: {}", nombreArchivo, e.getMessage());
            throw new RuntimeException("Error al guardar archivo local: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] obtenerEvidencia(String nombreArchivo) {
        try {
            Path baseDir = Paths.get(uploadDir).normalize().toAbsolutePath();
            Path filePath = baseDir.resolve(nombreArchivo).normalize();
            if (!filePath.startsWith(baseDir) || !Files.exists(filePath)) {
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            LOG.error("Error al leer archivo local {}: {}", nombreArchivo, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean existeEvidencia(String nombreArchivo) {
        Path baseDir = Paths.get(uploadDir).normalize().toAbsolutePath();
        Path filePath = baseDir.resolve(nombreArchivo).normalize();
        return filePath.startsWith(baseDir) && Files.exists(filePath);
    }

    @Override
    public String obtenerUrlModelo(String nombreArchivo) {
        return "/api/v1/modelos/descargar/" + nombreArchivo;
    }

    @Override
    public byte[] obtenerModelo(String nombreArchivo) {
        try {
            Path filePath = resolverRutaModelo(nombreArchivo);
            if (!Files.exists(filePath)) {
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            LOG.error("Error al leer modelo local {}: {}", nombreArchivo, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean existeModelo(String nombreArchivo) {
        Path filePath = resolverRutaModelo(nombreArchivo);
        return Files.exists(filePath);
    }

    private Path resolverRutaModelo(String nombreArchivo) {
        Path rutaDirecta = Paths.get(modelsDir).resolve(nombreArchivo).normalize();
        if (Files.exists(rutaDirecta)) {
            return rutaDirecta;
        }
        Path rutaPadre = Paths.get("../").resolve(modelsDir).resolve(nombreArchivo).normalize();
        if (Files.exists(rutaPadre)) {
            return rutaPadre;
        }
        Path rutaRoot = Paths.get("models/").resolve(nombreArchivo).normalize();
        if (Files.exists(rutaRoot)) {
            return rutaRoot;
        }
        return rutaDirecta;
    }
}
