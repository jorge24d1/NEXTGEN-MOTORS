package com.concesionario.service;

import com.concesionario.model.Usuario;
import com.concesionario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    /**
     * @param userId ID del usuario destinatario
     * @param titulo Título de la notificación
     * @param mensaje Cuerpo del mensaje
     */
    public void enviarNotificacion(String userId, String titulo, String mensaje) {
        try {
            Usuario usuario = usuarioRepository.findById(userId).orElse(null);
            
            if (usuario != null && usuario.getFcmToken() != null && !usuario.getFcmToken().isEmpty()) {
                
                // Construir mensaje nativo de FCM en formato JSON
                // Azure Notification Hubs recibe el payload de FCM y lo reenvía
                // ==========================================
                // ENVÍO DIRECTO VIA FIREBASE ADMIN ADD (BYPASS AZURE)
                // ==========================================
                
                // 1. Construir el mensaje V1 usando el SDK oficial
                // No necesitamos construir JSON manual, el objeto Message lo hace.
                com.google.firebase.messaging.Message message = com.google.firebase.messaging.Message.builder()
                        .setToken(usuario.getFcmToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(titulo)
                                .setBody(mensaje)
                                .build())
                        .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                        .putData("userId", userId)
                        .build();

                // 2. Enviar directamente a Google
                String response = com.google.firebase.messaging.FirebaseMessaging.getInstance().send(message);
                
                System.out.println("✅ Notificación enviada DIRECTAMENTE vía Firebase SDK. ID: " + response);
                
            } else {
                System.out.println("⚠️ Usuario " + userId + " no tiene Token FCM registrado. No se envió notificación.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error enviando notificación Azure: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
