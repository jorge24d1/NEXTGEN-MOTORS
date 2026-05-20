package com.concesionario.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.concesionario.repository.VehiculoRepository;
import com.concesionario.model.Vehiculo;
import java.util.List;
import java.util.Comparator;
import java.util.Arrays;
import java.text.Normalizer;

/**
 * Servicio de Chatbot impulsado por Groq (vía OpenAI compatibility layer)
 */
@Service
public class ChatbotService {

    private final ChatModel chatModel;
    private final VehiculoRepository vehiculoRepository;

    public ChatbotService(ChatModel chatModel, VehiculoRepository vehiculoRepository) {
        this.chatModel = chatModel;
        this.vehiculoRepository = vehiculoRepository;
    }
    
    // Récord auxiliar para puntuar los vehículos
    private record VehiculoScore(Vehiculo vehiculo, int score) {}

    // Elimina tildes/acentos para comparación flexible
    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                         .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }

    public String analizarYResponder(String mensajeUsuario, List<Map<String, String>> historial, boolean isAdmin) {
        String mensajeMin = mensajeUsuario.toLowerCase();
        
        // 1. Extraer palabras clave y expandirlas contextualmente
        List<String> palabrasOriginales = Arrays.stream(mensajeMin.replaceAll("[^a-záéíóúñ0-9\\s]", "").split("\\s+"))
                                           .filter(p -> p.length() > 2)
                                           .toList();
        
        // Expansión de términos para búsqueda contextual
        StringBuilder regexBuilder = new StringBuilder();
        for (String p : palabrasOriginales) {
            if (regexBuilder.length() > 0) regexBuilder.append("|");
            regexBuilder.append(p);
            
            // Sinónimos y conceptos relacionados mejorados (fuzzy context)
            if (p.startsWith("rapid") || p.startsWith("veloz") || p.contains("velocidad")) {
                regexBuilder.append("|potencia|aceleración|rendimiento|0-100|pista|deportivo|performance|rápido|rapido");
            } else if (p.contains("viaja") || p.contains("carretera") || p.contains("pasear")) {
                regexBuilder.append("|comodidad|confort|premium|autovía|distancia|largo|crucero|viajar|viaje");
            } else if (p.contains("familia") || p.contains("hijo") || p.contains("niño")) {
                regexBuilder.append("|espacio|seguridad|pasajeros|asientos|amplio|familiar");
            } else if (p.contains("campo") || p.contains("finca") || p.contains("offroad") || p.contains("todoterreno") || p.contains("montaña")) {
                regexBuilder.append("|4x4|tracción|terreno|robusto|aventura|suspensión|todoterreno|barro");
            } else if (p.contains("automati") || p.contains("automatic")) {
                regexBuilder.append("|automática|automatica|transmisión|caja");
            }
        }
                                           
        List<Vehiculo> vehiculosFiltrados;
        if (regexBuilder.length() > 0) {
            vehiculosFiltrados = vehiculoRepository.findByFiltroRegex(regexBuilder.toString());
        } else {
            vehiculosFiltrados = vehiculoRepository.findByDestacadoTrue();
        }

        if (vehiculosFiltrados == null || vehiculosFiltrados.isEmpty()) {
            vehiculosFiltrados = vehiculoRepository.findByDestacadoTrue();
        }
                                           
        // 2. Calcular puntaje con pesos contextuales
        List<Vehiculo> mejoresOpciones = vehiculosFiltrados.stream()
            .map(v -> {
                int score = 0;
                String desc = normalizarTexto(v.getDescripcion());
                String marc = normalizarTexto(v.getMarca());
                String mod  = normalizarTexto(v.getModelo());
                // Normalizar categoría para ignorar tildes ("Híbridos" → "hibridos")
                String cat  = normalizarTexto(v.getCategoria());
                // Normalizar también el término de búsqueda al comparar
                String mensajeNorm = normalizarTexto(mensajeUsuario);
                
                String[] terminosBusqueda = regexBuilder.toString().split("\\|");
                for (String t : terminosBusqueda) {
                    String tNorm = normalizarTexto(t);
                    if (marc.contains(tNorm) || mod.contains(tNorm)) {
                        score += 5;
                    } else if (cat.contains(tNorm)) {
                        score += 3;
                    } else if (desc.contains(tNorm)) {
                        score += 4;
                    }
                }
                // Bonus si la categoría aparece literalmente en el mensaje (sin tilde)
                if (!cat.isEmpty() && mensajeNorm.contains(cat)) score += 4;
                
                if (score == 0 && v.isDestacado()) score = 1;
                return new VehiculoScore(v, score);
            })
            .sorted(Comparator.comparingInt(VehiculoScore::score).reversed())
            .limit(2)
            .map(VehiculoScore::vehiculo)
            .toList();

        // 3. Crear el inventario reducido (Solo datos esenciales para ahorrar tokens)
        StringBuilder inventario = new StringBuilder("\nVEHÍCULOS:\n");
        for (Vehiculo v : mejoresOpciones) {
            inventario.append("- ID: [").append(v.getId()).append("] | ")
                      .append(v.getMarca()).append(" ").append(v.getModelo())
                      .append(" (").append(v.getCategoria()).append(") | $").append(v.getPrecio()).append("\n");
        }

        // 4. Preparar lista de mensajes con HISTORIAL
        List<Message> mensajes = new ArrayList<>();
        String systemPrompt = "Eres Dante, el asistente virtual oficial de NextGen Motors. Tu objetivo es ayudar a los usuarios con temas relacionados exclusivamente con el concesionario (vehículos, citas, contactos y consultas sobre automóviles).\n\n" +
            "REGLAS CRÍTICAS:\n" +
            "1. LÍMITES DE RESPUESTA: Si el usuario te pregunta sobre temas no relacionados con el concesionario o vehículos (como matemáticas, programación, historia, recetas, tareas generales, etc.), debes responder de forma amable y educada indicando que solo puedes asistir en temas relacionados con NextGen Motors y el sector automotriz.\n" +
            "2. AGENDAR CITAS: Si el usuario expresa interés en agendar o reservar una cita, explícale de forma atenta que puede hacerlo a través de nuestra plataforma y proporciónale el enlace directo en HTML: <a href=\"/usuario/cita\" style=\"color: #0066cc; text-decoration: underline;\">Agendar Cita aquí</a>.\n" +
            "3. QUIÉN ERES: Si te preguntan quién eres, preséntate como Dante, el asesor y asistente virtual de NextGen Motors.\n" +
            "4. Sé directo, breve y conciso en tus respuestas. Muestra siempre los IDs y Títulos de las reuniones que encuentres con 'listarReuniones' (si aplica).\n\n" +
            "Inventario recomendado para este usuario: " + inventario.toString() + "\n" +
            "REGLAS: Categorías: analista, gestion, marketing, acesoria.";
        
        mensajes.add(new SystemMessage(systemPrompt));

        // Añadir historial al contexto
        if (historial != null) {
            for (Map<String, String> msg : historial) {
                if ("user".equals(msg.get("role"))) {
                    mensajes.add(new UserMessage(msg.get("content")));
                } else {
                    mensajes.add(new AssistantMessage(msg.get("content")));
                }
            }
        }

        // Añadir mensaje actual
        mensajes.add(new UserMessage(mensajeUsuario));

        // 1. Truncar el historial al máximo (últimos 2 mensajes)
        if (mensajes.size() > 3) {
            List<Message> historialOptimizado = new ArrayList<>();
            historialOptimizado.add(mensajes.get(0));
            historialOptimizado.addAll(mensajes.subList(mensajes.size() - 2, mensajes.size()));
            mensajes = historialOptimizado;
        }

        // Definir opciones base (disponibles para todos)
        org.springframework.ai.openai.OpenAiChatOptions.Builder optionsBuilder = 
            org.springframework.ai.openai.OpenAiChatOptions.builder()
                .toolNames("buscarVehiculoIdeal")
                .temperature(0.4);

        if (isAdmin) {
            String adminInstrucciones = """
            ADMIN:
            - Usa 'listarTrabajadores' para ver personal.
            - 'crearTrabajador', 'actualizarTrabajador', 'crearReunion', 'listarReuniones', 'actualizarReunion'.
            - ¡No inventes datos!
            """;
            System.out.println("--- MODO ADMIN ACTIVADO EN ChatbotService ---");
            
            // Inyectar instrucciones de administrador al final del mensaje de sistema
            String contenidoActual = ((SystemMessage) mensajes.get(0)).getText();
            mensajes.set(0, new SystemMessage(contenidoActual + adminInstrucciones));

            optionsBuilder.toolNames("listarTrabajadores", "crearTrabajador", "eliminarTrabajador", "actualizarTrabajador", 
                                   "crearReunion", "listarReuniones", "actualizarReunion", "eliminarReunion", 
                                   "generarReporteReuniones", "buscarVehiculoIdeal")
                    .parallelToolCalls(false)
                    .temperature(0.2);
        }

        return chatModel.call(new Prompt(mensajes, optionsBuilder.build())).getResult().getOutput().getText();
    }
}
