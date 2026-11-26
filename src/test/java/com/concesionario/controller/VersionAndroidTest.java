package com.concesionario.controller;

import com.concesionario.config.*;
import com.concesionario.dto.FichaAndroid;
import com.concesionario.service.*;
import java.time.Instant;
import java.util.Optional;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VersionAndroidController.class)
@Import(SecurityConfig.class)
class VersionAndroidTest {
    static final String ADMIN = "/admin/versiones-app";
    @Autowired MockMvc mvc;
    @MockitoBean VersionesAndroid versiones;
    @MockitoBean AuthService auth;
    @MockitoBean TrabajadorDetailsService trabajadores;
    @MockitoBean CustomOAuth2SuccessHandler oauthSuccess;
    @MockitoBean CustomOAuth2UserService oauth;
    @MockitoBean CustomOidcUserService oidc;
    @MockitoBean ClientRegistrationRepository registros;

    MockHttpSession sesion(String role) {
        var sesion = new MockHttpSession();
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("cuenta", null,
                AuthorityUtils.createAuthorityList("ROLE_" + role)));
        sesion.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contexto);
        return sesion;
    }

    @Test void descargaYFichaSonPublicasConOSinVersion() throws Exception {
        mvc.perform(get("/api/movil/version")).andExpect(status().isNoContent());
        mvc.perform(get("/descargar-app")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("primera versión")));
        var ficha = new FichaAndroid(1, "1.0.0", "Citas y chat", Instant.EPOCH, "https://example.org/app.apk");
        when(versiones.vigente()).thenReturn(Optional.of(ficha));
        mvc.perform(get("/api/movil/version")).andExpect(jsonPath("$.codigo").value(1));
        mvc.perform(get("/descargar-app")).andExpect(content().string(org.hamcrest.Matchers.containsString(ficha.descarga())));
    }

    @Test void soloAdministradorConCsrfPuedePublicar() throws Exception {
        mvc.perform(get(ADMIN)).andExpect(status().is3xxRedirection());
        mvc.perform(get(ADMIN).session(sesion("TRABAJADOR"))).andExpect(status().isForbidden());
        var admin = sesion("ADMINISTRADOR");
        var req = mvc.perform(get(ADMIN).session(admin)).andExpect(status().isOk()).andReturn().getRequest();
        CsrfToken csrf = (CsrfToken) req.getAttribute("_csrf");
        mvc.perform(post(ADMIN + "/1/publicar").session(admin)).andExpect(status().isForbidden());
        verify(versiones, never()).publicar(anyInt());
        mvc.perform(post(ADMIN + "/1/publicar").session(admin).header(csrf.getHeaderName(), csrf.getToken()))
                .andExpect(redirectedUrl(ADMIN));
        verify(versiones).publicar(1);
    }
}
