package com.concesionario.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("enviosOfertas")
public record EnvioOferta(@Id String id, String asesor, String vehiculo, Instant creado,
                         Long campana, Estado estado, boolean prueba) {
    public enum Estado { PREPARANDO, BORRADOR, ACEPTADO, REVISAR }

    public EnvioOferta conEstado(Long campana, Estado estado) {
        return new EnvioOferta(id, asesor, vehiculo, creado, campana, estado, prueba);
    }
}
