package com.concesionario.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("sesiones_moviles")
public record SesionMovil(@Id String huella, String cuentaId, TipoCuenta tipo,
                         Instant vence) {
    public enum TipoCuenta { CLIENTE, ADMINISTRADOR, TRABAJADOR }
}
