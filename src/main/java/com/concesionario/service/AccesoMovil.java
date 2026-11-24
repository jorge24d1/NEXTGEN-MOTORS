package com.concesionario.service;

import com.concesionario.dto.PerfilMovil;
import com.concesionario.dto.LoginMovil;
import com.concesionario.model.*;
import com.concesionario.model.SesionMovil.TipoCuenta;
import com.concesionario.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccesoMovil {
    private static final Duration DURACION = Duration.ofHours(8);
    private static final SecureRandom AZAR = new SecureRandom();
    private final UsuarioRepository usuarios;
    private final AdministradorRepository administradores;
    private final TrabajadorRepository trabajadores;
    private final SesionMovilRepository sesiones;
    private final PasswordEncoder claves;

    public AccesoMovil(UsuarioRepository usuarios, AdministradorRepository administradores,
                       TrabajadorRepository trabajadores, SesionMovilRepository sesiones, PasswordEncoder claves) {
        this.usuarios = usuarios;
        this.administradores = administradores;
        this.trabajadores = trabajadores;
        this.sesiones = sesiones;
        this.claves = claves;
    }

    public Optional<LoginMovil> iniciar(String correo, String password) {
        if (correo.isBlank() || password.isBlank()) return Optional.empty();
        String email = correo.trim();
        var usuario = usuarios.findByCorreoUser(email);
        if (usuario.isPresent()) {
            return usuario.filter(u -> claves.matches(password, u.getPasswordUser()))
                    .flatMap(this::perfil).map(p -> emitir(p, TipoCuenta.CLIENTE));
        }
        var admin = administradores.findByCorreoAdmin(email);
        if (admin.isPresent()) {
            return admin.filter(a -> claves.matches(password, a.getPasswordAdmin()))
                    .flatMap(this::perfil).map(p -> emitir(p, TipoCuenta.ADMINISTRADOR));
        }
        return trabajadores.findByCorreo(email).filter(t -> claves.matches(password, t.getPassword()))
                .flatMap(this::perfil).map(p -> emitir(p, TipoCuenta.TRABAJADOR));
    }

    private LoginMovil emitir(PerfilMovil perfil, TipoCuenta tipo) {
        byte[] bytes = new byte[32];
        AZAR.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant vence = Instant.now().plus(DURACION);
        sesiones.insert(new SesionMovil(huella(token), perfil.userId(), tipo, vence));
        return new LoginMovil(true, perfil.userId(), perfil.nombre(), perfil.role(), token, vence);
    }

    public Optional<PerfilMovil> identificar(String token) {
        if (token.length() != 43) return Optional.empty();
        return sesiones.findById(huella(token)).filter(s -> s.vence().isAfter(Instant.now()))
                .flatMap(s -> switch (s.tipo()) {
                    case CLIENTE -> usuarios.findById(s.cuentaId()).flatMap(this::perfil);
                    case ADMINISTRADOR -> administradores.findById(s.cuentaId()).flatMap(this::perfil);
                    case TRABAJADOR -> trabajadores.findById(s.cuentaId()).flatMap(this::perfil);
                });
    }

    public void cerrar(String token) {
        sesiones.deleteById(huella(token));
    }

    private Optional<PerfilMovil> perfil(Usuario u) {
        return Optional.of(u).filter(c -> c.getRol() == Rol.USUARIO)
                .map(c -> new PerfilMovil(c.getId(), nombre(c.getNombreUser(), c.getApellidoUser()), "CLIENTE"));
    }

    private Optional<PerfilMovil> perfil(Administrador a) {
        return Optional.of(a).filter(c -> c.isActivo() && c.tieneRol(Rol.ADMINISTRADOR))
                .map(c -> new PerfilMovil(c.getId(), nombre(c.getNombreAdmin(), c.getApellidoAdmin()), "ADMINISTRADOR"));
    }

    private Optional<PerfilMovil> perfil(Trabajador t) {
        if (!t.isActivo()) return Optional.empty();
        String rol;
        if (t.tieneRol(Rol.ADMINISTRADOR)) rol = "ADMINISTRADOR";
        else if (t.tieneAlgunRol(Rol.TRABAJADOR, Rol.TRB_ANALISIS, Rol.TRB_GESTOR, Rol.TRB_ASESOR)) rol = "TRABAJADOR";
        else return Optional.empty();
        return Optional.of(new PerfilMovil(t.getId(), nombre(t.getNombre(), t.getApellido()), rol));
    }

    private static String nombre(String nombre, String apellido) {
        return ((nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido)).trim();
    }

    private static String huella(String token) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
