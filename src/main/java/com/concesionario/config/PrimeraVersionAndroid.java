package com.concesionario.config;

import com.concesionario.repository.VersionAndroidRepository;
import com.concesionario.service.VersionesAndroid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "render", havingValue = "true")
public class PrimeraVersionAndroid {
    private static final Logger log = LoggerFactory.getLogger(PrimeraVersionAndroid.class);
    private final VersionAndroidRepository repo;
    private final VersionesAndroid versiones;

    public PrimeraVersionAndroid(VersionAndroidRepository repo, VersionesAndroid versiones) {
        this.repo = repo;
        this.versiones = versiones;
    }

    // Alta inicial en Render. Las siguientes publicaciones se gestionan desde el panel.
    @EventListener(ApplicationReadyEvent.class)
    public void preparar() {
        try {
            if (versiones.vigente().isPresent()) return;
            if (!repo.existsById(1)) {
                versiones.crear(1, "1.0.0", "Consulta tus citas, conversa con Dante y recibe avisos del concesionario.");
            }
            versiones.publicar(1);
        } catch (RuntimeException ex) {
            log.warn("La primera descarga Android queda pendiente de publicación en el panel.");
        }
    }
}
