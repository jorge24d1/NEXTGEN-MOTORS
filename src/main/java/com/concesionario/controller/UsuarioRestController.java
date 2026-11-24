package com.concesionario.controller;

import com.concesionario.dto.PerfilMovil;
import com.concesionario.model.Cita;
import com.concesionario.repository.UsuarioRepository;
import com.concesionario.service.AccesoMovil;
import com.concesionario.service.CitaService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuario")
@CrossOrigin(origins = "*")
public class UsuarioRestController {
    private final AccesoMovil acceso;
    private final CitaService citas;
    private final UsuarioRepository usuarios;

    public UsuarioRestController(AccesoMovil acceso, CitaService citas, UsuarioRepository usuarios) {
        this.acceso = acceso;
        this.citas = citas;
        this.usuarios = usuarios;
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<Void> persistenciaNoDisponible() {
        return ResponseEntity.status(503).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String correo, @RequestParam String password) {
        return acceso.iniciar(correo, password).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("success", false, "message", "Credenciales inválidas")));
    }

    @GetMapping("/sesion")
    public PerfilMovil sesion(@AuthenticationPrincipal PerfilMovil perfil) {
        return perfil;
    }

    @PostMapping("/salir")
    public ResponseEntity<Void> salir(@RequestHeader("Authorization") String autorizacion) {
        acceso.cerrar(autorizacion.substring("Bearer ".length()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/citas")
    public ResponseEntity<?> obtenerCitas(@PathVariable String userId, @AuthenticationPrincipal PerfilMovil perfil) {
        if (!perfil.userId().equals(userId)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(citas.obtenerCitasPorUsuarioId(perfil.userId()).stream()
                .map(this::resumenCita).toList());
    }

    private Map<String, Object> resumenCita(Cita cita) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("id", cita.getId());
        datos.put("fechaSolicitud", cita.getFechaCreacion());
        datos.put("fechaAsignada", cita.getFechaAsignada());
        datos.put("estado", cita.getEstado());
        datos.put("comentario", cita.getComentario());
        datos.put("notasAdmin", cita.getNotasAdmin());
        datos.put("vehiculo", cita.getVehiculo() == null ? cita.getNombreVehiculo()
                : cita.getVehiculo().getMarca() + " " + cita.getVehiculo().getModelo());
        return datos;
    }

    @PostMapping("/{userId}/fcm-token")
    public ResponseEntity<?> guardarFcmToken(@PathVariable String userId, @RequestParam String token,
                                             @AuthenticationPrincipal PerfilMovil perfil) {
        if (!perfil.userId().equals(userId)) return ResponseEntity.status(403).build();
        if (token.isBlank() || token.length() > 4096) return ResponseEntity.badRequest().build();
        usuarios.actualizarFcmToken(perfil.userId(), token);
        return ResponseEntity.ok(Map.of("success", true, "message", "Token guardado"));
    }
}
