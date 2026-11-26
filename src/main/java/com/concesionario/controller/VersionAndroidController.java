package com.concesionario.controller;

import com.concesionario.dto.FichaAndroid;
import com.concesionario.service.VersionesAndroid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VersionAndroidController {
    private final VersionesAndroid versiones;
    public VersionAndroidController(VersionesAndroid versiones) { this.versiones = versiones; }

    @GetMapping("/api/movil/version")
    @ResponseBody
    public ResponseEntity<FichaAndroid> vigente() {
        return versiones.vigente().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/descargar-app")
    public String descarga(Model modelo) {
        modelo.addAttribute("version", versiones.vigente().orElse(null));
        return "descargar-app";
    }

    @GetMapping("/admin/versiones-app")
    public String administrar(Model modelo) {
        modelo.addAttribute("versiones", versiones.listar());
        return "admin/versiones-app";
    }

    @PostMapping("/admin/versiones-app")
    public String crear(@RequestParam int codigo, @RequestParam String version, @RequestParam String novedades,
                        RedirectAttributes salida) {
        try {
            versiones.crear(codigo, version, novedades);
            salida.addFlashAttribute("aviso", "Borrador guardado.");
        } catch (IllegalArgumentException e) { salida.addFlashAttribute("aviso", e.getMessage()); }
        return "redirect:/admin/versiones-app";
    }

    @PostMapping("/admin/versiones-app/{codigo}/publicar")
    public String publicar(@PathVariable int codigo, RedirectAttributes salida) {
        try {
            versiones.publicar(codigo);
            salida.addFlashAttribute("aviso", "Versión publicada.");
        } catch (IllegalArgumentException e) { salida.addFlashAttribute("aviso", e.getMessage()); }
        return "redirect:/admin/versiones-app";
    }
}
