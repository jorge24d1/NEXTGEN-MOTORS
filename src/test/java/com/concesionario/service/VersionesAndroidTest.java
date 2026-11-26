package com.concesionario.service;

import com.concesionario.model.VersionAndroid;
import com.concesionario.repository.VersionAndroidRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VersionesAndroidTest {
    private final VersionAndroidRepository repo = mock(VersionAndroidRepository.class);
    private final VersionesAndroid versiones = new VersionesAndroid(repo);

    @Test void unBorradorNoCambiaLaVersionVigente() {
        versiones.crear(1, "1.0.0", " Primera versión ");
        verify(repo).insert(new VersionAndroid(1, "1.0.0", "Primera versión", null));
        assertTrue(versiones.vigente().isEmpty());
    }

    @Test void rechazaCompilacionesAntiguasOCamposInvalidos() {
        when(repo.findFirstByPublicadaNotNullOrderByCodigoDesc())
                .thenReturn(Optional.of(new VersionAndroid(4, "1.2.0", "Cambios", Instant.EPOCH)));
        assertThrows(IllegalArgumentException.class, () -> versiones.crear(4, "1.0.0", "Antigua"));
        assertThrows(IllegalArgumentException.class, () -> versiones.crear(5, "x", "Sin versión"));
        assertThrows(IllegalArgumentException.class, () -> versiones.crear(5, "1.2.1", " "));
        assertEquals("https://github.com/jorge24d1/NEXTGEN-MOTORS_MOBILE/releases/download/android-4/nextgen-motors.apk",
                versiones.vigente().orElseThrow().descarga());
    }
}
