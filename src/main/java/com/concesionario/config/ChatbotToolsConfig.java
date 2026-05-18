package com.concesionario.config;

import com.concesionario.model.Reunion;
import com.concesionario.model.CategoriaReunion;
import com.concesionario.service.ReunionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;
import com.concesionario.dto.OptimizationRequest;
import com.concesionario.dto.OptimizationResult;
import com.concesionario.service.VehiculoOptimizationService;

@Configuration
public class ChatbotToolsConfig {

    public record ReunionRequest(String titulo, String descripcion, String fecha, String hora, String categoria) {}
    
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public static class UpdateReunionRequest {
        @com.fasterxml.jackson.annotation.JsonProperty(required = true)
        public Long id;
        @com.fasterxml.jackson.annotation.JsonProperty(required = false)
        public String titulo;
        @com.fasterxml.jackson.annotation.JsonProperty(required = false)
        public String descripcion;
        @com.fasterxml.jackson.annotation.JsonProperty(required = false)
        public String fecha;
        @com.fasterxml.jackson.annotation.JsonProperty(required = false)
        public String hora;
        @com.fasterxml.jackson.annotation.JsonProperty(required = false)
        public String categoria;
    }
    
    public record DeleteReunionRequest(Long id) {}
    public record ReunionResponse(String status, String message) {}


    @Bean
    @Description("Crea una nueva reunión en el sistema. La fecha debe ser yyyy-MM-dd y la hora HH:mm.")
    public Function<ReunionRequest, ReunionResponse> crearReunion(ReunionService reunionService) {
        return request -> {
            try {
                Reunion reunion = new Reunion();
                reunion.setTitulo(request.titulo());
                reunion.setDescripcion(request.descripcion());
                
                // Intento de parseo flexible de fecha
                String fechaRaw = request.fecha().replace("/", "-");
                LocalDate fecha;
                if (fechaRaw.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    String[] parts = fechaRaw.split("-");
                    fecha = LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
                } else {
                    fecha = LocalDate.parse(fechaRaw);
                }
                
                reunion.setFecha(fecha);
                reunion.setHora(LocalTime.parse(request.hora()));
                reunion.setCategoria(CategoriaReunion.valueOf(request.categoria().toLowerCase()));
                
                reunionService.guardar(reunion);
                return new ReunionResponse("SUCCESS", "Reunión '" + request.titulo() + "' creada para el " + fecha);
            } catch (Exception e) {
                return new ReunionResponse("ERROR", "No se pudo crear la reunión: " + e.getMessage());
            }
        };
    }

    @Bean
    @Description("Actualiza una reunión. Solo 'id' es obligatorio. El resto (titulo, descripcion, fecha, hora, categoria) son opcionales y solo se deben enviar si se desea cambiarlos.")
    public Function<UpdateReunionRequest, ReunionResponse> actualizarReunion(ReunionService reunionService) {
        return request -> {
            try {
                if (request.id == null) return new ReunionResponse("ERROR", "El ID de la reunión es obligatorio.");
                
                Reunion reunion = reunionService.obtenerPorId(request.id);
                if (reunion == null) return new ReunionResponse("ERROR", "No se encontró la reunión con ID " + request.id);
                
                if (request.titulo != null) reunion.setTitulo(request.titulo);
                if (request.descripcion != null) reunion.setDescripcion(request.descripcion);
                if (request.fecha != null) {
                    String fechaRaw = request.fecha.replace("/", "-");
                    if (fechaRaw.matches("\\d{2}-\\d{2}-\\d{4}")) {
                        String[] parts = fechaRaw.split("-");
                        reunion.setFecha(LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0])));
                    } else {
                        reunion.setFecha(LocalDate.parse(fechaRaw));
                    }
                }
                if (request.hora != null) {
                    // Limpiar AM/PM si viene en el texto
                    String horaLimpia = request.hora.split(" ")[0];
                    reunion.setHora(LocalTime.parse(horaLimpia));
                }
                if (request.categoria != null) {
                    reunion.setCategoria(CategoriaReunion.valueOf(request.categoria.toLowerCase()));
                }
                
                reunionService.guardar(reunion);
                return new ReunionResponse("SUCCESS", "Reunión ID " + request.id + " actualizada correctamente.");
            } catch (Exception e) {
                return new ReunionResponse("ERROR", "Error al actualizar: " + e.getMessage());
            }
        };
    }

    @Bean
    @Description("Elimina una reunión del sistema usando su ID.")
    public Function<DeleteReunionRequest, ReunionResponse> eliminarReunion(ReunionService reunionService) {
        return request -> {
            try {
                reunionService.eliminar(request.id());
                return new ReunionResponse("SUCCESS", "Reunión ID " + request.id() + " eliminada.");
            } catch (Exception e) {
                return new ReunionResponse("ERROR", "Error al eliminar: " + e.getMessage());
            }
        };
    }

    @Bean
    @Description("Lista todas las reuniones programadas. Útil para buscar IDs antes de editar o eliminar.")
    public Function<Void, String> listarReuniones(ReunionService reunionService) {
        return request -> {
            List<Reunion> reuniones = reunionService.listarTodas();
            if (reuniones.isEmpty()) return "No hay reuniones programadas.";
            StringBuilder sb = new StringBuilder("Lista de Reuniones:\n");
            for (Reunion r : reuniones) {
                sb.append("- [ID: ").append(r.getId()).append("] ")
                  .append(r.getTitulo()).append(" | ")
                  .append(r.getFecha()).append(" ").append(r.getHora()).append("\n");
            }
            return sb.toString();
        };
    }

    @Bean
    @Description("Genera un archivo .txt con el resumen de todas las reuniones registradas.")
    public Function<Void, String> generarReporteReuniones(ReunionService reunionService) {
        return request -> {
            try {
                List<Reunion> reuniones = reunionService.listarTodas();
                if (reuniones.isEmpty()) return "No hay reuniones para reportar.";
                
                StringBuilder content = new StringBuilder("--- REPORTE DE REUNIONES - NEXTGEN MOTORS ---\n\n");
                for (Reunion r : reuniones) {
                    content.append("ID: ").append(r.getId()).append("\n")
                           .append("Título: ").append(r.getTitulo()).append("\n")
                           .append("Fecha: ").append(r.getFecha()).append("\n")
                           .append("Hora: ").append(r.getHora()).append("\n")
                           .append("Categoría: ").append(r.getCategoria()).append("\n")
                           .append("Descripción: ").append(r.getDescripcion() != null ? r.getDescripcion() : "N/A").append("\n")
                           .append("-------------------------------------------\n");
                }
                
                java.nio.file.Files.writeString(java.nio.file.Paths.get("reporte_reuniones.txt"), content.toString());
                return "Reporte generado exitosamente como 'reporte_reuniones.txt'.";
            } catch (Exception e) {
                return "Error al generar el reporte: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Encuentra el vehículo ideal usando un modelo MILP automatizado. El usuario NO da los pesos; el programa los calcula basado en el 'usoPrincipal' (Deportivo, Familiar, Trabajo, Lujo, Equilibrado). Requiere restricciones de precio (min/max), pasajeros y año.")
    public Function<OptimizationRequest, List<OptimizationResult>> buscarVehiculoIdeal(VehiculoOptimizationService optimizationService) {
        return request -> optimizationService.buscarVehiculoIdeal(request);
    }
}
