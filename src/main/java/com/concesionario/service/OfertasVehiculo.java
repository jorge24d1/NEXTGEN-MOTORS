package com.concesionario.service;

import com.concesionario.model.EnvioOferta;
import com.concesionario.model.Vehiculo;
import com.concesionario.repository.EnvioOfertaRepository;
import com.concesionario.repository.VehiculoRepository;
import java.net.URI;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import static com.concesionario.model.EnvioOferta.Estado.*;

@Service
public class OfertasVehiculo {
    private final VehiculoRepository vehiculos;
    private final EnvioOfertaRepository envios;
    private final BrevoOfertas brevo;
    private final String urlBase;

    public OfertasVehiculo(VehiculoRepository vehiculos, EnvioOfertaRepository envios, BrevoOfertas brevo,
                           @Value("${app.base-url}") String urlBase) {
        this.vehiculos = vehiculos;
        this.envios = envios;
        this.brevo = brevo;
        this.urlBase = urlBase;
    }

    public record Vista(String vehiculo, int destinatarios, boolean prueba) {}
    public record Confirmacion(long campana, boolean prueba) {}

    public Vista vista(String id) {
        return new Vista(nombre(vehiculo(id)), brevo.destinatarios(), brevo.esPrueba());
    }

    public Confirmacion enviar(String asesor, String id, UUID solicitud, int cantidadConfirmada, boolean pruebaConfirmada) {
        String clave = solicitud.toString();
        var previo = envios.findById(clave);
        if (previo.isPresent()) return resultado(previo.get(), asesor, id);
        Vehiculo vehiculo = vehiculo(id);
        int cantidad = brevo.destinatarios();
        if (cantidad == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Todavía no hay clientes suscritos.");
        if (cantidadConfirmada != cantidad || pruebaConfirmada != brevo.esPrueba()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cambió el público de la oferta. Vuelve a revisar la cantidad.");
        }
        String asunto = "Oferta de vehículo · " + nombre(vehiculo);
        String contenido = contenido(vehiculo);
        EnvioOferta envio = new EnvioOferta(clave, asesor, id, Instant.now(), null, PREPARANDO, brevo.esPrueba());
        try {
            envios.insert(envio);
        } catch (DuplicateKeyException e) {
            return resultado(envios.findById(clave).orElseThrow(), asesor, id);
        }
        try {
            long campana = brevo.crearBorrador(asunto + " · " + clave, asunto, contenido);
            envio = envio.conEstado(campana, BORRADOR);
            envios.save(envio);
            brevo.enviar(campana);
            envios.save(envio.conEstado(campana, ACEPTADO));
            return new Confirmacion(campana, envio.prueba());
        } catch (ResponseStatusException e) {
            envios.save(envio.conEstado(envio.campana(), REVISAR));
            throw e;
        }
    }

    private Confirmacion resultado(EnvioOferta envio, String asesor, String vehiculo) {
        if (!envio.asesor().equals(asesor) || !envio.vehiculo().equals(vehiculo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta confirmación pertenece a otra solicitud.");
        }
        if (envio.estado() == ACEPTADO) return new Confirmacion(envio.campana(), envio.prueba());
        if (envio.estado() == REVISAR) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El envío anterior necesita revisión en Brevo.");
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya existe una solicitud para este envío. Comprueba su estado en Brevo antes de iniciar otra.");
    }

    private Vehiculo vehiculo(String id) {
        return vehiculos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado."));
    }

    private String nombre(Vehiculo v) {
        return (v.getMarca() + " " + v.getModelo()).replaceAll("[\\p{Cntrl}{}]", "").strip();
    }

    private String contenido(Vehiculo v) {
        String nombre = escape(nombre(v));
        String enlace = UriComponentsBuilder.fromUriString(urlBase).pathSegment("vehiculos", "explorar", v.getId())
                .build().encode().toUriString();
        NumberFormat moneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        moneda.setMaximumFractionDigits(0);
        return """
                <html lang="es"><body style="font:16px Arial,sans-serif;color:#152031">
                <p>NextGen Motors</p><h1>%s</h1>%s
                <p><strong>%s COP</strong> · Modelo %d</p>
                <p>Motor: %s<br>Transmisión: %s</p>
                <p><a href="%s">Ver el vehículo en NextGen Motors</a></p>
                <p><a href="{{ unsubscribe }}">Dejar de recibir ofertas</a></p>
                </body></html>
                """.formatted(nombre, imagen(v), moneda.format(v.getPrecio()), v.getAño(),
                escape(v.getMotor()), escape(v.getTransmision()), escape(enlace));
    }

    private String imagen(Vehiculo v) {
        if (v.getImagenUrl() == null || v.getImagenUrl().isBlank()) return "";
        try {
            URI url = URI.create(urlBase + "/").resolve(v.getImagenUrl());
            if (url.getHost() != null && ("https".equalsIgnoreCase(url.getScheme()) || "http".equalsIgnoreCase(url.getScheme()))) {
                return "<img width=\"560\" style=\"max-width:100%;height:auto\" alt=\"" + escape(nombre(v)) + "\" src=\"" + escape(url.toString()) + "\">";
            }
        } catch (IllegalArgumentException ignored) {
            // Una imagen incompleta no impide abrir la ficha del vehículo.
        }
        return "";
    }

    private static String escape(String texto) {
        return texto == null ? "No especificado" : HtmlUtils.htmlEscape(texto).replace("{", "&#123;").replace("}", "&#125;");
    }
}
