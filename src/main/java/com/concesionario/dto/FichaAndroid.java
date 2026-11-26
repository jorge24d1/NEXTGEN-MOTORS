package com.concesionario.dto;

import java.time.Instant;

public record FichaAndroid(int codigo, String version, String novedades, Instant publicada, String descarga) {}
