package com.concesionario.service;

import com.concesionario.dto.FichaAndroid;
import com.concesionario.model.VersionAndroid;
import com.concesionario.repository.VersionAndroidRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class VersionesAndroid {
    private final VersionAndroidRepository repo;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public VersionesAndroid(VersionAndroidRepository repo) { this.repo = repo; }

    public Optional<FichaAndroid> vigente() {
        return repo.findFirstByPublicadaNotNullOrderByCodigoDesc().map(this::ficha);
    }

    public List<FichaAndroid> listar() {
        return repo.findTop30ByOrderByCodigoDesc().stream().map(this::ficha).toList();
    }

    private FichaAndroid ficha(VersionAndroid v) {
        String url = "https://github.com/jorge24d1/NEXTGEN-MOTORS_MOBILE/releases/download/android-"
                + v.codigo() + "/nextgen-motors.apk";
        return new FichaAndroid(v.codigo(), v.version(), v.novedades(), v.publicada(), url);
    }

    public void crear(int codigo, String version, String novedades) {
        if (codigo <= 0 || !version.matches("[0-9]{1,5}\\.[0-9]{1,5}\\.[0-9]{1,5}")
                || novedades.isBlank() || novedades.length() > 1000) {
            throw new IllegalArgumentException("Revisa el código, la versión y las novedades (máximo 1000 caracteres).");
        }
        validarPosterior(codigo);
        try {
            repo.insert(new VersionAndroid(codigo, version, novedades.strip(), null));
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Ya existe ese código de compilación.");
        }
    }

    private void validarPosterior(int codigo) {
        if (vigente().filter(v -> v.codigo() >= codigo).isPresent()) {
            throw new IllegalArgumentException("El código debe superar al de la versión vigente.");
        }
    }

    public void publicar(int codigo) {
        var v = repo.findById(codigo).orElseThrow(() -> new IllegalArgumentException("El borrador no existe."));
        validarPosterior(codigo);
        var peticion = HttpRequest.newBuilder(URI.create(ficha(v).descarga()))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).timeout(Duration.ofSeconds(8)).build();
        try {
            var respuesta = http.send(peticion, HttpResponse.BodyHandlers.discarding());
            boolean disponible = respuesta.statusCode() == 302 && respuesta.headers().firstValue("Location")
                    .filter(url -> url.startsWith("https://release-assets.githubusercontent.com/")).isPresent();
            if (!disponible) throw new IllegalArgumentException("La APK todavía no está disponible en el release público.");
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo comprobar la descarga. Vuelve a intentarlo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Comprobación interrumpida.");
        }
        if (repo.publicarBorrador(codigo, Instant.now()) != 1) {
            throw new IllegalArgumentException("El borrador ya se publicó.");
        }
    }
}
