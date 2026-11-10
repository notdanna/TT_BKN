package com.descortezadores.usecases.history;

import java.util.UUID;

/** Quién pide la operación, tomado del JWT (atributo id_cuenta y rol). */
public record Solicitante(UUID idCuenta, boolean esAdministrador) {

    public static final String ROL_ADMINISTRADOR = "Administrador";

    /** Puede modificar un registro si es suyo o si es Administrador. */
    public boolean puedeModificar(UUID dueno) {
        return esAdministrador || (idCuenta != null && idCuenta.equals(dueno));
    }
}
