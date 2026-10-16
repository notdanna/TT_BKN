package com.descortezadores.adapters.controllers;

import com.descortezadores.adapters.dto.ModeloDTO;
import com.descortezadores.infrastructure.storage.AlmacenamientoService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Distribución de Modelos TFLite (ModeloController - PT-15, PI-07, PI-08)")
class ModeloControllerTest {

    private ModeloController controller;
    private MockAlmacenamientoService mockAlmacenamiento;

    @BeforeEach
    void setUp() {
        mockAlmacenamiento = new MockAlmacenamientoService();
        controller = new ModeloController(mockAlmacenamiento, "1.0.0", "light_classifier.tflite");
    }

    @Test
    @DisplayName("PT-15 / PI-07: Consulta de metadatos de la última versión del modelo TFLite")
    void testConsultarUltimaVersion() {
        byte[] modelBytes = "fake_tflite_model_weights".getBytes();
        mockAlmacenamiento.archivosModelo.put("light_classifier.tflite", modelBytes);

        HttpResponse<ModeloDTO> response = controller.obtenerUltimaVersion();
        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());

        ModeloDTO dto = response.body();
        assertEquals("1.0.0", dto.version());
        assertEquals("light_classifier.tflite", dto.nombreArchivo());
        assertEquals(modelBytes.length, dto.tamanoBytes());
        assertNotNull(dto.sha256());
        assertFalse(dto.sha256().isBlank());
        assertEquals("/api/v1/modelos/descargar/light_classifier.tflite", dto.urlDescarga());
    }

    @Test
    @DisplayName("PI-08: Descarga directa de modelo local como octet-stream")
    void testDescargaModeloLocal() {
        byte[] modelBytes = "modelo_binario_bytes".getBytes();
        mockAlmacenamiento.archivosModelo.put("light_classifier.tflite", modelBytes);

        HttpResponse<?> response = controller.descargarModelo("light_classifier.tflite");
        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertArrayEquals(modelBytes, (byte[]) response.body());
    }

    @Test
    @DisplayName("PT-15: Redirección 302 a Amazon S3 cuando la URL de descarga es remota")
    void testRedireccionS3() {
        mockAlmacenamiento.urlRemota = "https://descortezadores-evidencias-2026.s3.us-east-1.amazonaws.com/modelos/light_classifier.tflite";

        HttpResponse<?> response = controller.descargarModelo("light_classifier.tflite");
        assertTrue(response.getStatus().getCode() >= 300 && response.getStatus().getCode() < 400,
                "Debe responder un código de redirección 3xx");
        assertEquals(mockAlmacenamiento.urlRemota, response.getHeaders().get(io.micronaut.http.HttpHeaders.LOCATION));
    }

    @Test
    @DisplayName("Descarga de modelo inexistente retorna 404 Not Found")
    void testDescargaModeloInexistente() {
        HttpResponse<?> response = controller.descargarModelo("inexistente.tflite");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    private static class MockAlmacenamientoService implements AlmacenamientoService {
        Map<String, byte[]> archivosModelo = new HashMap<>();
        String urlRemota = null;

        @Override
        public String guardarEvidencia(byte[] contenido, String nombreArchivo, String contentType) {
            return "/api/v1/evidencias/" + nombreArchivo;
        }

        @Override
        public byte[] obtenerEvidencia(String nombreArchivo) {
            return null;
        }

        @Override
        public boolean existeEvidencia(String nombreArchivo) {
            return false;
        }

        @Override
        public String obtenerUrlModelo(String nombreArchivo) {
            if (urlRemota != null) {
                return urlRemota;
            }
            return "/api/v1/modelos/descargar/" + nombreArchivo;
        }

        @Override
        public byte[] obtenerModelo(String nombreArchivo) {
            return archivosModelo.get(nombreArchivo);
        }

        @Override
        public boolean existeModelo(String nombreArchivo) {
            return archivosModelo.containsKey(nombreArchivo);
        }
    }
}
