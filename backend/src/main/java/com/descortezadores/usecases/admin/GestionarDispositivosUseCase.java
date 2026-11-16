package com.descortezadores.usecases.admin;

import com.descortezadores.adapters.dto.DispositivoDTO;
import com.descortezadores.domain.entities.CuentaDispositivo;
import com.descortezadores.domain.entities.Dispositivo;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.infrastructure.persistence.CuentaDispositivoRepository;
import com.descortezadores.infrastructure.persistence.DispositivoRepository;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso de Gestión de Dispositivos asociados a cuentas:
 * CU-A06, CU-A07, CU-A08, CU-A09 (§8.8.4)
 */
@Singleton
public class GestionarDispositivosUseCase {

    private final DispositivoRepository dispositivoRepository;
    private final CuentaDispositivoRepository cuentaDispositivoRepository;

    public GestionarDispositivosUseCase(
        DispositivoRepository dispositivoRepository,
        CuentaDispositivoRepository cuentaDispositivoRepository
    ) {
        this.dispositivoRepository = dispositivoRepository;
        this.cuentaDispositivoRepository = cuentaDispositivoRepository;
    }

    public List<Dispositivo> listarTodosDispositivos() {
        return (List<Dispositivo>) dispositivoRepository.findAll();
    }

    public Dispositivo registrarDispositivo(DispositivoDTO dto) {
        Optional<Dispositivo> existente = dispositivoRepository.findByIdentificadorUnico(dto.identificadorUnico());
        if (existente.isPresent()) {
            return existente.get();
        }

        Dispositivo nuevo = new Dispositivo(
            UUID.randomUUID(),
            dto.identificadorUnico(),
            dto.nombreDispositivo() != null ? dto.nombreDispositivo() : "Dispositivo de campo",
            dto.tokenDispositivo(),
            "Activo",
            Instant.now()
        );

        return dispositivoRepository.save(nuevo);
    }

    public CuentaDispositivo asociarDispositivoACuenta(UUID idCuenta, UUID idDispositivo) {
        // Verificar si el dispositivo ya está activamente asociado a otra cuenta (MSG33)
        List<CuentaDispositivo> activas = cuentaDispositivoRepository.findByIdDispositivoAndEstadoCuentaDispositivo(idDispositivo, "Activa");
        if (!activas.isEmpty()) {
            for (CuentaDispositivo cd : activas) {
                if (!cd.idCuenta().equals(idCuenta)) {
                    throw new IllegalStateException(MensajesSistema.MSG33.getCodigo() + ": " + MensajesSistema.MSG33.getDescripcion());
                }
            }
            return activas.get(0);
        }

        CuentaDispositivo nuevaAsociacion = CuentaDispositivo.asociar(idCuenta, idDispositivo);
        return cuentaDispositivoRepository.save(nuevaAsociacion);
    }

    public void desasociarDispositivo(UUID idCuenta, UUID idDispositivo) {
        CuentaDispositivo asociacion = cuentaDispositivoRepository.findByIdCuentaAndIdDispositivo(idCuenta, idDispositivo)
            .orElseThrow(() -> new IllegalArgumentException(MensajesSistema.MSG35.getCodigo() + ": " + MensajesSistema.MSG35.getDescripcion()));

        CuentaDispositivo desasociada = new CuentaDispositivo(
            asociacion.idCuentaDispositivo(),
            asociacion.idCuenta(),
            asociacion.idDispositivo(),
            asociacion.fechaAsociacion(),
            Instant.now(),
            "desasociada"
        );

        cuentaDispositivoRepository.update(desasociada);
    }

    public List<CuentaDispositivo> listarDispositivosPorCuenta(UUID idCuenta) {
        return cuentaDispositivoRepository.findByIdCuenta(idCuenta);
    }
}
