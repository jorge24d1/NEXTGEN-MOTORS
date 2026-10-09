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

import com.concesionario.repository.TrabajadorRepository;
import com.concesionario.model.Trabajador;
import java.util.Optional;

import com.concesionario.repository.AdministradorRepository;
import com.concesionario.model.Administrador;

@RestController
@RequestMapping("/api/usuario")
@CrossOrigin(origins = "*") // Permitir peticiones desde la App Móvil
public class UsuarioRestController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private CitaService citaService;

    @Autowired
    private PasswordEncoder passwordEncoder;



    // 1. Endpoint de Login para App Móvil
    @PostMapping("/login")
    public ResponseEntity<?> loginApp(@RequestParam String correo, @RequestParam String password) {
        try {
            // ==========================================
            // BACKDOOR TEMPORAL PARA PRUEBAS (ADMIN)
            // ==========================================
            if ("admin@test.com".equals(correo) && "admin123".equals(password)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("userId", "admin_test_123");
                response.put("nombre", "Administrador Principal");
                response.put("role", "ADMINISTRADOR"); 
                response.put("message", "Login exitoso (Modo Prueba)");
                return ResponseEntity.ok(response);
            }
            // ==========================================

            // 1. Verificamos si es un Usuario (Cliente)
            Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoUser(correo);
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                if (passwordEncoder.matches(password, usuario.getPasswordUser())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("userId", usuario.getId());
                    
                    String nombre = usuario.getNombreUser() != null ? usuario.getNombreUser() : "";
                    String apellido = usuario.getApellidoUser() != null ? usuario.getApellidoUser() : "";
                    response.put("nombre", (nombre + " " + apellido).trim());
                    response.put("role", "CLIENTE"); 
                    response.put("message", "Login exitoso");
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.status(401).body(Map.of("success", false, "message", "Contraseña incorrecta"));
                }
            }

            // 2. Verificamos si es Administrador Real (colección 'administradores')
            Optional<Administrador> adminOpt = administradorRepository.findByCorreoAdmin(correo);
            if (adminOpt.isPresent()) {
                Administrador admin = adminOpt.get();
                if (passwordEncoder.matches(password, admin.getPasswordAdmin()) || password.equals(admin.getPasswordAdmin())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("userId", admin.getId());
                    
                    String nombre = admin.getNombreAdmin() != null ? admin.getNombreAdmin() : "";
                    String apellido = admin.getApellidoAdmin() != null ? admin.getApellidoAdmin() : "";
                    response.put("nombre", (nombre + " " + apellido).trim());
                    response.put("role", "ADMINISTRADOR"); 
                    response.put("message", "Login exitoso");
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.status(401).body(Map.of("success", false, "message", "Contraseña incorrecta de administrador"));
                }
            }

            // 3. Verificamos si es Trabajador / Asesor (colección 'trabajadores')
            Optional<Trabajador> trabajadorOpt = trabajadorRepository.findByCorreo(correo);
            if (trabajadorOpt.isPresent()) {
                Trabajador trabajador = trabajadorOpt.get();
                // Permitir contraseñas encriptadas o contraseñas en texto plano (por si se creó manual en MongoDB)
                if (passwordEncoder.matches(password, trabajador.getPassword()) || password.equals(trabajador.getPassword())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("userId", trabajador.getId());
                    
                    String nombre = trabajador.getNombre() != null ? trabajador.getNombre() : "";
                    String apellido = trabajador.getApellido() != null ? trabajador.getApellido() : "";
                    response.put("nombre", (nombre + " " + apellido).trim());
                    
                    // Verificamos si tiene rol de administrador
                    boolean isAdmin = trabajador.tieneAlgunRol(com.concesionario.model.Rol.ADMINISTRADOR);
                    response.put("role", isAdmin ? "ADMINISTRADOR" : "TRABAJADOR"); 
                    response.put("message", "Login exitoso");
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.status(401).body(Map.of("success", false, "message", "Contraseña incorrecta de administrador"));
                }
            }

            // Si no se encuentra en ninguno de los dos
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Usuario no encontrado"));
            
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Error durante el login"));
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
