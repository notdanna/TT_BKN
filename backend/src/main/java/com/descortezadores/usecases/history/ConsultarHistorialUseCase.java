package com.descortezadores.usecases.history;

import com.descortezadores.domain.entities.Analisis;
import com.descortezadores.domain.entities.Arbol;
import com.descortezadores.infrastructure.persistence.AnalisisRepository;
import com.descortezadores.infrastructure.persistence.ArbolRepository;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: ConsultarHistorialUseCase
 * Consultas para dashboard, historial y mapas web/móvil (§8.8.2 y §8.8.3).
 */
@Singleton
public class ConsultarHistorialUseCase {

    private final ArbolRepository arbolRepository;
    private final AnalisisRepository analisisRepository;

    public ConsultarHistorialUseCase(
        ArbolRepository arbolRepository,
        AnalisisRepository analisisRepository
    ) {
        this.arbolRepository = arbolRepository;
        this.analisisRepository = analisisRepository;
    }

    public List<Arbol> listarTodosArboles() {
        return arbolRepository.findAllOrderByFechaRegistroDesc();
    }

    public Optional<Arbol> obtenerArbolPorId(UUID idArbol) {
        return arbolRepository.findById(idArbol);
    }

    public List<Analisis> obtenerHistorialPorArbol(UUID idArbol) {
        return analisisRepository.findByIdArbol(idArbol);
    }

    public List<Analisis> obtenerTodosLosAnalisis() {
        return analisisRepository.findAllOrderByFechaCapturaDesc();
    }
}
