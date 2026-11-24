package com.concesionario.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class CorreoRecuperacionTest {
    private final JavaMailSender smtp = mock(JavaMailSender.class);
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
    private final RestClient cliente = builder.build();
    private final String destino = "cliente@example.org";
    private final String enlace = "https://example.org/auth/reset-password?token=prueba";

    @Test
    void enviaPorHttpsAlDestinatarioConElEnlaceOriginal() {
        brevo.expect(requestTo("https://api.brevo.com/v3/smtp/email")).andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "prueba"))
                .andExpect(jsonPath("$.sender.email").value("avisos@example.org"))
                .andExpect(jsonPath("$.to[0].email").value(destino))
                .andExpect(jsonPath("$.subject").value("Recuperación"))
                .andExpect(jsonPath("$.textContent").value(enlace))
                .andRespond(withStatus(HttpStatus.CREATED));
        new CorreoRecuperacion(smtp, "", "prueba", "avisos@example.org", cliente)
                .enviar(destino, "Recuperación", enlace);
        brevo.verify();
        verifyNoInteractions(smtp);
    }

    @Test
    void rechazoDeBrevoNoExponeElCuerpoNiDuplicaElEnvio() {
        brevo.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body(enlace));
        var correo = new CorreoRecuperacion(smtp, "", "prueba", "avisos@example.org", cliente);
        var error = assertThrows(IllegalStateException.class, () -> correo.enviar(destino, "Recuperación", enlace));
        assertFalse(error.getMessage().contains(enlace));
        verifyNoInteractions(smtp);
        brevo.verify();
    }

    @Test
    void configuracionParcialSeDetectaAlArrancar() {
        assertThrows(IllegalArgumentException.class, () -> new CorreoRecuperacion(smtp, "", "", destino, cliente));
        assertThrows(IllegalArgumentException.class, () -> new CorreoRecuperacion(smtp, "", "prueba", "", cliente));
    }

    @Test
    void usaSmtpSiAmbasVariablesEstanVacias() {
        new CorreoRecuperacion(smtp, "local@example.org", "", "", cliente).enviar(destino, "Recuperación", enlace);
        verify(smtp).send(argThat((SimpleMailMessage m) -> "local@example.org".equals(m.getFrom())
                && destino.equals(m.getTo()[0]) && enlace.equals(m.getText())));
        brevo.verify();
    }
}
