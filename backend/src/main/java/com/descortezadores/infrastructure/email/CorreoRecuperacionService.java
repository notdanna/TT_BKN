package com.descortezadores.infrastructure.email;

/**
 * Servicio para el envío de códigos de recuperación de contraseña (CU-G02).
 */
public interface CorreoRecuperacionService {

    /**
     * Envía el código de recuperación al correo registrado del usuario.
     *
     * @param correoDestino Correo electrónico del destinatario
     * @param nombreUsuario Nombre del usuario para personalizar el mensaje
     * @param codigo Código alfanumérico de 8 caracteres
     * @param vigenciaHoras Horas de vigencia del código (RN-04)
     */
    void enviarCodigoRecuperacion(String correoDestino, String nombreUsuario, String codigo, int vigenciaHoras);
}
