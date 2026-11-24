package com.concesionario.dto;

import java.time.Instant;

public record LoginMovil(boolean success, String userId, String nombre, String role, String token, Instant vence) {
    // Spring MVC puede registrar el valor de retorno con el nivel DEBUG.
    @Override public String toString() { return "LoginMovil[sesión emitida]"; }
}
