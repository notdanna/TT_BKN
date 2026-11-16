package com.descortezadores.domain.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Reglas de Negocio de Imagen (RN-07, RN-08, RN-09)")
class ReglasNegocioImagenTest {

    @Test
    @DisplayName("RN-07: Formatos válidos son aceptados (jpg, jpeg, png, webp, bmp, heic)")
    void testFormatosValidos() {
        assertTrue(ReglasNegocioImagen.esFormatoPermitido("jpg"));
        assertTrue(ReglasNegocioImagen.esFormatoPermitido("JPEG"));
        assertTrue(ReglasNegocioImagen.esFormatoPermitido(".png"));
        assertTrue(ReglasNegocioImagen.esFormatoPermitido("webp"));
        assertTrue(ReglasNegocioImagen.esFormatoPermitido("BMP"));
        assertTrue(ReglasNegocioImagen.esFormatoPermitido(".heic"));
    }

    @Test
    @DisplayName("RN-07: Formatos no permitidos son rechazados (gif, pdf, tiff, exe)")
    void testFormatosInvalidos() {
        assertFalse(ReglasNegocioImagen.esFormatoPermitido("gif"));
        assertFalse(ReglasNegocioImagen.esFormatoPermitido("pdf"));
        assertFalse(ReglasNegocioImagen.esFormatoPermitido("tiff"));
        assertFalse(ReglasNegocioImagen.esFormatoPermitido("exe"));
        assertFalse(ReglasNegocioImagen.esFormatoPermitido(null));
        assertFalse(ReglasNegocioImagen.esFormatoPermitido(""));
    }

    @Test
    @DisplayName("RN-08: Tamaño máximo permitido es de 10 MB")
    void testTamanoValido() {
        assertTrue(ReglasNegocioImagen.esTamanoValido(1024)); // 1 KB
        assertTrue(ReglasNegocioImagen.esTamanoValido(5 * 1024 * 1024)); // 5 MB
        assertTrue(ReglasNegocioImagen.esTamanoValido(10 * 1024 * 1024)); // 10 MB exactos

        assertFalse(ReglasNegocioImagen.esTamanoValido(0));
        assertFalse(ReglasNegocioImagen.esTamanoValido(-100));
        assertFalse(ReglasNegocioImagen.esTamanoValido(10 * 1024 * 1024 + 1)); // 10 MB + 1 byte
        assertFalse(ReglasNegocioImagen.esTamanoValido(20 * 1024 * 1024)); // 20 MB
    }

    @Test
    @DisplayName("RN-09: Imagen con resolución >= 300x300 px es aceptada")
    void testResolucionValida() throws IOException {
        byte[] imagenValida = generarImagen(300, 300, "jpg");
        var resultado = ReglasNegocioImagen.validarResolucion(imagenValida);
        assertTrue(resultado.valida(), "Imagen 300x300 debe ser válida");

        byte[] imagenMayor = generarImagen(1024, 768, "png");
        var resultadoMayor = ReglasNegocioImagen.validarResolucion(imagenMayor);
        assertTrue(resultadoMayor.valida(), "Imagen 1024x768 debe ser válida");
    }

    @Test
    @DisplayName("RN-09: Imagen con resolución < 300x300 px es rechazada con mensaje informativo")
    void testResolucionInvalida() throws IOException {
        byte[] imagenPequena = generarImagen(200, 200, "jpg");
        var resultado = ReglasNegocioImagen.validarResolucion(imagenPequena);
        assertFalse(resultado.valida(), "Imagen 200x200 debe ser rechazada");
        assertTrue(resultado.mensaje().contains("RN-09"));
        assertTrue(resultado.mensaje().contains("200x200"));
    }

    @Test
    @DisplayName("Validación con array nulo o vacío debe fallar")
    void testBytesVacios() {
        var resNulo = ReglasNegocioImagen.validarResolucion(null);
        assertFalse(resNulo.valida());

        var resVacio = ReglasNegocioImagen.validarResolucion(new byte[0]);
        assertFalse(resVacio.valida());
    }

    private byte[] generarImagen(int ancho, int alto, String formato) throws IOException {
        BufferedImage image = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, formato, baos);
        return baos.toByteArray();
    }
}
