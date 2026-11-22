package com.concesionario.controller;

import com.concesionario.model.*;
import com.concesionario.repository.*;
import com.concesionario.service.*;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerfilCitaTest {
    @Mock UsuarioRepository usuarios;
    @Mock UsuarioService perfiles;
    @Mock TrabajadorRepository trabajadores;
    @Mock CitaService citas;
    @Mock VehiculoService vehiculos;
    @Mock PasswordEncoder encoder;
    @InjectMocks UsuarioController controller;

    final Usuario usuario = new Usuario();
    final Principal principal = () -> "cliente@example.com";
    final MockHttpSession session = new MockHttpSession();
    final RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    @BeforeEach
    void prepararUsuario() {
        usuario.setCorreoUser(principal.getName());
        usuario.setIdentificacionUser("123");
        usuario.setPasswordUser("hash original");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void retomaElVehiculoYCompletaElApellidoUnaVez(String apellidoAnterior) {
        usuario.setApellidoUser(apellidoAnterior);
        when(usuarios.findByCorreoUser(principal.getName())).thenReturn(Optional.of(usuario));
        when(perfiles.findByCorreoUser(principal.getName())).thenReturn(usuario);
        assertEquals("redirect:/usuario/completar-perfil", controller.mostrarFormularioCita(
                "v1", "Garantías", "Azul", principal, new ExtendedModelMap(), session));

        assertEquals("redirect:/usuario/completar-perfil", controller.guardarPerfilCompleto(
                "  ", null, null, principal, session, redirect));
        verify(usuarios, never()).save(any());
        assertEquals("redirect:/usuario/cita", controller.guardarPerfilCompleto(
                "  Del Río ", "otra", "otra", principal, session, redirect));
        assertEquals("Del Río", usuario.getApellidoUser());
        assertEquals("123", usuario.getIdentificacionUser());
        assertEquals("hash original", usuario.getPasswordUser());

        ExtendedModelMap modelo = new ExtendedModelMap();
        modelo.addAllAttributes(redirect.getFlashAttributes());
        assertEquals("cita", controller.mostrarFormularioCita(null, null, null, principal, modelo, session));
        Cita cita = (Cita) modelo.get("cita");
        assertEquals("v1", cita.getVehiculoId());
        assertEquals("Garantías", cita.getTipo());
        assertEquals("Azul", cita.getColorVehiculo());
        assertEquals("Del Río", cita.getApellidos());
        assertEquals("redirect:/usuario/perfil", controller.guardarPerfilCompleto(
                "otro", null, null, principal, session, redirect));
        verify(usuarios).save(usuario);
        verifyNoInteractions(encoder);
    }

    @Test
    void cuentaNuevaCompletaTodosLosCamposPendientes() {
        usuario.setIdentificacionUser(null);
        usuario.setPasswordUser(null);
        when(perfiles.findByCorreoUser(principal.getName())).thenReturn(usuario);
        when(encoder.encode("clave")).thenReturn("hash nuevo");
        assertEquals("redirect:/usuario/perfil", controller.guardarPerfilCompleto(
                "Ruiz", " 123 ", "clave", principal, session, redirect));
        assertFalse(usuario.perfilIncompleto());
        assertEquals("123", usuario.getIdentificacionUser());
        assertEquals("hash nuevo", usuario.getPasswordUser());
        verify(usuarios).save(usuario);
    }

    @Test
    void envioDirectoConservaElBorradorYUsaLaIdentidadDelPerfilAlGuardar() {
        when(usuarios.findByCorreoUser(principal.getName())).thenReturn(Optional.of(usuario));
        when(perfiles.findByCorreoUser(principal.getName())).thenReturn(usuario);
        Cita formulario = new Cita();
        formulario.setTipo("Otros");
        formulario.setTelefono("3001234567");
        formulario.setTrabajadorId("t1");
        formulario.setComentario("Consulta");
        assertEquals("redirect:/usuario/completar-perfil", controller.guardarCita(formulario, principal, session, redirect));
        verifyNoInteractions(citas, trabajadores);
        assertEquals("redirect:/usuario/cita", controller.guardarPerfilCompleto("Ruiz", null, null, principal, session, redirect));
        ExtendedModelMap modelo = new ExtendedModelMap();
        modelo.addAllAttributes(redirect.getFlashAttributes());
        assertEquals("cita", controller.mostrarFormularioCita(null, null, null, principal, modelo, session));
        Cita borrador = (Cita) modelo.get("cita");
        assertEquals("Consulta", borrador.getComentario());
        assertEquals("3001234567", borrador.getTelefono());
        assertEquals("t1", borrador.getTrabajadorId());
        borrador.setApellidos("valor del formulario");
        when(trabajadores.findById("t1")).thenReturn(Optional.of(new Trabajador()));
        assertTrue(controller.guardarCita(borrador, principal, session, redirect).startsWith("redirect:/usuario/perfil?success"));
        verify(citas).guardarCitaSimple(borrador, usuario, null);
        assertEquals("Ruiz", borrador.getApellidos());
    }
}
