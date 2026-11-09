package com.concesionario.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class PrediccionService {

    private static final Logger log = LoggerFactory.getLogger(PrediccionService.class);

    private final RestClient restClient;
    private final String apiUrl;

    public PrediccionService(@Value("${prediccion.api.url:http://localhost:8000}") String apiUrl) {
        this.apiUrl = apiUrl;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .build();
        log.info("✅ PrediccionService conectado a FastAPI en: {}", apiUrl);
    }

    /**
     * Consulta el microservicio FastAPI (/predecir) enviando las variables en un Map simple.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> consultarApi(double citas, double antiguedad, String estado, String interes, double tiempo) {
        return consultarApi(citas, antiguedad, estado, interes, tiempo, "No", "No", "No", "No");
    }

    /**
     * Consulta el microservicio FastAPI (/predecir) incluyendo las nuevas variables comerciales y financieras.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> consultarApi(double citas, double antiguedad, String estado, String interes, double tiempo,
                                           String asistioCita, String tieneCuotaInicial, String capacidadEndeudamiento, String capacidadFinanciacion) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("cantidad_citas", citas);
            body.put("antiguedad_cuenta", antiguedad);
            body.put("estado_ultima_cita", estado);
            body.put("interes_vehiculo", interes);
            body.put("tiempo_entre_citas", tiempo);

            // Nuevas variables post-cita
            body.put("asistio_cita", asistioCita != null ? asistioCita : "No");
            body.put("tiene_cuota_inicial", tieneCuotaInicial != null ? tieneCuotaInicial : "No");
            body.put("capacidad_endeudamiento", capacidadEndeudamiento != null ? capacidadEndeudamiento : "No");
            body.put("capacidad_financiacion", capacidadFinanciacion != null ? capacidadFinanciacion : "No");

            log.info("📡 [PrediccionService] Enviando datos a FastAPI ({}): {}", apiUrl, body);

            Map<String, Object> respuesta = restClient.post()
                    .uri("/predecir")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            log.info("✅ [PrediccionService] Respuesta recibida de FastAPI: {}", respuesta);
            return respuesta;
        } catch (Exception e) {
            log.error("❌ Error al llamar FastAPI ({}/predecir): {}", apiUrl, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Predice si el cliente es potencial ("Si" o "No").
     */
    public String predecir(double citas, double antiguedad, String estado, String interes, double tiempo) {
        Map<String, Object> resp = consultarApi(citas, antiguedad, estado, interes, tiempo);
        if (resp != null && resp.get("prediccion") != null) {
            return String.valueOf(resp.get("prediccion"));
        }
        return "No";
    }

    /**
     * Obtiene la probabilidad calculada en porcentaje (0.0 - 100.0).
     */
    public double obtenerProbabilidadSi(double citas, double antiguedad, String estado, String interes, double tiempo) {
        Map<String, Object> resp = consultarApi(citas, antiguedad, estado, interes, tiempo);
        if (resp != null && resp.get("probabilidad") != null) {
            Object valor = resp.get("probabilidad");
            if (valor instanceof Number num) {
                return num.doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(valor));
            } catch (Exception ignored) {}
        }
        return 0.0;
    }
}
