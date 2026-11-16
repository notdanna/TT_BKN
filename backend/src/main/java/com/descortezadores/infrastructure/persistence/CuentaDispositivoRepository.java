package com.descortezadores.infrastructure.persistence;

import com.descortezadores.domain.entities.CuentaDispositivo;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface CuentaDispositivoRepository extends CrudRepository<CuentaDispositivo, UUID> {
    List<CuentaDispositivo> findByIdCuenta(UUID idCuenta);
    Optional<CuentaDispositivo> findByIdCuentaAndIdDispositivo(UUID idCuenta, UUID idDispositivo);
    List<CuentaDispositivo> findByIdDispositivoAndEstadoCuentaDispositivo(UUID idDispositivo, String estado);
}
