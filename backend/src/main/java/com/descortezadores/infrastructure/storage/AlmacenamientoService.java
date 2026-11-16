package com.descortezadores.infrastructure.storage;

/**
 * Puerto de abstracción de almacenamiento (Clean Architecture / Inversión de Dependencias).
 * Desacopla los casos de uso (sincronización, consulta de evidencia y distribución de modelos)
 * del medio físico subyacente (Sistema de archivos local vs. Amazon S3).
 *
 * Cumple con los requerimientos del TT:
 * - Almacenamiento de evidencias fotográficas (RN-18, chk_arbol_o_zona).
 * - Publicación y descarga de versiones del modelo TFLite (PT-15, PI-07, PI-08).
 */
public interface AlmacenamientoService {

    /**
     * Guarda una evidencia fotográfica y retorna la URL o ruta permanente para la base de datos.
     * En local: /api/v1/evidencias/{nombreArchivo}
     * En AWS S3: https://{bucket}.s3.{region}.amazonaws.com/evidencias/{nombreArchivo} (o URL CloudFront)
     *
     * @param contenido Bytes de la imagen
     * @param nombreArchivo Nombre único del archivo (ej. UUID.jpg)
     * @param contentType Tipo MIME (image/jpeg, image/png, image/webp)
     * @return Ruta local relativa o URL pública/S3 permanente
     */
    String guardarEvidencia(byte[] contenido, String nombreArchivo, String contentType);

    /**
     * Obtiene el contenido binario de una evidencia fotográfica.
     *
     * @param nombreArchivo Nombre del archivo
     * @return Bytes del archivo
     */
    byte[] obtenerEvidencia(String nombreArchivo);

    /**
     * Verifica si una evidencia existe en el almacenamiento.
     *
     * @param nombreArchivo Nombre del archivo
     * @return true si existe, false en caso contrario
     */
    boolean existeEvidencia(String nombreArchivo);

    /**
     * Obtiene la URL pública o de descarga para el modelo de clasificación TFLite.
     *
     * @param nombreArchivo Nombre del archivo de modelo (ej. light_classifier.tflite)
     * @return URL de descarga (endpoint local o URL directa a S3/CDN)
     */
    String obtenerUrlModelo(String nombreArchivo);

    /**
     * Obtiene los bytes del modelo TFLite.
     *
     * @param nombreArchivo Nombre del archivo de modelo
     * @return Bytes del modelo
     */
    byte[] obtenerModelo(String nombreArchivo);

    /**
     * Verifica si el archivo de modelo existe en el almacenamiento.
     *
     * @param nombreArchivo Nombre del archivo de modelo
     * @return true si existe
     */
    boolean existeModelo(String nombreArchivo);
}
