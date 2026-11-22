package com.descortezadores.usecases.sync;

import com.descortezadores.domain.entities.CuentaDispositivo;
import com.descortezadores.domain.entities.Dispositivo;
import com.descortezadores.infrastructure.persistence.CuentaDispositivoRepository;
import com.descortezadores.infrastructure.persistence.DispositivoRepository;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

/**
 * Garantiza que un {@code idDispositivo} recibido desde la app móvil exista en {@code dispositivo}
 * antes de asignarlo como llave foránea en árboles, zonas o análisis, evitando errores en sincronizaciones
 * realizadas tras operar en sesión offline.
 */
@Singleton
public class AseguradorDispositivo {

    private static final Logger LOG = LoggerFactory.getLogger(AseguradorDispositivo.class);

    private final DispositivoRepository dispositivoRepository;
    private final CuentaDispositivoRepository cuentaDispositivoRepository;

    public AseguradorDispositivo(
        DispositivoRepository dispositivoRepository,
        CuentaDispositivoRepository cuentaDispositivoRepository
    ) {
        this.dispositivoRepository = dispositivoRepository;
        this.cuentaDispositivoRepository = cuentaDispositivoRepository;
    }

    @Nullable
    public UUID asegurar(@Nullable UUID idDispositivo, @Nullable UUID idCuenta, @Nullable String nombreOpcional) {
        if (idDispositivo == null) {
            return null;
        }
        try {
            if (dispositivoRepository.findById(idDispositivo).isEmpty()) {
                String nombre = (nombreOpcional != null && !nombreOpcional.isBlank())
                    ? nombreOpcional.trim()
                    : "Dispositivo Móvil";
                dispositivoRepository.save(new Dispositivo(
                    idDispositivo,
                    idDispositivo.toString(),
                    nombre,
                    null,
                    "Activo",
                    Instant.now()
                ));
                LOG.info("Dispositivo {} registrado automáticamente durante sincronización", idDispositivo);
            }
            if (idCuenta != null
                && cuentaDispositivoRepository.findByIdCuentaAndIdDispositivo(idCuenta, idDispositivo).isEmpty()) {
                cuentaDispositivoRepository.save(CuentaDispositivo.asociar(idCuenta, idDispositivo));
            }
            return idDispositivo;
        } catch (Exception ex) {
            if (dispositivoRepository.findById(idDispositivo).isPresent()) {
                return idDispositivo;
            }
            LOG.warn("No se pudo asegurar dispositivo {}: {}", idDispositivo, ex.getMessage());
            return null;
        }
    }
}
