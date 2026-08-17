package com.concesionario.service;

import com.concesionario.model.Cita;
import com.concesionario.model.Usuario;
import com.concesionario.repository.CitaRepository;
import com.concesionario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificacionService {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AzureHubService azureHubService;

    // =============================================
    // GESTIÓN DE CITAS Y NOTIFICACIONES INTERNAS DB
    // =============================================

    public long contarCitasNoLeidas() {
        return citaRepository.countByLeidaFalse();
    }

    public List<Cita> obtenerCitasNoLeidas() {
        return citaRepository.findByLeidaFalseOrderByFechaCreacionDesc();
    }

    public void marcarComoLeida(String id) {
        citaRepository.findById(id).ifPresent(cita -> {
            cita.setLeida(true);
            citaRepository.save(cita);
        });
    }

    public void marcarTodasComoLeidas() {
        List<Cita> citasNoLeidas = citaRepository.findByLeidaFalse();
        citasNoLeidas.forEach(cita -> cita.setLeida(true));
        citaRepository.saveAll(citasNoLeidas);
    }

    // =============================================
    // ENVÍO DE NOTIFICACIONES PUSH (FIREBASE / AZURE)
    // =============================================

    /**
     * @param userId ID del usuario destinatario
     * @param titulo Título de la notificación
     * @param mensaje Cuerpo del mensaje
     */
    public void enviarNotificacion(String userId, String titulo, String mensaje) {
        try {
            Usuario usuario = usuarioRepository.findById(userId).orElse(null);
            
            if (usuario != null && usuario.getFcmToken() != null && !usuario.getFcmToken().isEmpty()) {
                
                com.google.firebase.messaging.Message message = com.google.firebase.messaging.Message.builder()
                        .setToken(usuario.getFcmToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(titulo)
                                .setBody(mensaje)
                                .build())
                        .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                        .putData("userId", userId)
                        .build();

                String response = com.google.firebase.messaging.FirebaseMessaging.getInstance().send(message);
                
                System.out.println("✅ Notificación enviada DIRECTAMENTE vía Firebase SDK. ID: " + response);
                
            } else {
                System.out.println("⚠️ Usuario " + userId + " no tiene Token FCM registrado. No se envió notificación.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error enviando notificación Push: " + e.getMessage());
            e.printStackTrace();
        }
    }
}