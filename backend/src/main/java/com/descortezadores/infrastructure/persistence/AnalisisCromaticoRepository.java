package com.descortezadores.infrastructure.persistence;

import com.descortezadores.domain.entities.AnalisisCromatico;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface AnalisisCromaticoRepository extends CrudRepository<AnalisisCromatico, UUID> {
    Optional<AnalisisCromatico> findByIdAnalisis(UUID idAnalisis);
}
