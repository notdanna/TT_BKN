package com.descortezadores.usecases.auth;

import com.descortezadores.domain.entities.Cuenta;
import com.descortezadores.domain.entities.TokenRecuperacion;
import com.descortezadores.domain.rules.MensajesSistema;
import com.descortezadores.infrastructure.persistence.CuentaRepository;
import jakarta.inject.Singleton;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Caso de Uso: Recuperación y Restablecimiento de Contraseñas (CU-G02, CU-G03).
 * Implementa las reglas de negocio:
 * - RN-04: Vigencia máxima de 24 horas para el enlace de recuperación.
 * - RN-05: Enlace de un solo uso; invalidado automáticamente tras su utilización.
 * - RN-06: Máximo 3 solicitudes por usuario en un periodo de 30 días naturales.
 * - MSG1 a MSG10: Validaciones de datos faltantes, correos, estados y fortaleza de contraseñas.
 */
@Singleton
public class GestionarPasswordUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(GestionarPasswordUseCase.class);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d|.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$");

    private final CuentaRepository cuentaRepository;
    private final Map<String, TokenRecuperacion> tokensPorCodigo = new ConcurrentHashMap<>();
    private final List<TokenRecuperacion> historialSolicitudes = Collections.synchronizedList(new ArrayList<>());

    public GestionarPasswordUseCase(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    /**
     * Solicita la recuperación de contraseña (CU-G02).
     *
     * @param correo Correo electrónico del usuario
     * @return El token o código generado (para envío de correo / desarrollo)
     */
    public String solicitarRecuperacion(String correo) {
        if (correo == null || correo.trim().isBlank()) {
            throw new IllegalArgumentException(MensajesSistema.MSG01.getDescripcion());
        }

        String email = correo.trim().toLowerCase();
        Cuenta cuenta = cuentaRepository.findByCorreo(email)
            .orElseThrow(() -> new IllegalArgumentException(MensajesSistema.MSG02.getDescripcion()));

        if ("desactivada".equalsIgnoreCase(cuenta.estadoCuenta())) {
            throw new IllegalArgumentException(MensajesSistema.MSG04.getDescripcion());
        }

        // RN-06: Máximo 3 solicitudes por usuario en un periodo de 30 días naturales
        Instant hace30Dias = Instant.now().minus(Duration.ofDays(30));
        long solicitudesRecientes = historialSolicitudes.stream()
            .filter(t -> t.idCuenta().equals(cuenta.idCuenta()) && t.fechaCreacion().isAfter(hace30Dias))
            .count();

        if (solicitudesRecientes >= 3) {
            throw new IllegalStateException("Se ha superado el límite de 3 solicitudes de recuperación en un periodo de 30 días naturales (RN-06).");
        }

        // Generar código de recuperación de 8 caracteres alfanuméricos en mayúsculas
        String codigo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plus(Duration.ofHours(24)); // RN-04: 24 horas

        TokenRecuperacion token = new TokenRecuperacion(
            UUID.randomUUID(),
            cuenta.idCuenta(),
            email,
            codigo,
            ahora,
            expiracion,
            false
        );

        tokensPorCodigo.put(codigo, token);
        historialSolicitudes.add(token);

        LOG.info("Solicitud de recuperación generada para {}: código={} válido hasta={}", email, codigo, expiracion);
        return codigo;
    }

    /**
     * Restablece la contraseña utilizando el código de recuperación (CU-G03).
     */
    public void restablecerContrasena(String codigo, String nuevaContrasena, String confirmacionContrasena) {
        if (codigo == null || codigo.trim().isBlank()
            || nuevaContrasena == null || nuevaContrasena.isBlank()
            || confirmacionContrasena == null || confirmacionContrasena.isBlank()) {
            throw new IllegalArgumentException(MensajesSistema.MSG01.getDescripcion());
        }

        String tokenKey = codigo.trim().toUpperCase();
        TokenRecuperacion token = tokensPorCodigo.get(tokenKey);

        if (token == null || token.usado()) {
            throw new IllegalArgumentException(MensajesSistema.MSG07.getDescripcion()); // MSG7: Código no válido
        }

        if (token.estaExpirado()) {
            throw new IllegalArgumentException(MensajesSistema.MSG08.getDescripcion()); // MSG8: Código expirado
        }

        if (!nuevaContrasena.equals(confirmacionContrasena)) {
            throw new IllegalArgumentException(MensajesSistema.MSG09.getDescripcion()); // MSG9: Contraseñas no coinciden
        }

        // MSG10: Validación de fortaleza de contraseña
        if (!PASSWORD_PATTERN.matcher(nuevaContrasena).matches()) {
            throw new IllegalArgumentException(MensajesSistema.MSG10.getDescripcion());
        }

        Cuenta cuenta = cuentaRepository.findById(token.idCuenta())
            .orElseThrow(() -> new IllegalArgumentException(MensajesSistema.MSG02.getDescripcion()));

        String nuevoHash = BCrypt.hashpw(nuevaContrasena, BCrypt.gensalt(10));
        Cuenta cuentaActualizada = new Cuenta(
            cuenta.idCuenta(),
            cuenta.correo(),
            nuevoHash,
            cuenta.rol(),
            cuenta.estadoCuenta(),
            cuenta.fechaCreacion(),
            cuenta.nombreCompleto()
        );

        cuentaRepository.update(cuentaActualizada);

        // RN-05: El enlace o código queda invalidado automáticamente tras su uso
        tokensPorCodigo.put(tokenKey, new TokenRecuperacion(
            token.idToken(),
            token.idCuenta(),
            token.correo(),
            token.token(),
            token.fechaCreacion(),
            token.fechaExpiracion(),
            true
        ));

        LOG.info("Contraseña actualizada exitosamente para la cuenta {}", cuenta.correo());
    }

    public Optional<TokenRecuperacion> consultarToken(String codigo) {
        if (codigo == null) return Optional.empty();
        return Optional.ofNullable(tokensPorCodigo.get(codigo.trim().toUpperCase()));
    }
}
