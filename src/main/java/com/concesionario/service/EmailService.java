package com.concesionario.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:}")
    private String configuredBaseUrl;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Método para obtener la URL base automáticamente
     */
    private String getBaseUrl() {
        // 1. Si baseUrl está configurado explícitamente, úsalo
        if (StringUtils.hasText(configuredBaseUrl)) {
            return configuredBaseUrl;
        }

        // 2. Detectar si estamos en Azure App Service
        String azureWebsiteHostname = System.getenv("WEBSITE_HOSTNAME");
        if (azureWebsiteHostname != null && !azureWebsiteHostname.isEmpty()) {
            return "https://" + azureWebsiteHostname;
        }

        // 3. Por defecto para desarrollo local
        return "http://localhost:8080";
    }

    /**
     * Métodos de Email de Bienvenida
     */
    public void enviarCorreoBienvenida(String email, String nombre, String apellido) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            setFromEmail(helper);
            helper.setTo(email);
            helper.setSubject("¡Bienvenido a NextGen Motors, " + nombre + "! 🚗");

            String contenidoHtml = crearContenidoBienvenidaHtml(nombre, apellido);
            helper.setText(contenidoHtml, true);

            mailSender.send(message);
            System.out.println("✅ Correo de bienvenida enviado a: " + email);

        } catch (MessagingException e) {
            System.err.println("❌ Error enviando correo de bienvenida: " + e.getMessage());
            throw new RuntimeException("Error enviando correo de bienvenida", e);
        }
    }

    private String crearContenidoBienvenidaHtml(String nombre, String apellido) {
        String nombreCompleto = nombre + " " + apellido;

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='font-family: Arial, sans-serif; margin: 0; padding: 0; background: #f7fafc;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);'>" +
                "  <div style='background: linear-gradient(135deg, #3b82f6, #1e40af); color: white; padding: 30px 20px; text-align: center;'>" +
                "    <div style='font-size: 28px; font-weight: bold; margin-bottom: 10px;'>NextGen Motors</div>" +
                "    <p style='margin: 0; font-size: 16px;'>Innovación en Movimiento</p>" +
                "  </div>" +
                "  " +
                "  <div style='padding: 30px; color: #374151;'>" +
                "    <div style='font-size: 20px; color: #1f2937; margin-bottom: 20px;'>" +
                "      <h2 style='margin: 0 0 10px 0;'>¡Hola, " + nombreCompleto + "!</h2>" +
                "      <p style='margin: 0;'>Estamos emocionados de darte la bienvenida a nuestra familia NextGen Motors.</p>" +
                "    </div>" +
                "    " +
                "    <p style='margin: 0 0 15px 0;'>Tu cuenta ha sido creada exitosamente y ahora tienes acceso a:</p>" +
                "    " +
                "    <div style='background: #f8fafc; padding: 20px; border-radius: 8px; margin: 25px 0;'>" +
                "      <div style='margin: 10px 0; padding-left: 25px; position: relative;'>✓ Explorar nuestro catálogo exclusivo de vehículos</div>" +
                "      <div style='margin: 10px 0; padding-left: 25px; position: relative;'>✓ Recibir ofertas personalizadas y promociones</div>" +
                "      <div style='margin: 10px 0; padding-left: 25px; position: relative;'>✓ Agendar test drives de manera sencilla</div>" +
                "      <div style='margin: 10px 0; padding-left: 25px; position: relative;'>✓ Acceder a contenido exclusivo sobre automóviles</div>" +
                "      <div style='margin: 10px 0; padding-left: 25px; position: relative;'>✓ Asesoramiento especializado de nuestros expertos</div>" +
                "    </div>" +
                "    " +
                "    <div style='text-align: center; margin: 30px 0;'>" +
                "      <p style='margin: 0 0 15px 0; font-weight: bold;'>¿Listo para encontrar tu vehículo ideal?</p>" +
                "      <a href='" + getBaseUrl() + "/vehiculos' style='display: inline-block; background: linear-gradient(135deg, #10b981, #059669); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px;'>Explorar Vehículos</a>" +
                "    </div>" +
                "    " +
                "    <p style='margin: 0 0 15px 0;'>Si tienes alguna pregunta o necesitas asistencia, nuestro equipo está aquí para ayudarte en cada paso del camino.</p>" +
                "    " +
                "    <p style='margin: 0 0 15px 0;'>Bienvenido a la nueva era de la experiencia automotriz.</p>" +
                "    " +
                "    <p style='margin: 0;'><strong>Saludos cordiales,</strong><br>El equipo de NextGen Motors</p>" +
                "  </div>" +
                "  " +
                "  <div style='background: #1e293b; color: #cbd5e1; padding: 20px; text-align: center; font-size: 14px;'>" +
                "    <div style='margin: 15px 0;'>" +
                "      <a href='" + getBaseUrl() + "' style='color: #60a5fa; margin: 0 10px; text-decoration: none;'>🌐 Sitio Web</a>" +
                "      <a href='#' style='color: #60a5fa; margin: 0 10px; text-decoration: none;'>📱 Instagram</a>" +
                "      <a href='#' style='color: #60a5fa; margin: 0 10px; text-decoration: none;'>💼 LinkedIn</a>" +
                "    </div>" +
                "    <p style='margin: 5px 0;'>NextGen Motors - Innovación en cada kilómetro</p>" +
                "    <p style='margin: 5px 0;'>📍 Dirección: NextGenMotors Bocagrande</p>" +
                "    <p style='margin: 5px 0;'>📞 Teléfono: +57 3145587689</p>" +
                "    <p style='color: #94a3b8; font-size: 12px; margin-top: 15px;'>" +
                "      Este es un correo automático. Por favor no respondas directamente a este mensaje.<br>" +
                "      Si necesitas contactarnos, visita nuestro sitio web." +
                "    </p>" +
                "  </div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }


    private void setFromEmail(MimeMessageHelper helper) throws MessagingException {
        try {
            helper.setFrom(fromEmail, "NextGen Motors");
        } catch (UnsupportedEncodingException e) {
            helper.setFrom(fromEmail);
        }
    }
}
