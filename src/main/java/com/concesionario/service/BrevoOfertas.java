package com.concesionario.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.InternetAddress;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class BrevoOfertas {
    private final ObjectMapper json;
    private final HttpClient http;
    private final URI api;
    private final String clave;
    private final String remitente;
    private final long lista;
    private final String prueba;
    private final boolean envioClientes;

    @Autowired
    BrevoOfertas(ObjectMapper json, @Value("${BREVO_MARKETING_API_KEY:}") String clave,
                 @Value("${BREVO_SENDER_EMAIL:}") String remitente, @Value("${BREVO_OFERTAS_LISTA_ID:0}") long lista,
                 @Value("${OFERTAS_CORREO_PRUEBA:}") String prueba, @Value("${OFERTAS_ENVIO_CLIENTES:false}") boolean envioClientes) {
        this(json, URI.create("https://api.brevo.com/v3/"), clave, remitente, lista, prueba, envioClientes);
    }

    BrevoOfertas(ObjectMapper json, URI api, String clave, String remitente, long lista, String prueba, boolean envioClientes) {
        this.json = json;
        this.api = api;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.clave = clave;
        this.remitente = remitente;
        this.lista = lista;
        this.prueba = prueba.strip();
        this.envioClientes = envioClientes;
    }

    boolean esPrueba() {
        return !prueba.isEmpty();
    }

    int destinatarios() {
        if (clave.isBlank() || remitente.isBlank()) throw inactivo();
        if (esPrueba()) {
            try {
                InternetAddress direccion = new InternetAddress(prueba, true);
                direccion.validate();
                if (!prueba.equals(direccion.getAddress()) || direccion.isGroup()) throw inactivo();
            } catch (jakarta.mail.internet.AddressException e) {
                throw inactivo();
            }
            return 1;
        }
        if (!envioClientes || lista <= 0) throw inactivo();
        JsonNode cantidad = peticion("GET", "contacts/lists/" + lista, null, false).path("totalSubscribers");
        if (!cantidad.canConvertToInt() || cantidad.asInt() < 0) throw respuestaInvalida();
        return cantidad.asInt();
    }

    public record Preferencia(boolean suscrito, boolean bloqueado) {}

    public Preferencia preferencia(String email) {
        if (lista <= 0) throw inactivo();
        String ruta = "contacts/" + UriUtils.encodePathSegment(email, UTF_8);
        JsonNode contacto = peticion("GET", ruta, null, true);
        boolean bloqueado = contacto.path("emailBlacklisted").asBoolean() || contieneLista(contacto.path("listUnsubscribed"));
        return new Preferencia(!bloqueado && contieneLista(contacto.path("listIds")), bloqueado);
    }

    private boolean contieneLista(JsonNode ids) {
        for (JsonNode id : ids) {
            if (id.asLong() == lista) return true;
        }
        return false;
    }

    public Preferencia guardarPreferencia(String email, boolean aceptar) {
        Preferencia actual = preferencia(email);
        if (aceptar && actual.bloqueado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Brevo tiene registrada una baja para este correo.");
        }
        if (actual.suscrito() == aceptar) return actual;
        if (aceptar) {
            peticion("POST", "contacts", Map.of("email", email, "listIds", List.of(lista), "updateEnabled", true), false);
        } else {
            peticion("PUT", "contacts/" + UriUtils.encodePathSegment(email, UTF_8), Map.of("unlinkListIds", List.of(lista)), false);
        }
        return preferencia(email);
    }

    long crearBorrador(String nombre, String asunto, String html) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("name", nombre);
        datos.put("subject", asunto);
        datos.put("sender", Map.of("name", "NextGen Motors", "email", remitente));
        datos.put("htmlContent", html);
        if (!esPrueba()) datos.put("recipients", Map.of("listIds", List.of(lista)));
        long id = peticion("POST", "emailCampaigns", datos, false).path("id").asLong();
        if (id <= 0) throw respuestaInvalida();
        return id;
    }

    void enviar(long id) {
        String ruta = "emailCampaigns/" + id;
        if (esPrueba()) {
            peticion("POST", ruta + "/sendTest", Map.of("emailTo", List.of(prueba)), false);
        } else {
            if (!envioClientes || lista <= 0) throw inactivo();
            peticion("POST", ruta + "/sendNow", null, false);
        }
    }

    private JsonNode peticion(String metodo, String ruta, Object cuerpo, boolean admitirAusente) {
        if (clave.isBlank()) throw inactivo();
        try {
            var contenido = cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(cuerpo));
            var solicitud = HttpRequest.newBuilder(api.resolve(ruta)).timeout(Duration.ofSeconds(12))
                    .header("api-key", clave).header("Content-Type", "application/json")
                    .method(metodo, contenido).build();
            var respuesta = http.send(solicitud, HttpResponse.BodyHandlers.ofByteArray());
            int estado = respuesta.statusCode();
            if (admitirAusente && estado == 404) return json.createObjectNode();
            if (estado == 402 || estado == 429) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Brevo alcanzó un límite de envío o de solicitudes.");
            }
            if (estado < 200 || estado >= 300) throw respuestaInvalida();
            if (respuesta.body().length == 0) return json.createObjectNode();
            return json.readTree(respuesta.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw respuestaInvalida();
        } catch (IOException e) {
            throw respuestaInvalida();
        }
    }

    private static ResponseStatusException inactivo() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de ofertas aún no está configurado.");
    }

    private static ResponseStatusException respuestaInvalida() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Brevo no confirmó la operación. Revisa el estado en Brevo antes de repetirla.");
    }
}
