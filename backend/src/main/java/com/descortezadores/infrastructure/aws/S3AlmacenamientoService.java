package com.descortezadores.infrastructure.aws;

import com.descortezadores.infrastructure.storage.AlmacenamientoService;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * Implementación de almacenamiento de evidencias y modelos en Amazon S3.
 *
 * Se activa automáticamente en entornos AWS cuando 'aws.s3.enabled: true'
 * (ej. mediante variable de entorno AWS_S3_ENABLED=true en Lambda o ECS).
 *
 * Utiliza las credenciales del entorno estándar de AWS (IAM Role de Lambda,
 * perfiles de AWS CLI o variables de entorno estándar de AWS).
 */
@Singleton
@Requires(property = "aws.s3.enabled", value = "true")
public class S3AlmacenamientoService implements AlmacenamientoService {

    private static final Logger LOG = LoggerFactory.getLogger(S3AlmacenamientoService.class);

    private final AwsS3Properties properties;
    private final S3Client s3Client;

    public S3AlmacenamientoService(AwsS3Properties properties) {
        this.properties = properties;
        Region awsRegion = Region.of(properties.getRegion() != null ? properties.getRegion() : "us-east-1");
        this.s3Client = S3Client.builder()
                .region(awsRegion)
                .build();
        LOG.info("S3AlmacenamientoService inicializado para el bucket: {} en la región: {}",
                properties.getBucketName(), properties.getRegion());
    }

    /**
     * Constructor con cliente inyectado (para pruebas con S3 Mock / LocalStack)
     */
    public S3AlmacenamientoService(AwsS3Properties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    @Override
    public String guardarEvidencia(byte[] contenido, String nombreArchivo, String contentType) {
        String key = sanitizePrefix(properties.getEvidenciasPrefix()) + nombreArchivo;
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .contentType(contentType != null ? contentType : "image/jpeg")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(contenido));
            LOG.info("Evidencia guardada exitosamente en S3: s3://{}/{}", properties.getBucketName(), key);

            return construirUrlPublica(key);
        } catch (Exception e) {
            LOG.error("Error al subir evidencia a Amazon S3 {}: {}", key, e.getMessage(), e);
            throw new RuntimeException("Error al almacenar evidencia en Amazon S3: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] obtenerEvidencia(String nombreArchivo) {
        String key = sanitizePrefix(properties.getEvidenciasPrefix()) + nombreArchivo;
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(getRequest).asByteArray();
        } catch (NoSuchKeyException e) {
            LOG.warn("Evidencia no encontrada en S3: {}", key);
            return null;
        } catch (Exception e) {
            LOG.error("Error al descargar evidencia de S3 {}: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean existeEvidencia(String nombreArchivo) {
        String key = sanitizePrefix(properties.getEvidenciasPrefix()) + nombreArchivo;
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            LOG.warn("Error al verificar existencia de evidencia en S3 {}: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public String obtenerUrlModelo(String nombreArchivo) {
        String key = sanitizePrefix(properties.getModelosPrefix()) + nombreArchivo;
        return construirUrlPublica(key);
    }

    @Override
    public byte[] obtenerModelo(String nombreArchivo) {
        String key = sanitizePrefix(properties.getModelosPrefix()) + nombreArchivo;
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(getRequest).asByteArray();
        } catch (NoSuchKeyException e) {
            LOG.warn("Modelo no encontrado en S3: {}", key);
            return null;
        } catch (Exception e) {
            LOG.error("Error al descargar modelo de S3 {}: {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean existeModelo(String nombreArchivo) {
        String key = sanitizePrefix(properties.getModelosPrefix()) + nombreArchivo;
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            LOG.warn("Error al verificar existencia de modelo en S3 {}: {}", key, e.getMessage());
            return false;
        }
    }

    private String construirUrlPublica(String key) {
        if (properties.getCdnUrl() != null && !properties.getCdnUrl().isBlank()) {
            String cdn = properties.getCdnUrl().endsWith("/")
                    ? properties.getCdnUrl().substring(0, properties.getCdnUrl().length() - 1)
                    : properties.getCdnUrl();
            return cdn + "/" + key;
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                properties.getBucketName(), properties.getRegion(), key);
    }

    private String sanitizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }
}
