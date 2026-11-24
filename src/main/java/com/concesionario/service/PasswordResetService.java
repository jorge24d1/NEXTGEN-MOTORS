package com.concesionario.service;

import com.concesionario.model.Usuario;
import com.concesionario.model.Trabajador;
import com.concesionario.model.Administrador;
import com.concesionario.repository.UsuarioRepository;
import com.concesionario.repository.TrabajadorRepository;
import com.concesionario.repository.AdministradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private CorreoRecuperacion correoRecuperacion;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.base-url:}") // El valor puede estar vacío
    private String configuredBaseUrl;

    private static final long EXPIRE_TOKEN_AFTER_MINUTES = 30;

    /**
     * Obtiene la URL base automáticamente detectando el entorno
     */
    private String getBaseUrl() {
        // 1. Si baseUrl está configurado explícitamente, úsalo
        if (StringUtils.hasText(configuredBaseUrl)) {
            return configuredBaseUrl.endsWith("/") ? configuredBaseUrl.substring(0, configuredBaseUrl.length() - 1) : configuredBaseUrl;
        }

        // 2. Detectar si estamos en Azure App Service
        String azureWebsiteHostname = System.getenv("WEBSITE_HOSTNAME");
        if (azureWebsiteHostname != null && !azureWebsiteHostname.isEmpty()) {
            // Estamos en Azure, usar HTTPS
            return "https://" + azureWebsiteHostname;
        }

        // 3. Por defecto para desarrollo local
        return "http://localhost:8080";
    }

    public void initiatePasswordReset(String email) {
        // Buscar primero en usuarios
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoUser(email);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String token = generateToken();
            usuario.setResetPasswordToken(token);
            usuario.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(EXPIRE_TOKEN_AFTER_MINUTES));
            usuarioRepository.save(usuario);
            sendResetEmail(usuario.getCorreoUser(), token, "usuario");
            return;
        }

        // Buscar en trabajadores
        Optional<Trabajador> trabajadorOpt = trabajadorRepository.findByCorreo(email);
        if (trabajadorOpt.isPresent()) {
            Trabajador trabajador = trabajadorOpt.get();
            String token = generateToken();
            trabajador.setResetPasswordToken(token);
            trabajador.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(EXPIRE_TOKEN_AFTER_MINUTES));
            trabajadorRepository.save(trabajador);
            sendResetEmail(trabajador.getCorreo(), token, "trabajador");
            return;
        }

        // Buscar en administradores
        Optional<Administrador> administradorOpt = administradorRepository.findByCorreoAdmin(email);
        if (administradorOpt.isPresent()) {
            Administrador administrador = administradorOpt.get();
            String token = generateToken();
            administrador.setResetPasswordToken(token);
            administrador.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(EXPIRE_TOKEN_AFTER_MINUTES));
            administradorRepository.save(administrador);
            sendResetEmail(administrador.getCorreoAdmin(), token, "administrador");
            return;
        }

    }

    public void resetPassword(String token, String newPassword) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Token inválido");
        }
        // Buscar en usuarios
        Optional<Usuario> usuarioOpt = usuarioRepository.findByResetPasswordToken(token);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (isTokenExpired(usuario.getResetPasswordTokenExpiry())) {
                throw new RuntimeException("El token ha expirado");
            }
            usuario.setPasswordUser(passwordEncoder.encode(newPassword));
            usuario.setResetPasswordToken(null);
            usuario.setResetPasswordTokenExpiry(null);
            usuarioRepository.save(usuario);
            return;
        }

        // Buscar en trabajadores
        Optional<Trabajador> trabajadorOpt = trabajadorRepository.findByResetPasswordToken(token);
        if (trabajadorOpt.isPresent()) {
            Trabajador trabajador = trabajadorOpt.get();
            if (isTokenExpired(trabajador.getResetPasswordTokenExpiry())) {
                throw new RuntimeException("El token ha expirado");
            }
            trabajador.setPassword(passwordEncoder.encode(newPassword));
            trabajador.setResetPasswordToken(null);
            trabajador.setResetPasswordTokenExpiry(null);
            trabajadorRepository.save(trabajador);
            return;
        }

        // Buscar en administradores
        Optional<Administrador> administradorOpt = administradorRepository.findByResetPasswordToken(token);
        if (administradorOpt.isPresent()) {
            Administrador administrador = administradorOpt.get();
            if (isTokenExpired(administrador.getResetPasswordTokenExpiry())) {
                throw new RuntimeException("El token ha expirado");
            }
            administrador.setPasswordAdmin(passwordEncoder.encode(newPassword));
            administrador.setResetPasswordToken(null);
            administrador.setResetPasswordTokenExpiry(null);
            administradorRepository.save(administrador);
            return;
        }

        throw new RuntimeException("Token inválido");
    }

    public void validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("Token inválido");
        }

        // Buscar en usuarios
        Optional<Usuario> usuarioOpt = usuarioRepository.findByResetPasswordToken(token);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (isTokenExpired(usuario.getResetPasswordTokenExpiry())) {
                throw new RuntimeException("El token ha expirado");
            }
            return; // Token válido
        }

        // Buscar en trabajadores
        Optional<Trabajador> trabajadorOpt = trabajadorRepository.findByResetPasswordToken(token);
        if (trabajadorOpt.isPresent()) {
            Trabajador trabajador = trabajadorOpt.get();
            if (isTokenExpired(trabajador.getResetPasswordTokenExpiry())) {
                throw new RuntimeException("El token ha expirado");
            }
            return; // Token válido
        }

        // Buscar en administradores
        Optional<Administrador> administradorOpt = administradorRepository.findByResetPasswordToken(token);
        if (administradorOpt.isPresent()) {
            Administrador administrador = administradorOpt.get();
            if (isTokenExpired(administrador.getResetPasswordTokenExpiry())) {
                throw new RuntimeException("El token ha expirado");
            }
            return; // Token válido
        }

        throw new RuntimeException("Token inválido");
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    private boolean isTokenExpired(LocalDateTime tokenExpiry) {
        return tokenExpiry == null || LocalDateTime.now().isAfter(tokenExpiry);
    }

    private void sendResetEmail(String email, String token, String tipoUsuario) {
        String baseUrl = getBaseUrl();
        String resetLink = baseUrl + "/auth/reset-password?token=" + token;

        String tipoCuenta = "";
        switch (tipoUsuario) {
            case "usuario":
                tipoCuenta = "cliente";
                break;
            case "trabajador":
                tipoCuenta = "trabajador";
                break;
            case "administrador":
                tipoCuenta = "administrador";
                break;
        }

        correoRecuperacion.enviar(email, "Restablecimiento de contraseña - NextGenMotors",
                "Hola,\n\n" +
                        "Has solicitado restablecer tu contraseña de " + tipoCuenta + " en NextGenMotors.\n\n" +
                        "Para crear una nueva contraseña, haz clic en el siguiente enlace:\n" +
                        resetLink +
                        "\n\nEste enlace expirará en " + EXPIRE_TOKEN_AFTER_MINUTES + " minutos." +
                        "\n\nSi no solicitaste este restablecimiento, por favor ignora este mensaje." +
                        "\n\nSaludos cordiales,\nEquipo NextGenMotors"
        );
    }

    // Método adicional para verificar si un email existe en cualquier tipo de cuenta
    public boolean emailExists(String email) {
        return usuarioRepository.findByCorreoUser(email).isPresent() ||
                trabajadorRepository.findByCorreo(email).isPresent() ||
                administradorRepository.findByCorreoAdmin(email).isPresent();
    }

    // Método para obtener la URL base (útil para testing)
    public String getCurrentBaseUrl() {
        return getBaseUrl();
    }
}
