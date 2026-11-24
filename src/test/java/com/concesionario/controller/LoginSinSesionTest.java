package com.concesionario.controller;

import com.concesionario.config.CustomOAuth2SuccessHandler;
import com.concesionario.config.SecurityConfig;
import com.concesionario.service.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = LoginSinSesionTest.Web.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "spring.config.import=", "logging.level.org.springframework.web=WARN"})
class LoginSinSesionTest {
    @TestComponent
    @Configuration(proxyBeanMethods = false)
    @Import({AuthController.class, SecurityConfig.class})
    @ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class, ThymeleafAutoConfiguration.class,
            SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
    static class Web {}

    @LocalServerPort int puerto;
    @MockitoBean UsuarioService usuarios;
    @MockitoBean AdministradorService administradores;
    @MockitoBean ValidacionService validacion;
    @MockitoBean AuthService auth;
    @MockitoBean TrabajadorDetailsService trabajadores;
    @MockitoBean CustomOAuth2SuccessHandler success;
    @MockitoBean CustomOAuth2UserService oauth;
    @MockitoBean CustomOidcUserService oidc;
    @MockitoBean ClientRegistrationRepository registros;

    @Test void primeraVisitaRecibeElFormularioCompletoYLaCookieDeSesion() throws Exception {
        var peticion = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + "/login")).GET().build();
        var respuesta = HttpClient.newHttpClient().send(peticion, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("name=\"password\"") && respuesta.body().contains("name=\"_csrf\""));
        assertTrue(respuesta.body().stripTrailing().endsWith("</html>"));
        assertTrue(respuesta.headers().allValues("Set-Cookie").stream().anyMatch(c -> c.startsWith("JSESSIONID=")));
    }
}
