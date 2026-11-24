package com.concesionario.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class CorreoRecuperacion {
    private static final Logger log = LoggerFactory.getLogger(CorreoRecuperacion.class);
    private final JavaMailSender smtp;
    private final String remitenteSmtp;
    private final String claveBrevo;
    private final String remitenteBrevo;
    private final RestClient cliente;

    @Autowired
    CorreoRecuperacion(JavaMailSender smtp, @Value("${spring.mail.username}") String remitenteSmtp,
                     @Value("${BREVO_API_KEY:}") String claveBrevo,
                     @Value("${BREVO_SENDER_EMAIL:}") String remitenteBrevo) {
        this(smtp, remitenteSmtp, claveBrevo, remitenteBrevo, clienteHttps());
    }

    CorreoRecuperacion(JavaMailSender smtp, String remitenteSmtp, String claveBrevo,
                      String remitenteBrevo, RestClient cliente) {
        this.smtp = smtp;
        this.remitenteSmtp = remitenteSmtp;
        this.claveBrevo = claveBrevo;
        this.remitenteBrevo = remitenteBrevo;
        this.cliente = cliente;
        if (StringUtils.hasText(claveBrevo) != StringUtils.hasText(remitenteBrevo)) {
            throw new IllegalArgumentException("Configura BREVO_API_KEY y BREVO_SENDER_EMAIL juntos.");
        }
    }

    private static RestClient clienteHttps() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory).build();
    }

    void enviar(String destinatario, String asunto, String texto) {
        if (!StringUtils.hasText(claveBrevo)) {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitenteSmtp);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            smtp.send(mensaje);
            return;
        }
        cliente.post().uri("https://api.brevo.com/v3/smtp/email")
                .header("api-key", claveBrevo)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MensajeBrevo(Map.of("name", "NextGen Motors", "email", remitenteBrevo),
                        List.of(Map.of("email", destinatario)), asunto, texto))
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() != 201) {
                        log.warn("Brevo rechazó el correo de recuperación (HTTP {})", response.getStatusCode().value());
                        throw new IllegalStateException("No se pudo enviar el correo de recuperación.");
                    }
                    return null;
                });
    }

    private record MensajeBrevo(Map<String, String> sender, List<Map<String, String>> to,
                                String subject, String textContent) {
        // RestClient incluye toString() del cuerpo cuando se habilitan logs DEBUG.
        @Override
        public String toString() {
            return "Correo de recuperación";
        }
    }
}
