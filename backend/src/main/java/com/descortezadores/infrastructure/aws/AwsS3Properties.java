package com.descortezadores.infrastructure.aws;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Propiedades de configuración para Amazon S3 y AWS.
 * Mapeadas desde 'aws.s3' en application.yml o variables de entorno.
 */
@ConfigurationProperties("aws.s3")
public class AwsS3Properties {

    private boolean enabled = false;
    private String region = "us-east-1";
    private String bucketName = "descortezadores-evidencias-2026";
    private String evidenciasPrefix = "evidencias/";
    private String modelosPrefix = "modelos/";
    private String cdnUrl = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getEvidenciasPrefix() {
        return evidenciasPrefix;
    }

    public void setEvidenciasPrefix(String evidenciasPrefix) {
        this.evidenciasPrefix = evidenciasPrefix;
    }

    public String getModelosPrefix() {
        return modelosPrefix;
    }

    public void setModelosPrefix(String modelosPrefix) {
        this.modelosPrefix = modelosPrefix;
    }

    public String getCdnUrl() {
        return cdnUrl;
    }

    public void setCdnUrl(String cdnUrl) {
        this.cdnUrl = cdnUrl;
    }
}
