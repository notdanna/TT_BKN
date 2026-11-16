package com.descortezadores.usecases.admin;

import com.descortezadores.adapters.dto.UsuarioDTO;
import com.descortezadores.domain.entities.Cuenta;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.infrastructure.persistence.CuentaRepository;
import jakarta.inject.Singleton;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso de Administración de Cuentas:
 * CU-A01, CU-A02, CU-A03, CU-A04, CU-A05 (§8.8.4)
 */
@Singleton
public class GestionarCuentasUseCase {

    private final CuentaRepository cuentaRepository;

    public GestionarCuentasUseCase(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    public List<Cuenta> listarCuentas() {
        return (List<Cuenta>) cuentaRepository.findAll();
    }

    public Optional<Cuenta> obtenerPorId(UUID idCuenta) {
        return cuentaRepository.findById(idCuenta);
    }

    public Cuenta crearCuenta(UsuarioDTO dto) {
        if (cuentaRepository.findByCorreo(dto.correo()).isPresent()) {
            throw new IllegalArgumentException(MensajesSistema.MSG31.getCodigo() + ": " + MensajesSistema.MSG31.getDescripcion());
        }

        String rawPassword = (dto.contrasena() != null && !dto.contrasena().isBlank())
            ? dto.contrasena()
            : "Password123!";

        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));

        Cuenta nueva = new Cuenta(
            UUID.randomUUID(),
            dto.correo(),
            hash,
            dto.rol() != null ? dto.rol() : "Experto de campo",
            "Activa",
            Instant.now(),
            dto.nombreCompleto()
        );

        return cuentaRepository.save(nueva);
    }

    public Cuenta actualizarCuenta(UUID idCuenta, UsuarioDTO dto) {
        Cuenta existente = cuentaRepository.findById(idCuenta)
            .orElseThrow(() -> new IllegalArgumentException(MensajesSistema.MSG32.getCodigo() + ": " + MensajesSistema.MSG32.getDescripcion()));

        String nuevoHash = existente.contrasenaHash();
        if (dto.contrasena() != null && !dto.contrasena().isBlank()) {
            nuevoHash = BCrypt.hashpw(dto.contrasena(), BCrypt.gensalt(10));
        }

        Cuenta actualizada = new Cuenta(
            existente.idCuenta(),
            dto.correo() != null ? dto.correo() : existente.correo(),
            nuevoHash,
            dto.rol() != null ? dto.rol() : existente.rol(),
            dto.estadoCuenta() != null ? dto.estadoCuenta() : existente.estadoCuenta(),
            existente.fechaCreacion(),
            dto.nombreCompleto() != null ? dto.nombreCompleto() : existente.nombreCompleto()
        );

        return cuentaRepository.update(actualizada);
    }

    public Cuenta cambiarEstado(UUID idCuenta, String nuevoEstado) {
        Cuenta existente = cuentaRepository.findById(idCuenta)
            .orElseThrow(() -> new IllegalArgumentException(MensajesSistema.MSG32.getCodigo() + ": " + MensajesSistema.MSG32.getDescripcion()));

        Cuenta actualizada = new Cuenta(
            existente.idCuenta(),
            existente.correo(),
            existente.contrasenaHash(),
            existente.rol(),
            nuevoEstado,
            existente.fechaCreacion(),
            existente.nombreCompleto()
        );

        return cuentaRepository.update(actualizada);
    }
}
