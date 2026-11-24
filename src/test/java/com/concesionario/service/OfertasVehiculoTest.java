package com.concesionario.service;

import com.concesionario.model.*;
import com.concesionario.repository.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OfertasVehiculoTest {
    private final VehiculoRepository vehiculos = mock(VehiculoRepository.class);
    private final EnvioOfertaRepository envios = mock(EnvioOfertaRepository.class);
    private final BrevoOfertas brevo = mock(BrevoOfertas.class);
    private final OfertasVehiculo ofertas = new OfertasVehiculo(vehiculos, envios, brevo, "https://tienda.example");
    private final UUID solicitud = UUID.randomUUID();
    private final Vehiculo vehiculo = new Vehiculo("Marca", "Auto", 2025, 25000, "SUV", "/foto.jpg");

    @Test void contenidoDelVehiculoConfirmadoConFotoPrecioYBaja() {
        vehiculo.setId("v1");
        vehiculo.setMotor("<eléctrico>");
        when(vehiculos.findById("v1")).thenReturn(Optional.of(vehiculo));
        when(brevo.destinatarios()).thenReturn(1);
        when(brevo.crearBorrador(anyString(), contains("Marca Auto"), anyString())).thenReturn(7L);
        assertEquals(7, ofertas.enviar("asesor", "v1", solicitud, 1, false).campana());
        var html = ArgumentCaptor.forClass(String.class);
        verify(brevo).crearBorrador(anyString(), anyString(), html.capture());
        for (String parte : new String[]{"https://tienda.example/vehiculos/explorar/v1", "https://tienda.example/foto.jpg",
                "25.000", "COP", "2025", "&lt;", "{{ unsubscribe }}"}) assertTrue(html.getValue().contains(parte), parte);
        verify(brevo).enviar(7L);
    }

    @Test void repetirConfirmacionNoEnviaDeNuevoYNoPermiteConsultarLaDeOtroAsesor() {
        var envio = new EnvioOferta(solicitud.toString(), "ana", "v1", Instant.now(), 8L, EnvioOferta.Estado.ACEPTADO, true);
        when(envios.findById(solicitud.toString())).thenReturn(Optional.of(envio));
        assertEquals(8, ofertas.enviar("ana", "v1", solicitud, 1, true).campana());
        assertThrows(ResponseStatusException.class, () -> ofertas.enviar("otro", "v1", solicitud, 1, true));
        assertThrows(ResponseStatusException.class, () -> ofertas.enviar("ana", "v2", solicitud, 1, true));
        verifyNoInteractions(brevo, vehiculos);
    }

    @Test void sinVehiculoSinDestinatariosOCambioDeModoNoSeCreaCampana() {
        assertThrows(ResponseStatusException.class, () -> ofertas.enviar("ana", "v1", solicitud, 1, true));
        when(vehiculos.findById("v1")).thenReturn(Optional.of(vehiculo));
        assertThrows(ResponseStatusException.class, () -> ofertas.enviar("ana", "v1", solicitud, 1, false));
        when(brevo.destinatarios()).thenReturn(1);
        assertThrows(ResponseStatusException.class, () -> ofertas.enviar("ana", "v1", solicitud, 1, true));
        verify(brevo, never()).crearBorrador(anyString(), anyString(), anyString());
    }
}
