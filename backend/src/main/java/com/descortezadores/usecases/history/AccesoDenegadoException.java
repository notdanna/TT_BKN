package com.descortezadores.usecases.history;

/** La cuenta solicitante no es dueña del registro ni Administrador (la API responde 403). */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String message) {
        super(message);
    }
}
