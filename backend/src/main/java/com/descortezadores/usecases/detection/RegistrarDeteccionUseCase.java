package com.descortezadores.usecases.detection;

import com.descortezadores.adapters.dto.ArbolDTO;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.UUID;

/**
 * Caso de uso: RegistrarDeteccionUseCase / RegistrarArbol
 */
@Singleton
public class RegistrarDeteccionUseCase {

    private final ArbolRepository arbolRepository;

    public RegistrarDeteccionUseCase(ArbolRepository arbolRepository) {
        this.arbolRepository = arbolRepository;
    }

    public Arbol registrarArbol(ArbolDTO dto, UUID idCuenta) {
        Arbol nuevo = new Arbol(
            UUID.randomUUID(),
            dto.nombreEspecifico(),
            dto.especie(),
            dto.longitud(),
            dto.latitud(),
            Instant.now(),
            dto.descripcion(),
            idCuenta,
            dto.idDispositivo()
        );
        return arbolRepository.save(nuevo);
    }
}
