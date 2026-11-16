package com.descortezadores.infrastructure.email;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

/**
 * Implementación de envío de correos de recuperación mediante Amazon SES.
 *
 * Se activa automáticamente cuando 'aws.ses.enabled: true' (por ejemplo en el perfil 'aws').
 */
@Singleton
@Requires(property = "aws.ses.enabled", value = "true")
public class SesCorreoRecuperacionService implements CorreoRecuperacionService {

    private static final Logger LOG = LoggerFactory.getLogger(SesCorreoRecuperacionService.class);

    private final String fromEmail;
    private final SesClient sesClient;

    public SesCorreoRecuperacionService(
        @Value("${aws.ses.region:us-east-1}") String region,
        @Value("${aws.ses.from-email:no-reply@ecoverde.mx}") String fromEmail
    ) {
        this.fromEmail = fromEmail;
        this.sesClient = SesClient.builder()
            .region(Region.of(region != null && !region.isBlank() ? region : "us-east-1"))
            .build();
        LOG.info("SesCorreoRecuperacionService inicializado en región {} con remitente {}", region, fromEmail);
    }

    @Override
    public void enviarCodigoRecuperacion(String correoDestino, String nombreUsuario, String codigo, int vigenciaHoras) {
        String saludo = (nombreUsuario != null && !nombreUsuario.isBlank()) ? nombreUsuario : "Brigadista";
        String subject = "EcoVerde — Código de recuperación de contraseña";

        String textBody = String.format(
            "Hola %s,\n\n" +
            "Recibimos una solicitud para restablecer tu contraseña en EcoVerde.\n\n" +
            "Tu código de recuperación es: %s\n" +
            "Vigencia: %d horas (un solo uso).\n\n" +
            "Si no solicitaste este cambio, puedes ignorar este mensaje.",
            saludo, codigo, vigenciaHoras
        );

        String htmlBody = String.format(
            "<div style=\"font-family:sans-serif;max-width:520px;margin:0 auto;padding:24px;border:1px solid #e0e0e0;border-radius:12px;\">" +
            "<h2 style=\"color:#1b5e20;margin-top:0;\">EcoVerde — Recuperación de contraseña</h2>" +
            "<p>Hola <strong>%s</strong>,</p>" +
            "<p>Recibimos una solicitud para restablecer tu contraseña. Usa el siguiente código en la aplicación:</p>" +
            "<div style=\"background:#f1f3f5;border-radius:8px;padding:16px;text-align:center;margin:20px 0;\">" +
            "<span style=\"font-size:26px;font-weight:bold;letter-spacing:4px;color:#1b5e20;\">%s</span>" +
            "</div>" +
            "<p style=\"color:#555;font-size:13px;\">Este código tiene una vigencia de <strong>%d horas</strong> y es de un solo uso.</p>" +
            "<p style=\"color:#777;font-size:12px;margin-bottom:0;\">Si no solicitaste este cambio, ignora este correo.</p>" +
            "</div>",
            escapeHtml(saludo), escapeHtml(codigo), vigenciaHoras
        );

        SendEmailRequest request = SendEmailRequest.builder()
            .source(fromEmail)
            .destination(Destination.builder().toAddresses(correoDestino).build())
            .message(
                Message.builder()
                    .subject(Content.builder().charset("UTF-8").data(subject).build())
                    .body(
                        Body.builder()
                            .text(Content.builder().charset("UTF-8").data(textBody).build())
                            .html(Content.builder().charset("UTF-8").data(htmlBody).build())
                            .build()
                    )
                    .build()
            )
            .build();

        try {
            sesClient.sendEmail(request);
            LOG.info("Correo de recuperación enviado por Amazon SES a {}", correoDestino);
        } catch (SesException e) {
            LOG.error("Error de Amazon SES al enviar correo a {}: {}", correoDestino, e.awsErrorDetails().errorMessage(), e);
            throw new IllegalStateException("No fue posible enviar el correo de recuperación: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    private static String escapeHtml(String input) {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
