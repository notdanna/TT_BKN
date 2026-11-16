package com.descortezadores.domain.rules;

/**
 * Catálogo de mensajes oficiales del sistema según Tabla 6 (pág. 51)
 * del documento de Trabajo Terminal (ESCOM - IPN).
 */
public enum MensajesSistema {
    MSG01("MSG1", "Dato requerido faltante."),
    MSG02("MSG2", "El correo electrónico no está registrado."),
    MSG03("MSG3", "Contraseña incorrecta."),
    MSG04("MSG4", "La cuenta se encuentra desactivada."),
    MSG05("MSG5", "El dispositivo no está autorizado para esta cuenta."),
    MSG06("MSG6", "No fue posible enviar el correo de recuperación."),
    MSG07("MSG7", "El enlace o código de recuperación no es válido."),
    MSG08("MSG8", "El enlace o código de recuperación ha expirado."),
    MSG09("MSG9", "Las contraseñas no coinciden."),
    MSG10("MSG10", "La contraseña no cumple con los requisitos de seguridad."),
    MSG11("MSG11", "No existe una sesión activa."),
    MSG12("MSG12", "No fue posible cerrar la sesión correctamente."),
    MSG13("MSG13", "No existen registros para la jornada actual."),
    MSG14("MSG14", "No fue posible cargar la información local."),
    MSG15("MSG15", "No existen registros con el estado sanitario seleccionado."),
    MSG16("MSG16", "No fue posible cargar los registros recientes."),
    MSG17("MSG17", "No fue posible acceder a la cámara."),
    MSG18("MSG18", "No fue posible importar la imagen seleccionada."),
    MSG19("MSG19", "Imagen no válida para el análisis."),
    MSG20("MSG20", "No fue posible registrar la ubicación GPS."),
    MSG21("MSG21", "No fue posible guardar el análisis en la base local."),
    MSG22("MSG22", "No fue posible obtener la clasificación del estado sanitario."),
    MSG23("MSG23", "No fue posible recortar la imagen."),
    MSG24("MSG24", "Área de recorte no válida."),
    MSG25("MSG25", "No fue posible sincronizar el registro."),
    MSG26("MSG26", "No existen registros sincronizados disponibles."),
    MSG27("MSG27", "No fue posible cargar la información consolidada."),
    MSG28("MSG28", "No fue posible aplicar los filtros seleccionados."),
    MSG29("MSG29", "No se cuenta con permisos de administración."),
    MSG30("MSG30", "No fue posible cargar la información de usuarios."),
    MSG31("MSG31", "El correo electrónico ya se encuentra registrado."),
    MSG32("MSG32", "No fue posible guardar los cambios de la cuenta."),
    MSG33("MSG33", "El dispositivo ya se encuentra asociado a una cuenta."),
    MSG34("MSG34", "No fue posible asociar el dispositivo."),
    MSG35("MSG35", "No fue posible desasociar el dispositivo.");

    private final String codigo;
    private final String descripcion;

    MensajesSistema(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
