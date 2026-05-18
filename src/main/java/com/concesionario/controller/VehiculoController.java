package com.concesionario.controller;

import com.concesionario.model.Vehiculo;
import com.concesionario.service.VehiculoService;
import com.concesionario.service.CitaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.concesionario.service.VehiculoOptimizationService;
import com.concesionario.dto.OptimizationRequest;
import com.concesionario.dto.OptimizationResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class VehiculoController {
    private static final Logger log = LoggerFactory.getLogger(VehiculoController.class);
    private final VehiculoService vehiculoService;
    private final CitaService citaService;
    private final VehiculoOptimizationService optimizationService;


    // ✅ CONSTRUCTOR CORREGIDO - inicializar TODOS los servicios
    public VehiculoController(VehiculoService vehiculoService,
                              CitaService citaService,
                              VehiculoOptimizationService optimizationService
                              ) {
        this.vehiculoService = vehiculoService;
        this.citaService = citaService;
        this.optimizationService = optimizationService;

    }

    @GetMapping("/vehiculos")
    public String mostrarVehiculos(Model model) {
        List<Vehiculo> vehiculos = vehiculoService.obtenerTodos();

        // Solución 1: Filtrar vehículos sin categoría
        Map<String, List<Vehiculo>> vehiculosPorCategoria = vehiculos.stream()
                .filter(v -> v.getCategoria() != null && !v.getCategoria().isEmpty())
                .collect(Collectors.groupingBy(Vehiculo::getCategoria));

        model.addAttribute("categorias", vehiculosPorCategoria.keySet());
        model.addAttribute("vehiculosPorCategoria", vehiculosPorCategoria);
        return "vehiculos";
    }

    @GetMapping("/fragments/chatbot")
    public String Inicio(Model model) {
//
        return "chatbot";
    }





    @GetMapping("/vehiculos/explorar/{id}")
    public String explorarVehiculo(@PathVariable String id, Model model) {
        Vehiculo vehiculo = vehiculoService.obtenerPorId(id);
        if (vehiculo == null) {
            return "redirect:/vehiculos";
        }
        model.addAttribute("vehiculo", vehiculo);
        return "explorar-vehiculo";
    }
    @GetMapping("/")
    public String redirectToInicio() {
        return "redirect:/usuario/Inicio";
    }

    @GetMapping("/nosotros")
    public String nosotros(){
        return "nosotros";
    }
    @GetMapping("/garantias")
    public String garantias(){
        return "garantias";
    }
    @GetMapping("/credito")
    public String credito(){
        return "credito";
    }

    @GetMapping("/cookies")
    public String cookies(){
        return "cookies";
    }
    @GetMapping("/terminos")
    public String terminos(){
        return "terminos";
    }
    @GetMapping("/ubicaciones")
    public String ubicaciones(){
        return "ubicaciones";
    }

    @GetMapping("/vehiculo-ideal")
    public String buscarVehiculoIdeal() {
        return "vehiculo_ideal";
    }

    @PostMapping("/vehiculo-ideal")
    public String procesarVehiculoIdeal(
            @RequestParam(name = "minPrecio",      defaultValue = "0")          double minPrecio,
            @RequestParam(name = "maxPrecio",      defaultValue = "999999999")  double maxPrecio,
            @RequestParam(name = "minPasajeros",   defaultValue = "1")          int    minPasajeros,
            @RequestParam(name = "minAnio",        defaultValue = "2015")       int    minAnio,
            @RequestParam(name = "prefTransmision",defaultValue = "")           String prefTransmision,
            @RequestParam(name = "prefCombustible",defaultValue = "")           String prefCombustible,
            @RequestParam(name = "usoPrincipal",   defaultValue = "Equilibrado")String usoPrincipal,
            Model model) {
        try {
            log.info("[VehiculoIdeal] uso={} precio={}-{} pas={} anio={} trans={} comb={}",
                usoPrincipal, minPrecio, maxPrecio, minPasajeros, minAnio, prefTransmision, prefCombustible);

            OptimizationRequest request = new OptimizationRequest(
                minPrecio, maxPrecio, minPasajeros, minAnio,
                prefTransmision.isBlank() ? null : prefTransmision,
                prefCombustible.isBlank() ? null : prefCombustible,
                usoPrincipal, 3
            );

            List<OptimizationResult> resultados = optimizationService.buscarVehiculoIdeal(request);
            log.info("[VehiculoIdeal] Encontrados {} resultados", resultados.size());
            model.addAttribute("resultados", resultados);
            model.addAttribute("preferencias", request);
            return "resultados_ideal";

        } catch (Exception e) {
            log.error("[VehiculoIdeal] ERROR: {}", e.getMessage(), e);
            model.addAttribute("resultados", java.util.Collections.emptyList());
            model.addAttribute("preferencias", null);
            return "resultados_ideal";
        }
    }

}