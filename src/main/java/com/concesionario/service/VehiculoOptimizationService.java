package com.concesionario.service;

import com.concesionario.dto.OptimizationRequest;
import com.concesionario.dto.OptimizationResult;
import com.concesionario.model.Vehiculo;
import com.concesionario.repository.VehiculoRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio que implementa el modelo de optimización matemática para recomendar vehículos.
 */
@Service
public class VehiculoOptimizationService {

    private final VehiculoRepository vehiculoRepository;

    public VehiculoOptimizationService(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    /**
     * Encuentra los k vehículos que maximizan el Índice de Satisfacción del usuario.
     */
    public List<OptimizationResult> buscarVehiculoIdeal(OptimizationRequest request) {
        List<Vehiculo> todos = vehiculoRepository.findAll();
        
        if (todos.isEmpty()) return Collections.emptyList();

        // 1. Filtrar por Restricciones Duras (Hard Constraints)
        List<Vehiculo> filtrados = todos.stream()
            .filter(v -> v.getPrecio() >= request.minPrecio() && v.getPrecio() <= request.maxPrecio())
            .filter(v -> v.getPasajeros() != null && v.getPasajeros() >= request.minPasajeros())
            .filter(v -> v.getAño() >= request.minAnio())
            .toList();

        if (filtrados.isEmpty()) return Collections.emptyList();

        // 2. Determinar Pesos Matemáticos (El programa decide basado en el uso)
        double wPrecio, wAño, wPasajeros, wCat, wComb;
        String uso = request.usoPrincipal() != null ? request.usoPrincipal() : "Equilibrado";
        
        switch (uso) {
            case "Deportivo" -> { wPrecio = 1.0; wAño = 4.5; wPasajeros = 0.5; wCat = 2.0; wComb = 2.0; }
            case "Familiar"  -> { wPrecio = 2.0; wAño = 1.0; wPasajeros = 5.0; wCat = 1.0; wComb = 1.0; }
            case "Trabajo"   -> { wPrecio = 4.0; wAño = 1.0; wPasajeros = 2.0; wCat = 2.0; wComb = 1.0; }
            case "Lujo"      -> { wPrecio = 0.5; wAño = 3.5; wPasajeros = 1.0; wCat = 3.0; wComb = 2.0; }
            default          -> { wPrecio = 3.0; wAño = 2.0; wPasajeros = 2.0; wCat = 1.5; wComb = 1.5; }
        }

        // 3. Obtener límites globales para normalización
        double minAñoCat = todos.stream().mapToInt(Vehiculo::getAño).min().orElse(2000);
        double maxAñoCat = todos.stream().mapToInt(Vehiculo::getAño).max().orElse(2025);
        double maxPasajerosCat = todos.stream()
            .mapToInt(v -> v.getPasajeros() != null ? v.getPasajeros() : 0)
            .max().orElse(5);

        // 4. Calcular scores
        return filtrados.stream()
            .map(v -> calculateResult(v, request, minAñoCat, maxAñoCat, maxPasajerosCat, 
                                    wPrecio, wAño, wPasajeros, wCat, wComb))
            .sorted(Comparator.comparing(OptimizationResult::satisfactionIndex).reversed())
            .limit(request.k() > 0 ? request.k() : 3)
            .toList();
    }

    private OptimizationResult calculateResult(Vehiculo v, OptimizationRequest req, 
                                            double minAño, double maxAño, double maxPas,
                                            double wPrecio, double wAño, double wPasajeros, 
                                            double wCat, double wComb) {
        
        double sPrecio = (req.maxPrecio() - v.getPrecio()) / (req.maxPrecio() - req.minPrecio() + 1);
        if (sPrecio < 0) sPrecio = 0;
        
        double rangeAnio = maxAño - minAño;
        double sAnio = rangeAnio > 0 ? (v.getAño() - minAño) / rangeAnio : 1.0;
        
        double sPasajeros = maxPas > 0 ? (v.getPasajeros() != null ? (double) v.getPasajeros() / maxPas : 0.0) : 1.0;
        
        // Match de Categoría (basado en el uso) + descripción
        boolean matchCat = false;
        if (v.getCategoria() != null) {
            String cat = v.getCategoria().toLowerCase();
            String desc = v.getDescripcion() != null ? v.getDescripcion().toLowerCase() : "";
            if ("Deportivo".equals(req.usoPrincipal())) matchCat = cat.contains("deportiv") || cat.contains("performance") || desc.contains("deportiv") || desc.contains("superdeportiv");
            else if ("Familiar".equals(req.usoPrincipal())) matchCat = cat.contains("suv") || cat.contains("familiar");
            else if ("Trabajo".equals(req.usoPrincipal())) matchCat = cat.contains("pick") || cat.contains("comercial");
            else if ("Lujo".equals(req.usoPrincipal())) matchCat = desc.contains("lujo") || desc.contains("premium") || desc.contains("exclusivo");
        }
        double sCat = matchCat ? 1.0 : 0.0;
        
        // Match de Combustible y Transmisión
        boolean matchComb = v.getCombustible() != null && req.prefCombustible() != null && 
                           v.getCombustible().equalsIgnoreCase(req.prefCombustible());
        
        boolean matchTrans = v.getTransmision() != null && req.prefTransmision() != null && 
                            v.getTransmision().equalsIgnoreCase(req.prefTransmision());
        
        double scorePreferencias = (matchComb ? 0.6 : 0.0) + (matchTrans ? 0.4 : 0.0);

        double index = (wPrecio * sPrecio) +
                       (wAño * sAnio) +
                       (wPasajeros * sPasajeros) +
                       (wCat * sCat) +
                       (wComb * scorePreferencias);

        return new OptimizationResult(v, index, sPrecio, sAnio, sPasajeros, matchCat, matchComb);
    }
}
