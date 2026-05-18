package com.concesionario.service;

import com.concesionario.model.Trabajador;
import com.concesionario.repository.TrabajadorRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.ArrayList;
import com.concesionario.model.Rol;

@Configuration
public class TrabajadorTools {

    private final TrabajadorRepository trabajadorRepository;
    private final PasswordEncoder passwordEncoder;

    public TrabajadorTools(TrabajadorRepository trabajadorRepository, PasswordEncoder passwordEncoder) {
        this.trabajadorRepository = trabajadorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private boolean checkAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    public record ListarTrabajadoresRequest() {}

    @Bean
    @Description("Lista todos los trabajadores actuales de la empresa, devolviendo su ID, Nombre, Apellido, Identificación y Correo.")
    public Function<ListarTrabajadoresRequest, String> listarTrabajadores() {
        return request -> {
            if (!checkAdmin()) return "Error: Permiso denegado. Solo administradores pueden usar esta herramienta.";
            
            List<Trabajador> trabajadores = trabajadorRepository.findAll();
            if (trabajadores.isEmpty()) {
                return "No hay trabajadores registrados en la base de datos.";
            }
            return "Lista de Trabajadores:\n" + trabajadores.stream()
                    .map(t -> String.format("- ID: %s | Nombre: %s %s | Identificación: %s | Correo: %s",
                            t.getId(), t.getNombre(), t.getApellido(), t.getIdentificacion(), t.getCorreo()))
                    .collect(Collectors.joining("\n"));
        };
    }

    public record CrearTrabajadorRequest(
        @Description("Nombre del trabajador") String nombre, 
        @Description("Apellido del trabajador") String apellido, 
        @Description("DNI o Identificación única") String identificacion, 
        @Description("Correo electrónico") String correo, 
        @Description("Contraseña de acceso") String password,
        @Description("Rol específico (ej: TRB_GESTOR, TRB_ASESOR, TRB_ANALISIS)") String rol
    ) {}

    @Bean
    @Description("Crea un nuevo trabajador. Obligatorios: nombre, apellido, identificacion, correo, password, rol.")
    public Function<CrearTrabajadorRequest, String> crearTrabajador() {
        return request -> {
            if (!checkAdmin()) return "Error: Permiso denegado.";

            if (trabajadorRepository.existsByCorreo(request.correo())) {
                return "Error: Ya existe un trabajador con el correo " + request.correo();
            }

            Trabajador t = new Trabajador();
            t.setNombre(request.nombre());
            t.setApellido(request.apellido());
            t.setIdentificacion(request.identificacion());
            t.setCorreo(request.correo());
            t.setPassword(passwordEncoder.encode(request.password()));
            
            // Seteamos departamento a null/vacío para que no aparezca si no quieres
            t.setDepartamento(null);
            t.setCargo(null); 

            // 1. Inicializar Horario (dump: 13:00 a 22:00)
            t.setHoraInicioTrabajo(LocalTime.of(13, 0));
            t.setHoraFinTrabajo(LocalTime.of(22, 0));
            
            // 2. Inicializar Días de trabajo (5 días )
            t.setDiasTrabajo(new ArrayList<>(Arrays.asList("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES")));
            
            // 3. Asignar Roles al ARRAY (único lugar donde va el cargo/rol)
            List<Rol> roles = new ArrayList<>();
            roles.add(Rol.TRABAJADOR); // Rol base siempre presente
            
            try {
                if (request.rol() != null) {
                    Rol rolEnum = Rol.valueOf(request.rol().toUpperCase());
                    if (rolEnum != Rol.TRABAJADOR) {
                        roles.add(rolEnum);
                    }
                }
            } catch (Exception e) {
                // Si el rol no es válido, se queda solo con TRABAJADOR
            }
            t.setRoles(roles);

            trabajadorRepository.save(t);
            return "EXITO: Trabajador " + t.getNombre() + " creado con roles: " + roles;
        };
    }

    public record EliminarTrabajadorRequest(String identificacionOId) {}

    @Bean
    @Description("Despide o elimina permanentemente a un trabajador de la empresa. Recibe la identificación o el ID del trabajador a eliminar.")
    public Function<EliminarTrabajadorRequest, String> eliminarTrabajador() {
        return request -> {
            if (!checkAdmin()) return "Error: Permiso denegado. Solo administradores pueden despedir trabajadores.";

            String buscar = request.identificacionOId();
            Optional<Trabajador> porId = trabajadorRepository.findById(buscar);
            
            if (porId.isPresent()) {
                trabajadorRepository.delete(porId.get());
                return "Trabajador eliminado exitosamente de la base de datos.";
            }

            Optional<Trabajador> porIdentificacion = trabajadorRepository.findByIdentificacion(buscar);
            if (porIdentificacion.isPresent()) {
                trabajadorRepository.delete(porIdentificacion.get());
                return "Trabajador eliminado exitosamente de la base de datos.";
            }

            return "Error: No se encontró ningún trabajador con ese ID o Identificación.";
        };
    }

    public record ActualizarTrabajadorRequest(String identificacionOId, String nuevoRol, String nuevoCorreo) {}

    @Bean
    @Description("Edita o actualiza la información de un trabajador existente. Pide el ID o Identificación del trabajador y los datos a cambiar (rol o correo).")
    public Function<ActualizarTrabajadorRequest, String> actualizarTrabajador() {
        return request -> {
            if (!checkAdmin()) return "Error: Permiso denegado. Solo administradores pueden editar trabajadores.";

            String buscar = request.identificacionOId();
            Optional<Trabajador> tOpt = trabajadorRepository.findById(buscar);
            if (tOpt.isEmpty()) {
                tOpt = trabajadorRepository.findByIdentificacion(buscar);
            }

            if (tOpt.isPresent()) {
                Trabajador t = tOpt.get();
                
                // Si hay un nuevo rol, actualizamos el array de roles
                if (request.nuevoRol() != null && !request.nuevoRol().isBlank()) {
                    try {
                        Rol rolEnum = Rol.valueOf(request.nuevoRol().toUpperCase());
                        List<Rol> nuevosRoles = new ArrayList<>();
                        nuevosRoles.add(Rol.TRABAJADOR);
                        if (rolEnum != Rol.TRABAJADOR) nuevosRoles.add(rolEnum);
                        t.setRoles(nuevosRoles);
                    } catch (Exception e) {
                        // Rol no válido, no se cambia
                    }
                }
                
                if (request.nuevoCorreo() != null && !request.nuevoCorreo().isBlank()) t.setCorreo(request.nuevoCorreo());
                
                trabajadorRepository.save(t);
                return "Trabajador actualizado exitosamente. Roles: " + t.getRoles() + ", Correo: " + t.getCorreo();
            }

            return "Error: No se encontró ningún trabajador con ese ID o Identificación.";
        };
    }
}
