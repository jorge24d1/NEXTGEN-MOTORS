package com.concesionario.controller;

import com.concesionario.model.Reunion;
import com.concesionario.service.ReunionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/reuniones")
public class ReunionController {

    @Autowired
    private ReunionService reunionService;

    @GetMapping
    public List<Reunion> listar() {
        return reunionService.listarTodas();
    }

    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody Reunion reunion) {
        try {
            Reunion guardada = reunionService.guardar(reunion);
            return ResponseEntity.ok(guardada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al guardar la reunión: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            reunionService.eliminar(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar la reunión: " + e.getMessage());
        }
    }
}
