package com.descortezadores.infrastructure.email;

import io.micronaut.context.annotation.Secondary;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementación de respaldo para desarrollo local/pruebas cuando Amazon SES está deshabilitado.
 */
@Singleton
@Secondary
public class LogCorreoRecuperacionService implements CorreoRecuperacionService {

    private static final Logger LOG = LoggerFactory.getLogger(LogCorreoRecuperacionService.class);

    @Override
    public void enviarCodigoRecuperacion(String correoDestino, String nombreUsuario, String codigo, int vigenciaHoras) {
        LOG.info(
            "[DEV-EMAIL] Correo de recuperación para {} ({}): código={} (vigencia {}h)",
            correoDestino,
            nombreUsuario,
            codigo,
            vigenciaHoras
        );
    }
}
