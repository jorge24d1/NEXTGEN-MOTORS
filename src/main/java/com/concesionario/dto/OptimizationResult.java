package com.concesionario.dto;

import com.concesionario.model.Vehiculo;

/**
 * Resultado del modelo de optimización con el desglose de puntajes.
 */
public record OptimizationResult(
    Vehiculo vehiculo,
    double satisfactionIndex,
    double scorePrecio,
    double scoreAnio,
    double scorePasajeros,
    boolean matchCategoria,
    boolean matchCombustible
) {}

