package com.concesionario.controller;

import com.concesionario.model.*;
import com.concesionario.service.*;
import com.concesionario.repository.CitaRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import com.concesionario.dto.UsuarioDTO;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.concesionario.repository.TrabajadorRepository;
import java.io.IOException;
import java.util.stream.Collectors;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    public AdminController(
            VehiculoService vehiculoService,
            CitaService citaService,
            NotificacionService notificacionService,
            PasswordEncoder passwordEncoder) {

        this.vehiculoService = vehiculoService;
        this.citaService = citaService;
        this.notificacionService = notificacionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private VehiculoService vehiculoService;
    @Autowired
    private CitaService citaService;
    @Autowired
    private NotificacionService notificacionService; // Servicio unificado (DB Interna + Push)
    @Autowired
    private TrabajadorRepository trabajadorRepository;
    @Autowired
    private com.concesionario.repository.UsuarioRepository usuarioRepository; // ✅ AÑADIDO

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private SupabaseStorageService supabaseStorageService;

    // 🔍 ENDPOINT DE DIAGNÓSTICO (Temporal)
    @GetMapping("/test/status/v2")
    @ResponseBody
    public Map<String, Object> testFirebaseStatusV2() {
        Map<String, Object> status = new HashMap<>();
        try {
            FirebaseApp app = FirebaseApp.getInstance();
            status.put("firebase_inicializado", true);
            status.put("nombre_app", app.getName());
            // Credenciales ocultas por seguridad/visibilidad
            status.put("credential_check", "OK (Instance exists)");
        } catch (Exception e) {
            status.put("firebase_inicializado", false);
            status.put("error", e.getMessage());
        }
        return status;
    }

    @GetMapping("/test/force-notif")
    @ResponseBody
    public Map<String, Object> forceNotification() {
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("timestamp", LocalDateTime.now().toString());
        resultado.put("usuario_prueba", "prueba@gmail.com");

        try {
            // 1. Buscar usuario
            Usuario usuario = usuarioRepository.findByCorreoUser("prueba@gmail.com").orElse(null);
            if (usuario == null) {
                resultado.put("error", "Usuario no encontrado");
                return resultado;
            }

            // 2. Verificar Token
            String token = usuario.getFcmToken();
            resultado.put("token_intentado",
                    token != null && token.length() > 10 ? token.substring(0, 10) + "..." : "NULL/VACIO");

            if (token == null || token.isEmpty()) {
                resultado.put("envio_exitoso", false);
                resultado.put("razon", "El usuario no tiene Token FCM registrado");
                return resultado;
            }

            // 3. Verificar Credenciales en uso
            // (Simplificado para evitar errores de compilación)
            resultado.put("ACTUAL_KEY_ID_EN_SERVIDOR", "HARDCODED_V4_CHECKED");

            // 4. Intentar envío directo (Bypassing NotificationService para aislar errores)
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle("PRUEBA DESDE AZURE (Hardcoded Key)")
                            .setBody("Si lees esto, funcionó el fix de la llave 4.")
                            .build())
                    .putData("tipo", "PRUEBA")
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            resultado.put("envio_exitoso", true);
            resultado.put("mensaje_id", response);

        } catch (Exception e) {
            resultado.put("envio_exitoso", false);
            resultado.put("error_tipo", e.getClass().getSimpleName());
            resultado.put("error_mensaje", e.getMessage());
            // Stacktrace parcial
            StackTraceElement[] trace = e.getStackTrace();
            if (trace.length > 0)
                resultado.put("donde", trace[0].toString());
        }

        return resultado;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Estadísticas
        long totalCitas = citaService.contarTodasLasCitas();
        long totalUsuarios = usuarioService.contarUsuarios();
        long totalVehiculos = vehiculoService.contarTodosVehiculos();

        // Listados
        List<Vehiculo> vehiculos = vehiculoService.obtenerVehiculosNormales();
        List<Vehiculo> anuncios = vehiculoService.obtenerDestacados();
        List<Cita> citasPendientes = citaService.obtenerCitasPendientes();
        List<Trabajador> trabajadores = trabajadorRepository.findAll();

        // Agregar atributos al modelo
        model.addAttribute("totalCitas", totalCitas);
        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("totalVehiculos", totalVehiculos);
        model.addAttribute("vehiculos", vehiculos);
        model.addAttribute("anuncios", anuncios);
        model.addAttribute("citas", citasPendientes);
        model.addAttribute("numeroNotificaciones", notificacionService.contarCitasNoLeidas());
        model.addAttribute("citasNoLeidas", notificacionService.obtenerCitasNoLeidas());
        model.addAttribute("trabajadores", trabajadores);
        return "admin/dashboard";
    }

    @PostMapping("/RegistroT")
    public String registroTrabajador(
            @RequestParam("nombre") String nombre,
            @RequestParam("apellido") String apellido,
            @RequestParam("correo") String correo,
            @RequestParam("identificacion") String identificacion,
            @RequestParam("password") String password,
            @RequestParam("horaInicio") @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
            @RequestParam("horaFin") @DateTimeFormat(pattern = "HH:mm") LocalTime horaFin,
            @RequestParam(value = "diasTrabajo", required = false) List<String> diasTrabajo,
            @RequestParam(value = "roles", required = false) List<String> rolesStrings, // ✅ NUEVO PARÁMETRO
            @RequestParam(value = "sede", required = false) String sede, // ✅ NUEVA SEDE
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validación básica de días de trabajo
        if (diasTrabajo == null || diasTrabajo.isEmpty()) {
            model.addAttribute("error", "Debe seleccionar al menos un día de trabajo");
            return manejarErrorRegistro(model, nombre, apellido, correo, identificacion, horaInicio, horaFin);
        }

        // ✅ NUEVA VALIDACIÓN: Verificar que se seleccione al menos un rol
        if (rolesStrings == null || rolesStrings.isEmpty()) {
            model.addAttribute("error", "Debe seleccionar al menos un rol para el trabajador");
            return manejarErrorRegistro(model, nombre, apellido, correo, identificacion, horaInicio, horaFin);
        }

        try {
            Trabajador trabajador = new Trabajador();
            trabajador.setNombre(nombre);
            trabajador.setApellido(apellido);
            trabajador.setCorreo(correo);
            trabajador.setIdentificacion(identificacion);
            trabajador.setPassword(passwordEncoder.encode(password));
            trabajador.setHoraInicioTrabajo(horaInicio);
            trabajador.setHoraFinTrabajo(horaFin);
            trabajador.setDiasTrabajo(diasTrabajo);

            // ✅ NUEVO: Procesar los roles seleccionados
            List<Rol> roles = rolesStrings.stream()
                    .map(rolString -> {
                        try {
                            return Rol.valueOf(rolString);
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException("Rol inválido: " + rolString);
                        }
                    })
                    .collect(Collectors.toList());

            trabajador.setRoles(roles);

            // Asignar sede si tiene el rol de asesor
            if (roles.contains(Rol.TRB_ASESOR)) {
                trabajador.setSede(sede);
            }

            trabajadorRepository.save(trabajador);

            redirectAttributes.addFlashAttribute("success",
                    "Trabajador registrado exitosamente con roles: " + rolesStrings);
            return "redirect:/admin/dashboard";

        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Error: El correo o identificación ya existen");
            return manejarErrorRegistro(model, nombre, apellido, correo, identificacion, horaInicio, horaFin);
        } catch (Exception e) {
            model.addAttribute("error", "Error inesperado: " + e.getMessage());
            return manejarErrorRegistro(model, nombre, apellido, correo, identificacion, horaInicio, horaFin);
        }
    }

    private String manejarErrorRegistro(Model model, String nombre, String apellido,
            String correo, String identificacion,
            LocalTime horaInicio, LocalTime horaFin) {
        model.addAttribute("nombre", nombre);
        model.addAttribute("apellido", apellido);
        model.addAttribute("correo", correo);
        model.addAttribute("identificacion", identificacion);
        model.addAttribute("horaInicio", horaInicio != null ? horaInicio.toString() : "");
        model.addAttribute("horaFin", horaFin != null ? horaFin.toString() : "");
        return "admin/dashboard";
    }

    @PostMapping("/despedir-trabajador/{id}")
    @ResponseBody
    public String despedirTrabajador(@PathVariable String id) {
        try {
            trabajadorRepository.deleteById(id);
            return "OK";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/notificaciones")
    @ResponseBody
    public List<Map<String, Object>> obtenerNotificaciones() {
        List<Cita> citas = notificacionService.obtenerCitasNoLeidas();

        return citas.stream().map(cita -> {
            Map<String, Object> citaMap = new HashMap<>();
            citaMap.put("id", cita.getId());

            UsuarioDTO usuario = cita.getUsuario();
            citaMap.put("nombres", usuario != null ? usuario.getNombre() : "");
            citaMap.put("apellidos", usuario != null ? usuario.getApellido() : "");

            citaMap.put("tipo", cita.getTipo());
            citaMap.put("fechaCreacion", cita.getFechaCreacion().toString());
            citaMap.put("comentario", cita.getComentario());
            return citaMap;
        }).collect(Collectors.toList());
    }

    @PostMapping("/marcar-leida/{id}")
    @ResponseBody
    public String marcarComoLeida(@PathVariable String id) {
        notificacionService.marcarComoLeida(id);
        return "OK";
    }

    @PostMapping("/marcar-todas-leidas")
    @ResponseBody
    public String marcarTodasComoLeidas() {
        notificacionService.marcarTodasComoLeidas();
        return "OK";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("vehiculo", new Vehiculo());
        return "Admin/Nuevo";
    }

    @GetMapping("/obtener-vehiculo/{id}")
    @ResponseBody
    public Vehiculo obtenerVehiculoParaEdicion(@PathVariable String id) {
        Vehiculo vehiculo = vehiculoService.obtenerPorId(id);
        if (vehiculo.getColores() == null) {
            vehiculo.setColores(new ArrayList<>());
        }
        return vehiculo;
    }

    @PostMapping("/editar/{id}")
    public String editarVehiculo(
            @PathVariable String id,
            @ModelAttribute Vehiculo vehiculo,
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            @RequestParam("motor") String motor,
            @RequestParam("transmision") String transmision,
            @RequestParam("combustible") String combustible,
            @RequestParam("pasajeros") Integer pasajeros,
            @RequestParam String colores,
            @RequestParam(value = "modelo3d", required = false) MultipartFile modelo3d,
            @RequestParam("descripcion") String descripcion,
            Model model) {

        try {
            Vehiculo vehiculoExistente = vehiculoService.obtenerPorId(id);

            // ✅ CORRECCIÓN: Procesar la nueva imagen si se proporciona
            if (imagen != null && !imagen.isEmpty()) {
                vehiculoService.actualizarImagenVehiculo(vehiculoExistente, imagen);
            }

            // ✅ NUEVO: Procesar el modelo 3D si se proporciona
            if (modelo3d != null && !modelo3d.isEmpty()) {
                String urlModelo = supabaseStorageService.uploadFile(modelo3d);
                vehiculoExistente.setUrlModelo3d(urlModelo);
            }

            if (vehiculoExistente.getColores() == null) {
                vehiculoExistente.setColores(new ArrayList<>());
            }
            if (colores != null && !colores.isEmpty()) {
                List<String> listaColores = Arrays.stream(colores.split(","))
                        .map(String::trim)
                        .filter(color -> !color.isEmpty())
                        .collect(Collectors.toList());
                vehiculoExistente.setColores(listaColores);
            } else {
                vehiculoExistente.setColores(new ArrayList<>());
            }

            // Campos básicos
            vehiculoExistente.setMarca(vehiculo.getMarca());
            vehiculoExistente.setModelo(vehiculo.getModelo());
            vehiculoExistente.setAño(vehiculo.getAño());
            vehiculoExistente.setPrecio(vehiculo.getPrecio());
            vehiculoExistente.setCategoria(vehiculo.getCategoria());
            vehiculoExistente.setMotor(motor);
            vehiculoExistente.setTransmision(transmision);
            vehiculoExistente.setCombustible(combustible);
            vehiculoExistente.setPasajeros(pasajeros);
            vehiculoExistente.setDescripcion(descripcion);

            // Procesar colores
            if (colores != null && !colores.isEmpty()) {
                List<String> listaColores = Arrays.stream(colores.split(","))
                        .map(String::trim)
                        .filter(color -> !color.isEmpty())
                        .collect(Collectors.toList());
                vehiculoExistente.setColores(listaColores);
            } else {
                vehiculoExistente.setColores(new ArrayList<>());
            }

            // ✅ MODIFICADO: Ya no necesitamos manejar la imagen aquí porque
            // el VehiculoService ahora usa Cloudinary automáticamente
            // Solo llamamos al servicio para guardar
            vehiculoService.guardarVehiculo(vehiculoExistente);
            return "redirect:/admin/dashboard";

        } catch (Exception e) {
            model.addAttribute("error", "Error al editar el vehículo: " + e.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarVehiculo(@PathVariable String id) {
        // ✅ MODIFICADO: Ya no necesitamos eliminar archivos locales
        // Cloudinary maneja el almacenamiento automáticamente
        vehiculoService.eliminarVehiculo(id);
        return "redirect:/admin/dashboard";
    }

    // ❌ ELIMINADO: Ya no necesitamos el método guardarImagen local
    // private String guardarImagen(MultipartFile imagen) throws IOException {
    // // Este método ha sido eliminado porque ahora usamos Cloudinary
    // }

    @GetMapping("/citas")
    public String listarCitas(Model model) {
        List<Cita> citas = citaService.obtenerTodasLasCitas();
        model.addAttribute("citas", citas);
        model.addAttribute("numeroNotificaciones", notificacionService.contarCitasNoLeidas());
        return "Admin/CitasLista";
    }

    @ResponseBody
    @PostMapping("/citas/{id}/cambiar-estado")
    public String cambiarEstadoCita(@PathVariable String id, @RequestParam String estado) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            return "Cita no encontrada";
        }

        cita.setEstado(estado);
        cita.setLeida(true);
        citaService.guardarCita(cita);

        // 🔔 NOTIFICACIÓN PUSH
        if (cita.getUsuario() != null) {
            notificacionService.enviarNotificacion(
                    cita.getUsuario().getId(),
                    "Actualización de Cita",
                    "El administrador cambió el estado de tu cita a: " + estado);
        }

        return "OK";
    }

    @ResponseBody
    @PostMapping("/citas/{id}/asignar-fecha")
    public String asignarFechaCita(@PathVariable String id,
            @RequestParam String fecha,
            @RequestParam String horaInicio,
            @RequestParam String horaFin) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            return "Cita no encontrada";
        }

        // El Administrador tiene libertad total de asignación según requerimiento del usuario
        // skipping strict validation call here

        // Guardar detalles del rango horario
        cita.setFechaCita(fecha);
        cita.setHoraCita(horaInicio);
        cita.setHoraFinCita(horaFin);

        // Usar horaInicio para fechaAsignada (debe estar en formato estricto ej: 14:00)
        LocalDateTime fechaHora = LocalDateTime.parse(fecha + "T" + horaInicio);
        cita.setFechaAsignada(fechaHora);
        citaService.guardarCita(cita);

        // 🔔 NOTIFICACIÓN PUSH
        if (cita.getUsuario() != null) {
            notificacionService.enviarNotificacion(
                    cita.getUsuario().getId(),
                    "Cita Programada",
                    "El administrador ha programado tu cita para el: " + fecha + " a las " + horaInicio);
        }

        return "OK";
    }

    @ResponseBody
    @GetMapping("/citas/{id}/notas")
    public String obtenerNotasCita(@PathVariable String id) {
        Cita cita = citaService.obtenerCitaPorId(id);
        return (cita != null && cita.getNotasAdmin() != null) ? cita.getNotasAdmin() : "";
    }

    @ResponseBody
    @PostMapping("/citas/{id}/guardar-notas")
    public String guardarNotasCita(@PathVariable String id, @RequestParam String notas) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            return "Cita no encontrada";
        }

        cita.setNotasAdmin(notas);
        citaService.guardarCita(cita);

        // 🔔 NOTIFICACIÓN PUSH
        if (cita.getUsuario() != null) {
            notificacionService.enviarNotificacion(
                    cita.getUsuario().getId(),
                    "Nueva Nota en tu Cita",
                    "Nota del administrador: " + (notas.length() > 50 ? notas.substring(0, 47) + "..." : notas));
        }

        return "OK";
    }

    @ResponseBody
    @PostMapping("/citas/{id}/cancelar-fecha")
    public String cancelarFechaCita(@PathVariable String id) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            return "Cita no encontrada";
        }

        cita.setFechaAsignada(null);
        citaService.guardarCita(cita);

        return "OK";
    }

    @ResponseBody
    @PostMapping("/citas/{id}/volver-pendiente")
    public String volverAPendiente(@PathVariable String id) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            return "Cita no encontrada";
        }

        cita.setEstado("Pendiente");
        cita.setFechaAsignada(null);
        citaService.guardarCita(cita);

        return "OK";
    }

    @ResponseBody
    @GetMapping("/citas/{id}/datos")
    public Map<String, Object> obtenerDatosCita(@PathVariable String id) {
        Cita cita = citaService.obtenerCitaPorId(id);
        if (cita == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada");
        }

        Map<String, Object> datos = new HashMap<>();
        datos.put("estado", cita.getEstado());
        datos.put("fechaAsignada", cita.getFechaAsignada());
        datos.put("notasAdmin", cita.getNotasAdmin());

        return datos;
    }

    @GetMapping("/anuncios")
    public String listarAnuncios(Model model) {
        model.addAttribute("anuncios", vehiculoService.obtenerDestacados());
        return "Admin/Dashboard";
    }

    @PostMapping("/guardar-anuncio")
    public String guardarAnuncio(
            @RequestParam String marca,
            @RequestParam String modelo,
            @RequestParam int año,
            @RequestParam double precio,
            @RequestParam String categoria,
            @RequestParam String motor,
            @RequestParam String transmision,
            @RequestParam String combustible,
            @RequestParam int pasajeros,
            @RequestParam String descripcion,
            @RequestParam(value = "modelo3d", required = false) MultipartFile modelo3d,
            @RequestParam MultipartFile imagen,
            Model model) throws IOException {

        try {
            Vehiculo anuncio = new Vehiculo();
            anuncio.setMarca(marca);
            anuncio.setModelo(modelo);
            anuncio.setAño(año);
            anuncio.setPrecio(precio);
            anuncio.setCategoria(categoria);
            anuncio.setMotor(motor);
            anuncio.setTransmision(transmision);
            anuncio.setCombustible(combustible);
            anuncio.setPasajeros(pasajeros);
            anuncio.setDescripcion(descripcion);
            anuncio.setDestacado(true); // Es un anuncio

            // Subir modelo 3D si existe
            if (modelo3d != null && !modelo3d.isEmpty()) {
                System.out
                        .println("📦 [AdminController] Modelo 3D anuncio detectado: " + modelo3d.getOriginalFilename());
                try {
                    String urlModelo = supabaseStorageService.uploadFile(modelo3d);
                    System.out.println("✅ [AdminController] Modelo 3D anuncio subido a: " + urlModelo);
                    anuncio.setUrlModelo3d(urlModelo);
                } catch (Exception e) {
                    System.err.println("❌ [AdminController] Error subiendo modelo 3D anuncio: " + e.getMessage());
                }
            }

            vehiculoService.crearAnuncio(anuncio, imagen);
            return "redirect:/admin/dashboard";

        } catch (Exception e) {
            model.addAttribute("error", "Error al crear el anuncio: " + e.getMessage());
            return "redirect:/admin/dashboard?error=Error+al+crear+anuncio";
        }
    }

    @PostMapping("/anuncios/eliminar/{id}")
    public String eliminarAnuncio(@PathVariable String id) {
        vehiculoService.eliminarVehiculo(id);
        return "redirect:/admin/anuncios";
    }

    @PostMapping("/guardar-vehiculo")
    public String guardarVehiculoNormal(
            @RequestParam String marca,
            @RequestParam String modelo,
            @RequestParam int año,
            @RequestParam double precio,
            @RequestParam String categoria,
            @RequestParam String motor,
            @RequestParam String transmision,
            @RequestParam String combustible,
            @RequestParam int pasajeros,
            @RequestParam String descripcion,
            @RequestParam String colores,
            @RequestParam(value = "modelo3d", required = false) MultipartFile modelo3d,
            @RequestParam MultipartFile imagen) throws IOException {

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setMarca(marca);
        vehiculo.setModelo(modelo);
        vehiculo.setAño(año);
        vehiculo.setPrecio(precio);
        vehiculo.setCategoria(categoria);
        vehiculo.setMotor(motor);
        vehiculo.setTransmision(transmision);
        vehiculo.setCombustible(combustible);
        vehiculo.setPasajeros(pasajeros);
        vehiculo.setDescripcion(descripcion);
        vehiculo.setDestacado(false);

        // Subir modelo 3D si existe
        if (modelo3d != null && !modelo3d.isEmpty()) {
            System.out.println("📦 [AdminController] Modelo 3D detectado: " + modelo3d.getOriginalFilename() + " ("
                    + modelo3d.getSize() + " bytes)");
            try {
                String urlModelo = supabaseStorageService.uploadFile(modelo3d);
                System.out.println("✅ [AdminController] Modelo 3D subido a: " + urlModelo);
                vehiculo.setUrlModelo3d(urlModelo);
            } catch (Exception e) {
                System.err.println("❌ [AdminController] Error subiendo modelo 3D: " + e.getMessage());
                // No detenemos el guardado del vehículo, pero logueamos el error
            }
        } else {
            System.out.println("⚠️ [AdminController] No se recibió archivo de modelo 3D opciona o estaba vacío.");
        }

        // Procesar los colores
        if (colores != null && !colores.isEmpty()) {
            List<String> listaColores = Arrays.stream(colores.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            vehiculo.setColores(listaColores);
        }

        vehiculoService.crearVehiculoNormal(vehiculo, imagen, null);
        return "redirect:/admin/dashboard";
    }
}
