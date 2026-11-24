package com.concesionario.controller;

import com.concesionario.service.BrevoOfertas;
import com.concesionario.service.OfertasVehiculo;
import com.concesionario.utils.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class OfertasController {
    private final OfertasVehiculo ofertas;
    private final BrevoOfertas suscripciones;

    public OfertasController(OfertasVehiculo ofertas, BrevoOfertas suscripciones) {
        this.ofertas = ofertas;
        this.suscripciones = suscripciones;
    }

    public record Preferencia(@NotNull Boolean aceptar) {}
    public record Envio(@NotNull UUID solicitud, @NotNull @Min(1) Integer destinatarios, @NotNull Boolean prueba) {}

    @GetMapping("/usuario/perfil/ofertas")
    public BrevoOfertas.Preferencia preferencia(Principal principal) {
        return suscripciones.preferencia(SecurityUtils.getEmailFromPrincipal(principal));
    }

    @PostMapping("/usuario/perfil/ofertas")
    public BrevoOfertas.Preferencia guardar(Principal principal, @Valid @RequestBody Preferencia datos) {
        return suscripciones.guardarPreferencia(SecurityUtils.getEmailFromPrincipal(principal), datos.aceptar());
    }

    @GetMapping("/asesor/vehiculos/{id}/compartir")
    public OfertasVehiculo.Vista vista(@PathVariable String id) {
        return ofertas.vista(id);
    }

    @PostMapping("/asesor/vehiculos/{id}/compartir")
    public OfertasVehiculo.Confirmacion compartir(@PathVariable String id, Principal principal, @Valid @RequestBody Envio datos) {
        return ofertas.enviar(principal.getName(), id, datos.solicitud(), datos.destinatarios(), datos.prueba());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> error(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("mensaje", error.getReason()));
    }
}
