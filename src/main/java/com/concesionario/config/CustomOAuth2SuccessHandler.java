package com.concesionario.config;

import com.concesionario.model.Administrador;
import com.concesionario.model.Rol;
import com.concesionario.model.Trabajador;
import com.concesionario.model.Usuario;
import com.concesionario.repository.AdministradorRepository;
import com.concesionario.repository.TrabajadorRepository;
import com.concesionario.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import java.net.URI;
import java.io.IOException;
import java.util.Optional;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        logger.info("Login OAuth2 exitoso, procesando éxito para: {}", email);

        // 1. Verificar si es Administrador
        Optional<Administrador> adminOpt = administradorRepository.findByCorreoAdmin(email);
        if (adminOpt.isPresent()) {
            logger.info("Redirigiendo ADMINISTRADOR a /admin/dashboard");
            response.sendRedirect("/admin/dashboard");
            return;
        }

        // 2. Verificar si es Trabajador
        Optional<Trabajador> trabajadorOpt = trabajadorRepository.findByCorreo(email);
        if (trabajadorOpt.isPresent()) {
            Trabajador trabajador = trabajadorOpt.get();
            logger.info("Redirigiendo TRABAJADOR con roles {} a su respectivo dashboard", trabajador.getRoles());
            System.out.println("DEBUG: Redirigiendo Trabajador: " + email + " con roles: " + trabajador.getRoles());
            if (trabajador.tieneRol(Rol.TRB_GESTOR)) response.sendRedirect("/perfil_gestor");
            else if (trabajador.tieneRol(Rol.TRB_ANALISIS)) response.sendRedirect("/perfil_analisis");
            else if (trabajador.tieneRol(Rol.TRB_ASESOR)) response.sendRedirect("/perfil_asesor");
            else if (trabajador.tieneRol(Rol.TRABAJADOR)) response.sendRedirect("/admin/dashboard");
            return;
        }

        // 3. Verificar si es Usuario existente
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoUser(email);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            HttpSessionRequestCache cache = new HttpSessionRequestCache();
            SavedRequest saved = cache.getRequest(request, response);
            if (saved != null && "GET".equals(saved.getMethod())) {
                URI destino = URI.create(saved.getRedirectUrl());
                String rutaCita = request.getContextPath() + "/usuario/cita";
                if (rutaCita.equals(destino.getRawPath())) {
                    cache.removeRequest(request, response);
                    String query = destino.getRawQuery();
                    response.sendRedirect(rutaCita + (query == null ? "" : "?" + query));
                    return;
                }
            }
            if (usuario.perfilIncompleto()) {
                logger.info("Perfil incompleto para {}, redirigiendo a completar-perfil", email);
                response.sendRedirect("/usuario/completar-perfil");
            } else {
                logger.info("Perfil completo para {}, redirigiendo a Inicio", email);
                response.sendRedirect("/usuario/Inicio");
            }
            return;
        }

        // 4. Si no se encontró en ningún repositorio (teóricamente imposible si el UserService funcionó)
        response.sendRedirect("/usuario/completar-perfil");
    }
}
