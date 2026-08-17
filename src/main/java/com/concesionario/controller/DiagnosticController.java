package com.concesionario.controller;

import com.concesionario.service.NotificacionService;
import com.concesionario.repository.UsuarioRepository;
import com.concesionario.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/test/status")
public class DiagnosticController {

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/v2")
    public ResponseEntity<?> checkStatus() {
        return ResponseEntity.ok("Backend Online. Time: " + java.time.LocalDateTime.now());
    }

    // Probar envío a un usuario específico
    @PostMapping("/force-notif")
    public ResponseEntity<?> forceNotification(@RequestParam String correo) {
        try {
            Usuario usuario = usuarioRepository.findByCorreoUser(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con correo: " + correo));

            String fcmToken = usuario.getFcmToken();
            if (fcmToken == null || fcmToken.isEmpty()) {
                return ResponseEntity.badRequest().body("El usuario existe pero NO tiene Token FCM registrado.");
            }

            notificacionService.enviarNotificacion(usuario.getId(), "Test de Diagnóstico", "Si lees esto, las notificaciones funcionan ✅");
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "Enviado");
            response.put("userId", usuario.getId());
            response.put("token_full", fcmToken);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    @Autowired
    private com.concesionario.service.AzureHubService azureHubService;

    // Probar envío crudo (Raw Payload) para depurar formato
    @PostMapping("/send-raw")
    public ResponseEntity<?> sendRawNotification(@RequestParam String token, @RequestBody String jsonBody) {
        try {
            System.out.println("Testing Raw Payload to: " + token);
            System.out.println("Body: " + jsonBody);
            
            azureHubService.sendNotification(jsonBody, token);
            
            return ResponseEntity.ok("Enviado (Raw). Verifica tu celular.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
