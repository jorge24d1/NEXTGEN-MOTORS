package com.concesionario.controller;

import com.concesionario.model.Cita;
import com.concesionario.model.Usuario;
import com.concesionario.repository.UsuarioRepository;
import com.concesionario.service.CitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuario")
@CrossOrigin(origins = "*") // Permitir peticiones desde la App Móvil
public class UsuarioRestController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CitaService citaService;

    @Autowired
    private PasswordEncoder passwordEncoder;



    // 1. Endpoint de Login para App Móvil
    @PostMapping("/login")
    public ResponseEntity<?> loginApp(@RequestParam String correo, @RequestParam String password) {
        try {
            Usuario usuario = usuarioRepository.findByCorreoUser(correo)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (passwordEncoder.matches(password, usuario.getPasswordUser())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("userId", usuario.getId());
                
                String nombre = usuario.getNombreUser() != null ? usuario.getNombreUser() : "";
                String apellido = usuario.getApellidoUser() != null ? usuario.getApellidoUser() : "";
                response.put("nombre", (nombre + " " + apellido).trim());
                
                response.put("message", "Login exitoso");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Contraseña incorrecta"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Credenciales inválidas"));
        }
    }

    // 2. Endpoint para obtener Citas del Usuario
    @GetMapping("/{userId}/citas")
    public ResponseEntity<?> obtenerCitasUsuario(@PathVariable String userId) {
        try {
            List<Cita> citas = citaService.obtenerCitasPorUsuarioId(userId);
            
            // Mapeamos a un formato limpio para el JSON
            List<Map<String, Object>> citasJson = citas.stream().map(cita -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", cita.getId());
                map.put("fechaSolicitud", cita.getFechaCreacion());
                map.put("fechaAsignada", cita.getFechaAsignada());
                map.put("estado", cita.getEstado()); // "Pendiente", "Aprobada", "Rechazada"
                map.put("comentario", cita.getComentario());
                map.put("notasAdmin", cita.getNotasAdmin());
                map.put("vehiculo", cita.getNombreVehiculo()); // Ojo: podría ser nulo si viene de objeto Vehiculo
                
                // Si hay objeto vehículo vinculado
                if(cita.getVehiculo() != null) {
                     map.put("vehiculo", cita.getVehiculo().getMarca() + " " + cita.getVehiculo().getModelo());
                }
                
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(citasJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Endpoint para guardar Token FCM (Notificaciones)
    @PostMapping("/{userId}/fcm-token")
    public ResponseEntity<?> guardarFcmToken(@PathVariable String userId, @RequestParam String token) {
        try {
            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            usuario.setFcmToken(token);
            usuarioRepository.save(usuario);
            
            System.out.println("📱 Token FCM actualizado para usuario: " + usuario.getCorreoUser());
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Token guardado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
