package com.descortezadores.infrastructure.persistence;

import com.descortezadores.domain.entities.Cuenta;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface CuentaRepository extends CrudRepository<Cuenta, UUID> {
    Optional<Cuenta> findByCorreo(String correo);
}
