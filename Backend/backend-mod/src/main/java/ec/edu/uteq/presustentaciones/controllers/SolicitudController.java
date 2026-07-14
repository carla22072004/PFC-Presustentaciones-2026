package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.SolicitudService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final UsuarioRepository usuarioRepository;

    public SolicitudController(SolicitudService solicitudService, UsuarioRepository usuarioRepository) {
        this.solicitudService = solicitudService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/crear/{estudianteId}")
    public Solicitud crear(@PathVariable Long estudianteId, @RequestBody Solicitud datos) {
        return solicitudService.crearSolicitud(estudianteId, datos);
    }

    /** Crear solicitud usando el usuarioId del JWT (frontend lo usa directamente) */
    @PostMapping("/crear-por-usuario/{usuarioId}")
    public ResponseEntity<?> crearPorUsuario(@PathVariable Long usuarioId, @RequestBody Solicitud datos) {
        try {
            return ResponseEntity.ok(solicitudService.crearSolicitudPorUsuario(usuarioId, datos));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Listar MIS solicitudes — el backend obtiene el usuarioId desde el JWT,
     * sin depender de ningún parámetro enviado por el cliente.
     */
    @GetMapping("/mis-solicitudes")
    public ResponseEntity<?> listarMisSolicitudes() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName(); // el subject del JWT es el email
            Long usuarioId = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"))
                    .getId();
            return ResponseEntity.ok(solicitudService.listarPorUsuario(usuarioId));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(java.util.List.of());
        }
    }

    /** Listar solicitudes del usuario logueado (por usuarioId) — se mantiene por compatibilidad */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> listarPorUsuario(@PathVariable Long usuarioId) {
        try {
            return ResponseEntity.ok(solicitudService.listarPorUsuario(usuarioId));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(java.util.List.of());
        }
    }

    @PostMapping("/enviar/{id}")
    public Solicitud enviar(@PathVariable Long id) {
        return solicitudService.enviarSolicitud(id);
    }

    @PostMapping("/aprobar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public Solicitud aprobar(@PathVariable Long id) {
        return solicitudService.aprobarSolicitud(id);
    }

    @PostMapping("/rechazar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public Solicitud rechazar(@PathVariable Long id) {
        return solicitudService.rechazarSolicitud(id);
    }

    @PostMapping("/rechazar-con-observacion/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public ResponseEntity<?> rechazarConObservacion(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String observacion = body.getOrDefault("observacion", "");
            return ResponseEntity.ok(solicitudService.rechazarConObservacion(id, observacion));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/suspender/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> suspender(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String motivo = body.get("motivo");
            return ResponseEntity.ok(solicitudService.suspenderSolicitud(id, motivo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /** Solo ADMIN y DOCENTE pueden ver TODAS las solicitudes */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public List<Solicitud> listar() {
        return solicitudService.listarSolicitudes();
    }

    @GetMapping("/estudiante/{estudianteId}")
    public List<Solicitud> listarPorEstudiante(@PathVariable Long estudianteId) {
        return solicitudService.listarPorEstudiante(estudianteId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Solicitud> obtener(@PathVariable Long id) {
        return solicitudService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}