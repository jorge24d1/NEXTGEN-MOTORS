package com.concesionario.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class BrevoOfertasTest {
    private final ObjectMapper json = new ObjectMapper();
    private final LinkedBlockingQueue<Respuesta> respuestas = new LinkedBlockingQueue<>();
    private final List<Peticion> llamadas = new ArrayList<>();
    private HttpServer servidor;
    private URI api;
    record Respuesta(int estado, String json) {}
    record Peticion(String metodo, String ruta, JsonNode cuerpo) {}

    @BeforeEach void iniciar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            var cuerpo = intercambio.getRequestBody().readAllBytes();
            llamadas.add(new Peticion(intercambio.getRequestMethod(), intercambio.getRequestURI().getPath(),
                    cuerpo.length == 0 ? json.createObjectNode() : json.readTree(cuerpo)));
            var respuesta = respuestas.remove();
            byte[] bytes = respuesta.json().getBytes(UTF_8);
            intercambio.sendResponseHeaders(respuesta.estado(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
        api = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/");
    }

    @AfterEach void detener() { servidor.stop(0); }

    private BrevoOfertas cliente(String prueba, boolean activo) {
        return new BrevoOfertas(json, api, "prueba", "ofertas@example.org", 3, prueba, activo);
    }

    @Test void pruebaSoloParaUnaDireccionInclusoSiTambienSeActivaEnvioGeneral() throws Exception {
        var brevo = cliente("piloto@example.org", true);
        respuestas.add(new Respuesta(201, "{\"id\":7}"));
        respuestas.add(new Respuesta(204, ""));
        assertEquals(1, brevo.destinatarios());
        brevo.enviar(brevo.crearBorrador("nombre", "asunto", "<p>vehículo</p>"));
        assertFalse(llamadas.get(0).cuerpo().has("recipients"));
        assertEquals("/emailCampaigns/7/sendTest", llamadas.get(1).ruta());
        assertEquals(json.readTree("{\"emailTo\":[\"piloto@example.org\"]}"), llamadas.get(1).cuerpo());
    }

    @Test void campanaUsaSolamenteLaListaDeQuienesAceptaronYReportaCuota() throws Exception {
        var brevo = cliente("", true);
        respuestas.add(new Respuesta(200, "{\"totalSubscribers\":2,\"uniqueSubscribers\":7}"));
        respuestas.add(new Respuesta(201, "{\"id\":8}"));
        respuestas.add(new Respuesta(402, "detalle interno"));
        assertEquals(2, brevo.destinatarios());
        long id = brevo.crearBorrador("nombre", "asunto", "html");
        assertEquals(json.readTree("{\"listIds\":[3]}"), llamadas.get(1).cuerpo().get("recipients"));
        assertEquals(429, assertThrows(ResponseStatusException.class, () -> brevo.enviar(id)).getStatusCode().value());
        assertEquals("/emailCampaigns/8/sendNow", llamadas.get(2).ruta());
    }

    @Test void sinModoExplicitoOConDireccionMultipleSeImpideElEnvio() {
        assertThrows(ResponseStatusException.class, () -> cliente("", false).destinatarios());
        assertThrows(ResponseStatusException.class, () -> cliente("uno@example.org,dos@example.org", true).destinatarios());
        assertTrue(llamadas.isEmpty());
    }

    @Test void altasYBajasLimitadasALaListaDeOfertas() throws Exception {
        var brevo = cliente("", true);
        String activo = "{\"listIds\":[3],\"emailBlacklisted\":false}";
        respuestas.add(new Respuesta(404, "{}"));
        respuestas.add(new Respuesta(201, "{}"));
        respuestas.add(new Respuesta(200, activo));
        assertTrue(brevo.guardarPreferencia("cliente@example.org", true).suscrito());
        assertEquals(List.of(3), json.convertValue(llamadas.get(1).cuerpo().get("listIds"), List.class));
        assertFalse(llamadas.get(1).cuerpo().has("emailBlacklisted"));
        respuestas.add(new Respuesta(200, activo));
        respuestas.add(new Respuesta(204, ""));
        respuestas.add(new Respuesta(200, "{\"listIds\":[4]}"));
        assertFalse(brevo.guardarPreferencia("cliente@example.org", false).suscrito());
        assertEquals(json.readTree("{\"unlinkListIds\":[3]}"), llamadas.get(4).cuerpo());
    }

    @Test void respetaLaBajaEnElCorreoSinReactivarElContacto() {
        var brevo = cliente("", true);
        for (String estado : List.of("{\"emailBlacklisted\":true}", "{\"listIds\":[3],\"listUnsubscribed\":[3]}")) {
            respuestas.add(new Respuesta(200, estado));
            assertThrows(ResponseStatusException.class, () -> brevo.guardarPreferencia("cliente@example.org", true));
        }
        assertTrue(llamadas.stream().allMatch(p -> p.metodo().equals("GET")));
    }
}
