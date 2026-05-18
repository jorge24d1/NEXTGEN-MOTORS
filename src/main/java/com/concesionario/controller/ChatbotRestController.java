package com.concesionario.controller;

import com.concesionario.service.ChatbotService;
import com.concesionario.repository.VehiculoRepository;
import com.concesionario.model.Vehiculo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotRestController {

    @Autowired
    private ChatbotService chatbotService;
    
    @Autowired
    private VehiculoRepository vehiculoRepository;

    @PostMapping("/mensaje")
    public ResponseEntity<?> recibirMensaje(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            String mensaje = (String) request.get("mensaje");
            List<Map<String, String>> historial = (List<Map<String, String>>) request.get("historial");

            boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

            String respuestaIA = chatbotService.analizarYResponder(mensaje, historial, isAdmin);
            
            List<Vehiculo> recomendados = new ArrayList<>();
            
            // Buscar todos los [[ID: xxx]] generados por la IA
            Pattern pattern = Pattern.compile("\\[\\[ID:\\s*([^\\]]+)\\]\\]");
            Matcher matcher = pattern.matcher(respuestaIA);
            
            while (matcher.find()) {
                String id = matcher.group(1).trim();
                vehiculoRepository.findById(id).ifPresent(vehiculo -> {
                    // Evitar duplicados por si la IA repite un carro
                    if (recomendados.stream().noneMatch(v -> v.getId().equals(vehiculo.getId()))) {
                        recomendados.add(vehiculo);
                    }
                });
            }
            
            // Limpiar la respuesta para que el usuario no vea los [[ID: xxx]]
            respuestaIA = matcher.replaceAll("");

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("respuesta", respuestaIA.trim()); // trim para quitar espacios extra
            
            if (!recomendados.isEmpty()) {
                responseMap.put("vehiculosRecomendados", recomendados);
            }

            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            System.err.println("--- ERROR EN EL CHATBOT ---");
            System.err.println("Causa: " + (e.getCause() != null ? e.getCause().getMessage() : "Desconocida"));
            System.err.println("Mensaje: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("respuesta", "Lo siento, Dante tuvo un error al pensar. Detalles: " + e.getMessage()));
        }
    }
}