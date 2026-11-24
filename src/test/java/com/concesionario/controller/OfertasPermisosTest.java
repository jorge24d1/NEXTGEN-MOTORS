package com.concesionario.controller;

import com.concesionario.config.CustomOAuth2SuccessHandler;
import com.concesionario.config.SecurityConfig;
import com.concesionario.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OfertasController.class)
@Import(SecurityConfig.class)
class OfertasPermisosTest {
    static final String OFERTAS = "/usuario/perfil/ofertas";
    static final String COMPARTIR = "/asesor/vehiculos/v1/compartir";
    @Autowired MockMvc mvc;
    @MockitoBean BrevoOfertas suscripciones;
    @MockitoBean OfertasVehiculo vehiculos;
    @MockitoBean AuthService auth;
    @MockitoBean TrabajadorDetailsService trabajadores;
    @MockitoBean CustomOAuth2SuccessHandler success;
    @MockitoBean CustomOAuth2UserService oauth;
    @MockitoBean CustomOidcUserService oidc;
    @MockitoBean ClientRegistrationRepository registros;

    private MockHttpSession sesion(String rol) {
        var sesion = new MockHttpSession();
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("propio@example.org", null,
                AuthorityUtils.createAuthorityList("ROLE_" + rol)));
        sesion.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contexto);
        return sesion;
    }

    @Test void anonimoYRolesAjenosNoPuedenConsultarNiEnviar() throws Exception {
        mvc.perform(get(COMPARTIR)).andExpect(status().is3xxRedirection());
        mvc.perform(get(OFERTAS)).andExpect(status().is3xxRedirection());
        mvc.perform(get(COMPARTIR).session(sesion("USUARIO"))).andExpect(status().isForbidden());
        mvc.perform(get(OFERTAS).session(sesion("TRB_ASESOR"))).andExpect(status().isForbidden());
        var asesor = sesion("TRB_ASESOR");
        mvc.perform(get(COMPARTIR).session(asesor)).andExpect(status().isOk());
        mvc.perform(post(COMPARTIR).session(asesor).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verify(vehiculos, never()).enviar(anyString(), anyString(), any(), anyInt(), anyBoolean());
    }

    @Test void soloLaIdentidadDeLaSesionPuedeCambiarSuPreferencia() throws Exception {
        var usuario = sesion("USUARIO");
        var request = mvc.perform(get(OFERTAS).session(usuario)).andExpect(status().isOk()).andReturn().getRequest();
        CsrfToken csrf = (CsrfToken) request.getAttribute("_csrf");
        assertTrue(csrf.getToken().length() > 0);
        String cuerpo = "{\"aceptar\":true,\"email\":\"ajeno@example.org\"}";
        mvc.perform(post(OFERTAS).session(usuario).contentType("application/json").content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post(OFERTAS).session(usuario).header(csrf.getHeaderName(), csrf.getToken())
                .contentType("application/json").content(cuerpo)).andExpect(status().isOk());
        mvc.perform(post(COMPARTIR).session(usuario).header(csrf.getHeaderName(), csrf.getToken())
                .contentType("application/json").content("{}")).andExpect(status().isForbidden());
        verify(suscripciones).guardarPreferencia("propio@example.org", true);
        verify(suscripciones, never()).guardarPreferencia(eq("ajeno@example.org"), anyBoolean());
    }
}
