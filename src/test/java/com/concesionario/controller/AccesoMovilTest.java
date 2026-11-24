package com.concesionario.controller;

import com.concesionario.config.SeguridadMovil;
import com.concesionario.model.*;
import com.concesionario.repository.*;
import com.concesionario.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({UsuarioRestController.class, PrediccionController.class})
@Import({SeguridadMovil.class, AccesoMovil.class, BCryptPasswordEncoder.class})
class AccesoMovilTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BCryptPasswordEncoder encoder;
    @MockitoBean UsuarioRepository usuarios;
    @MockitoBean AdministradorRepository admins;
    @MockitoBean TrabajadorRepository trabajadores;
    @MockitoBean SesionMovilRepository sesiones;
    @MockitoBean CitaService citas;
    @MockitoBean UsuarioService clientes;
    @MockitoBean PrediccionService predicciones;
    @MockitoBean CitaRepository repoCitas;
    final Map<String, SesionMovil> almacen = new HashMap<>();
    static final String BASE = "/api/usuario";
    static final String EMBUDO = "/api/prediccion/embudo";
    final String clave = UUID.randomUUID().toString();

    @BeforeEach void preparar() {
        when(sesiones.insert(any(SesionMovil.class))).thenAnswer(call -> {
            SesionMovil s = call.getArgument(0);
            almacen.put(s.huella(), s);
            return s;
        });
        when(sesiones.findById(anyString())).thenAnswer(call -> Optional.ofNullable(almacen.get(call.getArgument(0))));
        doAnswer(call -> almacen.remove(call.getArgument(0))).when(sesiones).deleteById(anyString());
    }

    String entrar(String correo, String rol) throws Exception {
        var result = mvc.perform(post(BASE + "/login").param("correo", correo).param("password", clave))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(rol))
                .andExpect(jsonPath("$.vence").exists()).andReturn();
        return "Bearer " + json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    String cliente() throws Exception {
        Usuario u = new Usuario();
        u.setId("cliente"); u.setPasswordUser(encoder.encode(clave));
        when(usuarios.findByCorreoUser("cliente@example.org")).thenReturn(Optional.of(u));
        when(usuarios.findById("cliente")).thenReturn(Optional.of(u));
        return entrar("cliente@example.org", "CLIENTE");
    }

    @Test void anonimoOCredencialDesconocida() throws Exception {
        mvc.perform(get(BASE + "/cliente/citas")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/cliente/fcm-token").param("token", "prueba")).andExpect(status().isUnauthorized());
        mvc.perform(get(EMBUDO).header("Authorization", "Bearer " + "x".repeat(43))).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/login").param("correo", "nadie@example.org").param("password", clave))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(citas);
    }

    @Test void laCookieWebNoSustituyeLaCredencialMovil() throws Exception {
        var sesion = new org.springframework.mock.web.MockHttpSession();
        var contexto = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated("personal", null, org.springframework.security.core.authority.AuthorityUtils
                        .createAuthorityList("ROLE_ADMINISTRADOR")));
        sesion.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository
                .SPRING_SECURITY_CONTEXT_KEY, contexto);
        mvc.perform(get(EMBUDO).session(sesion)).andExpect(status().isUnauthorized());
        verifyNoInteractions(clientes);
    }

    @Test void clienteSoloOperaSobreSusRecursos() throws Exception {
        String auth = cliente();
        mvc.perform(get(BASE + "/sesion").header("Authorization", auth)).andExpect(jsonPath("$.userId").value("cliente"));
        mvc.perform(get(BASE + "/cliente/citas").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(post(BASE + "/cliente/fcm-token").param("token","dispositivo").header("Authorization", auth))
                .andExpect(status().isOk());
        mvc.perform(post(BASE + "/cliente/fcm-token").param("token"," ").header("Authorization", auth))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/otro/citas").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/otro/fcm-token").param("token", "dispositivo").header("Authorization", auth))
                .andExpect(status().isForbidden());
        mvc.perform(get(EMBUDO).header("Authorization", auth)).andExpect(status().isForbidden());
        verify(usuarios).actualizarFcmToken("cliente","dispositivo");
        verify(usuarios, never()).actualizarFcmToken(eq("otro"), anyString());
        verify(citas, never()).obtenerCitasPorUsuarioId("otro");
    }

    @Test void cerrarOVencerInvalidaLaCredencial() throws Exception {
        String auth = cliente();
        mvc.perform(post(BASE + "/salir").header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/sesion").header("Authorization", auth)).andExpect(status().isUnauthorized());
        String segunda = cliente();
        almacen.replaceAll((id, s) -> new SesionMovil(id, s.cuentaId(), s.tipo(), Instant.EPOCH));
        mvc.perform(get(BASE + "/sesion").header("Authorization", segunda)).andExpect(status().isUnauthorized());
    }

    @Test void trabajadorActivoPuedeVerEmbudoPeroNoRecursosDeCliente() throws Exception {
        Trabajador t = new Trabajador();
        t.setId("personal"); t.agregarRol(Rol.TRB_ANALISIS); t.setPassword(encoder.encode(clave));
        when(trabajadores.findByCorreo("personal@example.org")).thenReturn(Optional.of(t));
        when(trabajadores.findById("personal")).thenReturn(Optional.of(t));
        String auth = entrar("personal@example.org", "TRABAJADOR");
        mvc.perform(get(EMBUDO).header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get(BASE + "/personal/citas").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/personal/fcm-token").header("Authorization", auth)).andExpect(status().isForbidden());
        t.desactivar();
        mvc.perform(get(EMBUDO).header("Authorization", auth)).andExpect(status().isUnauthorized());
    }

    @Test void administradorNecesitaCuentaActivaYClaveCodificada() throws Exception {
        Administrador a = new Administrador();
        a.setId("admin"); a.setPasswordAdmin(clave);
        when(admins.findByCorreoAdmin("admin@example.org")).thenReturn(Optional.of(a));
        when(admins.findById("admin")).thenReturn(Optional.of(a));
        mvc.perform(post(BASE + "/login").param("correo", "admin@example.org").param("password", clave))
                .andExpect(status().isUnauthorized());
        a.setPasswordAdmin(encoder.encode(clave));
        String auth = entrar("admin@example.org", "ADMINISTRADOR");
        mvc.perform(get(EMBUDO).header("Authorization", auth)).andExpect(status().isOk());
        a.setActivo(false);
        mvc.perform(get(EMBUDO).header("Authorization", auth)).andExpect(status().isUnauthorized());
    }
}
