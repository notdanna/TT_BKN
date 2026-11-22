package com.descortezadores.usecases.detection;

import com.descortezadores.adapters.dto.ArbolDTO;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
import com.descortezadores.usecases.sync.AseguradorDispositivo;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.UUID;

/**
 * Caso de uso: RegistrarDeteccionUseCase / RegistrarArbol
 */
@Singleton
public class RegistrarDeteccionUseCase {

    private final ArbolRepository arbolRepository;
    @Nullable
    private final AseguradorDispositivo aseguradorDispositivo;

    public RegistrarDeteccionUseCase(ArbolRepository arbolRepository) {
        this(arbolRepository, null);
    }

    @Inject
    public RegistrarDeteccionUseCase(
        ArbolRepository arbolRepository,
        @Nullable AseguradorDispositivo aseguradorDispositivo
    ) {
        this.arbolRepository = arbolRepository;
        this.aseguradorDispositivo = aseguradorDispositivo;
    }

    public Arbol registrarArbol(ArbolDTO dto, UUID idCuenta) {
        UUID id = dto.idArbol() != null ? dto.idArbol() : UUID.randomUUID();
        UUID idDispositivo = aseguradorDispositivo != null
            ? aseguradorDispositivo.asegurar(dto.idDispositivo(), idCuenta, null)
            : dto.idDispositivo();
        var existente = arbolRepository.findById(id);
        if (existente.isPresent()) {
            Arbol prev = existente.get();
            if (prev.idDispositivo() == null && idDispositivo != null) {
                return arbolRepository.update(new Arbol(
                    prev.idArbol(),
                    prev.nombreEspecifico(),
                    prev.especie(),
                    prev.longitud(),
                    prev.latitud(),
                    prev.fechaRegistro(),
                    prev.descripcion(),
                    prev.idCuenta(),
                    idDispositivo
                ));
            }
            return prev;
        }
        Arbol nuevo = new Arbol(
            id,
            dto.nombreEspecifico(),
            dto.especie(),
            dto.longitud(),
            dto.latitud(),
            dto.fechaRegistro() != null ? dto.fechaRegistro() : Instant.now(),
            dto.descripcion(),
            idCuenta,
            idDispositivo
        );
        return arbolRepository.save(nuevo);
    }
}
