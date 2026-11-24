package com.concesionario.service;

import com.concesionario.controller.PasswordResetController;
import com.concesionario.model.Administrador;
import com.concesionario.model.Usuario;
import com.concesionario.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetCorreoTest {
    @Mock UsuarioRepository usuarios;
    @Mock TrabajadorRepository trabajadores;
    @Mock AdministradorRepository administradores;
    @Mock CorreoRecuperacion correo;
    @InjectMocks PasswordResetService servicio;

    @Test
    void solicitudAnonimaEntregaElTokenGuardadoSoloAlTitular() {
        ReflectionTestUtils.setField(servicio, "configuredBaseUrl", "https://example.org/");
        Usuario titular = new Usuario();
        titular.setCorreoUser("titular@example.org");
        when(usuarios.findByCorreoUser(titular.getCorreoUser())).thenReturn(Optional.of(titular));
        servicio.initiatePasswordReset(titular.getCorreoUser());
        verify(usuarios).save(titular);
        assertNotNull(titular.getResetPasswordToken());
        verify(correo).enviar(eq(titular.getCorreoUser()), anyString(),
                contains("https://example.org/auth/reset-password?token=" + titular.getResetPasswordToken()));
        verifyNoInteractions(trabajadores, administradores);
    }

    @Test
    void cuentaDesconocidaYFalloDeEntregaDanLaMismaRespuestaPublica() {
        PasswordResetController controller = new PasswordResetController();
        ReflectionTestUtils.setField(controller, "passwordResetService", servicio);
        var desconocida = new RedirectAttributesModelMap();
        assertEquals("redirect:/login", controller.processForgotPassword("otro@example.org", desconocida));
        verifyNoInteractions(correo);
        Administrador admin = new Administrador();
        admin.setCorreoAdmin("admin@example.org");
        when(administradores.findByCorreoAdmin(admin.getCorreoAdmin())).thenReturn(Optional.of(admin));
        doThrow(new IllegalStateException("detalle interno")).when(correo).enviar(anyString(), anyString(), anyString());
        var registrada = new RedirectAttributesModelMap();
        assertEquals("redirect:/login", controller.processForgotPassword(admin.getCorreoAdmin(), registrada));
        assertEquals(desconocida.getFlashAttributes(), registrada.getFlashAttributes());
        assertFalse(registrada.getFlashAttributes().toString().contains("detalle interno"));
    }

    @Test
    void sinTokenValidoNoSePuedeModificarUnaCuentaDeNingunRol() {
        assertThrows(IllegalArgumentException.class, () -> servicio.resetPassword(" ", "nueva"));
        verifyNoInteractions(usuarios, trabajadores, administradores);
        assertThrows(RuntimeException.class, () -> servicio.resetPassword("desconocido", "nueva"));
        verify(usuarios, never()).save(any());
        verify(trabajadores, never()).save(any());
        verify(administradores, never()).save(any());
    }
}
