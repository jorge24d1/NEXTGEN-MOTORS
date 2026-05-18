package com.concesionario.dto;

/**
 * Parámetros para el modelo de optimización de búsqueda del vehículo ideal.
 */
public record OptimizationRequest(
    double minPrecio,
    double maxPrecio,
    int minPasajeros,
    int minAnio,
    String prefTransmision,
    String prefCombustible,
    String usoPrincipal,  // "Deportivo", "Familiar", "Trabajo", "Económico"
    int k
) {}
