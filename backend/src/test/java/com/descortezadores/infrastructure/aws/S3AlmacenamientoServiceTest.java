package com.descortezadores.infrastructure.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Almacenamiento en Amazon S3 (S3AlmacenamientoService)")
class S3AlmacenamientoServiceTest {

    private AwsS3Properties properties;
    private S3Client s3ClientMock;
    private S3AlmacenamientoService service;
    private Map<String, byte[]> s3Storage;

    @BeforeEach
    void setUp() {
        s3Storage = new HashMap<>();
        properties = new AwsS3Properties();
        properties.setEnabled(true);
        properties.setRegion("us-east-1");
        properties.setBucketName("test-bucket-descortezadores");
        properties.setEvidenciasPrefix("evidencias/");
        properties.setModelosPrefix("modelos/");

        s3ClientMock = (S3Client) Proxy.newProxyInstance(
                S3Client.class.getClassLoader(),
                new Class<?>[]{S3Client.class},
                (proxy, method, args) -> {
                    if ("putObject".equals(method.getName())) {
                        PutObjectRequest req = (PutObjectRequest) args[0];
                        RequestBody body = (RequestBody) args[1];
                        byte[] bytes = body.contentStreamProvider().newStream().readAllBytes();
                        s3Storage.put(req.key(), bytes);
                        return PutObjectResponse.builder().build();
                    }
                    if ("getObjectAsBytes".equals(method.getName())) {
                        GetObjectRequest req = (GetObjectRequest) args[0];
                        byte[] bytes = s3Storage.get(req.key());
                        if (bytes == null) {
                            throw NoSuchKeyException.builder().message("Key not found").build();
                        }
                        GetObjectResponse getResp = GetObjectResponse.builder().contentLength((long) bytes.length).build();
                        return ResponseBytes.fromByteArray(getResp, bytes);
                    }
                    if ("headObject".equals(method.getName())) {
                        HeadObjectRequest req = (HeadObjectRequest) args[0];
                        if (!s3Storage.containsKey(req.key())) {
                            throw NoSuchKeyException.builder().message("Key not found").build();
                        }
                        return HeadObjectResponse.builder().build();
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    return null;
                }
        );

        service = new S3AlmacenamientoService(properties, s3ClientMock);
    }

    @Test
    @DisplayName("Subida y consulta de evidencia fotográfica a bucket S3")
    void testGuardarYObtenerEvidenciaS3() {
        byte[] imageBytes = new byte[]{0x01, 0x02, 0x03};
        String filename = "arbol_corteza_01.jpg";

        String url = service.guardarEvidencia(imageBytes, filename, "image/jpeg");
        assertNotNull(url);
        assertEquals("https://test-bucket-descortezadores.s3.us-east-1.amazonaws.com/evidencias/arbol_corteza_01.jpg", url);

        assertTrue(service.existeEvidencia(filename));
        byte[] retrieved = service.obtenerEvidencia(filename);
        assertNotNull(retrieved);
        assertArrayEquals(imageBytes, retrieved);
    }

    @Test
    @DisplayName("Generación de URL con CloudFront CDN cuando está configurado")
    void testUrlConCloudFront() {
        properties.setCdnUrl("https://d123456abcdef8.cloudfront.net");

        String url = service.guardarEvidencia(new byte[]{1}, "evidencia.jpg", "image/jpeg");
        assertEquals("https://d123456abcdef8.cloudfront.net/evidencias/evidencia.jpg", url);

        String urlModelo = service.obtenerUrlModelo("light_classifier.tflite");
        assertEquals("https://d123456abcdef8.cloudfront.net/modelos/light_classifier.tflite", urlModelo);
    }

    @Test
    @DisplayName("Consulta de modelo y verificación de existencia en S3")
    void testModeloEnS3() {
        String modelName = "light_classifier.tflite";
        byte[] modelBytes = new byte[]{0x7f, 0x45, 0x4c, 0x46};
        s3Storage.put("modelos/" + modelName, modelBytes);

        assertTrue(service.existeModelo(modelName));
        assertArrayEquals(modelBytes, service.obtenerModelo(modelName));

        assertFalse(service.existeModelo("no_existe.tflite"));
        assertNull(service.obtenerModelo("no_existe.tflite"));
    }
}
