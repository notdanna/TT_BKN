package com.descortezadores.adapters.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.UUID;

@Serdeable
public record SyncBatchResponse(
    int totalRecibidos,
    int totalSincronizados,
    List<UUID> idsSincronizados,
    List<String> errores
) {}
