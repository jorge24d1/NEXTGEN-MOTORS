package com.concesionario.repository;

import com.concesionario.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    @org.springframework.data.mongodb.repository.Query("{ '_id': ?0 }")
    @org.springframework.data.mongodb.repository.Update("{ '$set': { 'fcmToken': ?1 } }")
    void actualizarFcmToken(String id, String token);

    Optional<Usuario> findByCorreoUser(String correo);
    boolean existsByCorreoUser(String correo);
    boolean existsByIdentificacionUser(String identificacion);
    Optional<Usuario> findByResetPasswordToken(String token);
    
}
