package com.concesionario.config;

import com.concesionario.service.AuthService;
import com.concesionario.service.CustomOAuth2UserService;
import com.concesionario.service.CustomOidcUserService;
import com.concesionario.service.TrabajadorDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.Collection;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AuthService authService;

    @Autowired
    private TrabajadorDetailsService trabajadorDetailsService;

    @Autowired
    private CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private CustomOidcUserService customOidcUserService;

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> authorities;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/usuario/Inicio",
                                "/ubicaciones",
                                "/terminos",
                                "/cookies",
                                "/registro-admin",
                                "/usuario",
                                "/registro",
                                "/vehiculos",
                                "/vehiculos/explorar/{id}",
                                "/styles4.css",
                                "/images/**",
                                "/nosotros",
                                "/garantias",
                                "/credito",
                                "/api/chatbot/mensaje",
                                "/login",
                                "/usuario/agendamiento",
                                "/usuario/loginup",
                                "/usuario/completar-perfil",
                                "/css/**",
                                "/js/**",
                                "/STloginup.css",
                                "/uploads/**",
                                "/auth/**",
                                "/test/status/**",
                                "/api/usuario/**",
                                "/api/n8n/**"
                        ).permitAll()
                        .requestMatchers("/perfil_gestor").hasRole("TRB_GESTOR")
                        .requestMatchers("/perfil_analisis").hasRole("TRB_ANALISIS")
                        .requestMatchers("/perfil_asesor").hasRole("TRB_ASESOR")
                        .requestMatchers("/admin/**").hasAnyRole("ADMINISTRADOR", "TRABAJADOR", "TRB_GESTOR", "TRB_ANALISIS", "TRB_ASESOR")
                        .requestMatchers("/usuario/cita", "/usuario/cita/guardar").hasAnyRole("USUARIO", "ADMINISTRADOR", "TRABAJADOR", "TRB_GESTOR", "TRB_ANALISIS", "TRB_ASESOR")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler())
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)
                                .userAuthoritiesMapper(userAuthoritiesMapper())
                        )
                        .successHandler(customOAuth2SuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/usuario/Inicio")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // ✅ CONFIGURACIÓN MEJORADA: Control de sesiones concurrentes
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession() // Protege contra ataques de fixation
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false) // Invalida la sesión anterior
                        .expiredUrl("/login?expired=true")
                        .sessionRegistry(sessionRegistry()) // Registro de sesiones activas
                )

                .userDetailsService(userDetailsService());

        return http.build();
    }

    public UserDetailsService userDetailsService() {
        return authService;
    }


    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_TRB_GESTOR"))) {
                response.sendRedirect("/perfil_gestor");
            }
            else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_TRB_ANALISIS"))) {
                response.sendRedirect("/perfil_analisis");
            }
            else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_TRB_ASESOR"))) {
                response.sendRedirect("/perfil_asesor");
            }
            else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"))) {
                response.sendRedirect("/admin/dashboard");
            }
            else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_TRABAJADOR"))) {
                response.sendRedirect("/admin/dashboard");
            }
            else {
                response.sendRedirect("/usuario/Inicio");
            }
        };
    }

    // ✅ BEANS NECESARIOS PARA EL CONTROL DE SESIONES
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}