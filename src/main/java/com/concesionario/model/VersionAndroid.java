package com.concesionario.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("versiones_android")
public record VersionAndroid(@Id Integer codigo, String version, String novedades, Instant publicada) {}
