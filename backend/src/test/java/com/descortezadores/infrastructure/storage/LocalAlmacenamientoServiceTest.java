package com.descortezadores.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Almacenamiento Local (LocalAlmacenamientoService)")
class LocalAlmacenamientoServiceTest {

    @TempDir
    Path tempUploadDir;

    @TempDir
    Path tempModelsDir;

    private LocalAlmacenamientoService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalAlmacenamientoService(
                tempUploadDir.toString(),
                tempModelsDir.toString()
        );
    }

    @Test
    @DisplayName("Guardar y recuperar evidencia fotográfica en almacenamiento local")
    void testGuardarYRecuperarEvidencia() {
        byte[] content = "imagen_fake_bytes".getBytes();
        String filename = "evidencia_test.jpg";

        String url = storageService.guardarEvidencia(content, filename, "image/jpeg");
        assertNotNull(url);
        assertEquals("/api/v1/evidencias/" + filename, url);

        assertTrue(storageService.existeEvidencia(filename));
        byte[] retrieved = storageService.obtenerEvidencia(filename);
        assertNotNull(retrieved);
        assertArrayEquals(content, retrieved);
    }

    @Test
    @DisplayName("Protección contra Path Traversal en guardado y recuperación")
    void testProteccionPathTraversal() {
        byte[] content = "intruso".getBytes();
        assertThrows(IllegalArgumentException.class, () -> {
            storageService.guardarEvidencia(content, "../malicioso.jpg", "image/jpeg");
        });

        assertNull(storageService.obtenerEvidencia("../otro_archivo.txt"));
        assertFalse(storageService.existeEvidencia("../otro_archivo.txt"));
    }

    @Test
    @DisplayName("Recuperación y verificación de modelo TFLite")
    void testModeloLocal() throws IOException {
        String modelName = "light_classifier.tflite";
        byte[] modelBytes = new byte[]{0x10, 0x20, 0x30, 0x40};
        Files.write(tempModelsDir.resolve(modelName), modelBytes);

        assertTrue(storageService.existeModelo(modelName));
        assertEquals("/api/v1/modelos/descargar/" + modelName, storageService.obtenerUrlModelo(modelName));

        byte[] retrieved = storageService.obtenerModelo(modelName);
        assertNotNull(retrieved);
        assertArrayEquals(modelBytes, retrieved);
    }

    @Test
    @DisplayName("Archivo inexistente retorna null y false")
    void testArchivosInexistentes() {
        assertNull(storageService.obtenerEvidencia("no_existe.jpg"));
        assertFalse(storageService.existeEvidencia("no_existe.jpg"));
        assertNull(storageService.obtenerModelo("no_existe.tflite"));
        assertFalse(storageService.existeModelo("no_existe.tflite"));
    }
}
