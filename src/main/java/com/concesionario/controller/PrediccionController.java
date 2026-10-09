package com.concesionario.controller;

import com.concesionario.model.Usuario;
import com.concesionario.service.PrediccionService;
import com.concesionario.service.UsuarioService;
import com.concesionario.repository.CitaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PrediccionController {

    @Autowired
    private PrediccionService prediccionService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CitaRepository citaRepository;

    // Método para cargar usuarios via AJAX
    @GetMapping("/cargar-usuarios")
    @ResponseBody
    public List<Usuario> cargarUsuarios() {
        List<Usuario> usuarios = usuarioService.findAll();
        for (Usuario usuario : usuarios) {
            calcularDatosDesdeCitas(usuario);
        }
        return usuarios;
    }

    // Método para predecir via AJAX
    @PostMapping("/predecir-usuario")
    @ResponseBody
    public Map<String, Object> predecirUsuario(@RequestParam String usuarioId) {
        Map<String, Object> response = new HashMap<>();

        Usuario usuario = usuarioService.findById(usuarioId);
        if (usuario != null) {
            calcularDatosDesdeCitas(usuario);

            String resultado = prediccionService.predecir(
                    usuario.getCantidadCitas().doubleValue(),
                    usuario.getAntiguedadCuenta().doubleValue(),
                    usuario.getEstadoUltimaCita(),
                    usuario.getInteresVehiculo(),
                    usuario.getTiempoEntreCitas().doubleValue()
            );

            double probabilidad = prediccionService.obtenerProbabilidadSi(
                    usuario.getCantidadCitas().doubleValue(),
                    usuario.getAntiguedadCuenta().doubleValue(),
                    usuario.getEstadoUltimaCita(),
                    usuario.getInteresVehiculo(),
                    usuario.getTiempoEntreCitas().doubleValue()
            );

            usuario.setClientePotencial(resultado);
            usuario.setProbabilidad(probabilidad);
            usuario.setObservaciones(resultado.equals("Si") ? "Cliente potencial - Seguimiento recomendado" : "Requiere estrategias adicionales");

            usuarioService.save(usuario);

            response.put("success", true);
            response.put("usuario", usuario);
        } else {
            response.put("success", false);
            response.put("error", "Usuario no encontrado");
        }

        return response;
    }

    // Métodos existentes (los mantienes por si acaso)
    @GetMapping("/prediccion")
    public String mostrarPrediccion(@RequestParam(required = false) Boolean cargarUsuarios, Model model) {
        if (cargarUsuarios != null && cargarUsuarios) {
            List<Usuario> usuarios = usuarioService.findAll();
            for (Usuario usuario : usuarios) {
                calcularDatosDesdeCitas(usuario);
            }
            model.addAttribute("usuarios", usuarios);
        }
        return "prediccion";
    }

    @PostMapping("/predecir-usuario-old")
    public String predecirUsuarioOld(@RequestParam String usuarioId, Model model) {
        Usuario usuario = usuarioService.findById(usuarioId);
        if (usuario != null) {
            calcularDatosDesdeCitas(usuario);

            String resultado = prediccionService.predecir(
                    usuario.getCantidadCitas().doubleValue(),
                    usuario.getAntiguedadCuenta().doubleValue(),
                    usuario.getEstadoUltimaCita(),
                    usuario.getInteresVehiculo(),
                    usuario.getTiempoEntreCitas().doubleValue()
            );

            double probabilidad = prediccionService.obtenerProbabilidadSi(
                    usuario.getCantidadCitas().doubleValue(),
                    usuario.getAntiguedadCuenta().doubleValue(),
                    usuario.getEstadoUltimaCita(),
                    usuario.getInteresVehiculo(),
                    usuario.getTiempoEntreCitas().doubleValue()
            );

            usuario.setClientePotencial(resultado);
            usuario.setProbabilidad(probabilidad);
            usuario.setObservaciones(resultado.equals("Si") ? "Cliente potencial - Seguimiento recomendado" : "Requiere estrategias adicionales");

            usuarioService.save(usuario);
            model.addAttribute("usuarioSeleccionado", usuario);
        }

        List<Usuario> usuarios = usuarioService.findAll();
        for (Usuario u : usuarios) {
            calcularDatosDesdeCitas(u);
        }
        model.addAttribute("usuarios", usuarios);

        return "prediccion";
    }

    // ====================================================
    // ENDPOINT PARA APP MOVIL - Datos del Embudo Predictivo
    // ====================================================
    @CrossOrigin(origins = "*")
    @GetMapping("/api/prediccion/embudo")
    @ResponseBody
    public Map<String, Object> getEmbudoData() {
        List<Usuario> usuarios = usuarioService.findAll();
        for (Usuario u : usuarios) {
            calcularDatosDesdeCitas(u);
        }

        long total = usuarios.size();
        long conInteres = usuarios.stream().filter(u -> "Si".equals(u.getInteresVehiculo())).count();
        long citaAprobada = usuarios.stream().filter(u -> "Aprobada".equals(u.getEstadoUltimaCita())).count();
        long clientePotencial = usuarios.stream().filter(u -> "Si".equals(u.getClientePotencial())).count();

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("conInteres", conInteres);
        data.put("citaAprobada", citaAprobada);
        data.put("clientePotencial", clientePotencial);
        return data;
    }
    private void calcularDatosDesdeCitas(Usuario usuario) {
        var citas = citaRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId());

        // Calcular antigüedad basada en la fecha de creación del usuario
        LocalDateTime fechaCreacionUsuario = usuario.getFechaCreacion();
        if (fechaCreacionUsuario == null) {
            fechaCreacionUsuario = LocalDateTime.now(); // Valor por defecto
        }

        long antiguedad = ChronoUnit.DAYS.between(fechaCreacionUsuario, LocalDateTime.now());
        usuario.setAntiguedadCuenta((int) Math.max(0, antiguedad)); // Asegurar que no sea negativo

        if (citas.isEmpty()) {
            usuario.setCantidadCitas(0);
            usuario.setEstadoUltimaCita("Pendiente");
            usuario.setInteresVehiculo("No");
            usuario.setTiempoEntreCitas(0);
            return;
        }

        usuario.setCantidadCitas(citas.size());

        var ultimaCita = citas.get(0);
        usuario.setEstadoUltimaCita(ultimaCita.getEstado());

        String interes = "No";
        if (citas.size() >= 2 || "Aprobada".equals(ultimaCita.getEstado())) {
            interes = "Si";
        }
        usuario.setInteresVehiculo(interes);

        if (citas.size() > 1) {
            long totalDias = 0;
            for (int i = citas.size() - 1; i > 0; i--) {
                long dias = ChronoUnit.DAYS.between(citas.get(i).getFechaCreacion(), citas.get(i - 1).getFechaCreacion());
                totalDias += Math.abs(dias);
            }
            usuario.setTiempoEntreCitas((int) (totalDias / (citas.size() - 1)));
        } else {
            usuario.setTiempoEntreCitas(30);
        }
    }
}
