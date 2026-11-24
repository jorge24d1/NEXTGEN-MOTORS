package com.concesionario.config;

import com.concesionario.dto.PerfilMovil;
import com.concesionario.service.AccesoMovil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SeguridadMovil {
    @Bean @Order(1)
    SecurityFilterChain apiMovil(HttpSecurity http, AccesoMovil acceso) throws Exception {
        String usuarios = "/api/usuario";
        return http.securityMatcher(usuarios + "/**", "/api/prediccion/**")
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                // La API solo acepta la credencial explícita del encabezado. No autentica con cookies.
                .csrf(c -> c.ignoringRequestMatchers(usuarios + "/login", usuarios + "/*/fcm-token", usuarios + "/salir"))
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.setStatus(401))
                        .accessDeniedHandler((req, res, ex) -> res.setStatus(403)))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, usuarios + "/login").permitAll()
                        .requestMatchers(HttpMethod.GET, usuarios + "/sesion").authenticated()
                        .requestMatchers(HttpMethod.POST, usuarios + "/salir").authenticated()
                        .requestMatchers(HttpMethod.GET, usuarios + "/*/citas").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, usuarios + "/*/fcm-token").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/prediccion/embudo").hasAnyRole("ADMINISTRADOR", "TRABAJADOR")
                        .anyRequest().denyAll())
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                            FilterChain chain) throws ServletException, IOException {
                        String header = req.getHeader("Authorization");
                        if (header != null && header.startsWith("Bearer ")) {
                            try {
                                acceso.identificar(header.substring(7)).ifPresent(SeguridadMovil::autenticar);
                            } catch (org.springframework.dao.DataAccessException e) {
                                res.setStatus(503);
                                return;
                            }
                        }
                        chain.doFilter(req, res);
                    }
                }, AnonymousAuthenticationFilter.class)
                .build();
    }

    private static void autenticar(PerfilMovil perfil) {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(perfil, null,
                AuthorityUtils.createAuthorityList("ROLE_" + perfil.role())));
        SecurityContextHolder.setContext(contexto);
    }
}
