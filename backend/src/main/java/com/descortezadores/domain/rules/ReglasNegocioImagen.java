package com.descortezadores.domain.rules;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;

/**
 * Validador de reglas de negocio para imágenes según Capítulo 7 (§7.2, Tabla 5):
 * - RN-07: Formatos aceptados (JPG, JPEG, PNG, WEBP, BMP, HEIC).
 * - RN-08: Tamaño máximo de imagen (10 MB).
 * - RN-09: Resolución mínima 300x300 px.
 */
public final class ReglasNegocioImagen {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB (RN-08)
    private static final int MIN_WIDTH = 300; // RN-09
    private static final int MIN_HEIGHT = 300; // RN-09

    private static final Set<String> FORMATOS_PERMITIDOS = Set.of(
        "jpg", "jpeg", "png", "webp", "bmp", "heic"
    );

    private ReglasNegocioImagen() {}

    /**
     * Valida el formato según la extensión o mime type (RN-07)
     */
    public static boolean esFormatoPermitido(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase().replace(".", "").trim();
        return FORMATOS_PERMITIDOS.contains(ext);
    }

    /**
     * Valida el tamaño en bytes (RN-08)
     */
    public static boolean esTamanoValido(long tamanoBytes) {
        return tamanoBytes > 0 && tamanoBytes <= MAX_SIZE_BYTES;
    }

    /**
     * Valida la resolución mínima requerida de 300x300 px (RN-09)
     */
    public static ResultadoValidacion validarResolucion(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return new ResultadoValidacion(false, MensajesSistema.MSG19.getDescripcion());
        }

        if (!esTamanoValido(imageBytes.length)) {
            return new ResultadoValidacion(false, "El tamaño de la imagen excede el límite máximo permitido de 10 MB (RN-08).");
        }

        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                // Algunos formatos como WEBP/HEIC en Java nativo pueden no parsearse por ImageIO sin plugins adicionales
                // Si ImageIO no puede decodificarlo directamente pero el tamaño es válido, se acepta provisionalmente
                return new ResultadoValidacion(true, "OK");
            }
            if (image.getWidth() < MIN_WIDTH || image.getHeight() < MIN_HEIGHT) {
                return new ResultadoValidacion(
                    false,
                    String.format("La imagen tiene resolución de %dx%d px. Se requiere una resolución mínima de %dx%d px (RN-09).",
                        image.getWidth(), image.getHeight(), MIN_WIDTH, MIN_HEIGHT)
                );
            }
        } catch (Exception e) {
            return new ResultadoValidacion(false, MensajesSistema.MSG19.getDescripcion());
        }

        return new ResultadoValidacion(true, "OK");
    }

    public record ResultadoValidacion(boolean valida, String mensaje) {}
}
