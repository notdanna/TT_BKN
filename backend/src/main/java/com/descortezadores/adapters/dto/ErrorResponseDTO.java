package com.descortezadores.adapters.dto;

import com.descortezadores.domain.rules.MensajesSistema;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ErrorResponseDTO(
    String codigo,
    String mensaje
) {
    public static ErrorResponseDTO de(MensajesSistema mensaje) {
        return new ErrorResponseDTO(mensaje.getCodigo(), mensaje.getDescripcion());
    }

    public static ErrorResponseDTO personalizado(String codigo, String mensaje) {
        return new ErrorResponseDTO(codigo, mensaje);
    }
}
