package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.SolicitudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;

@Slf4j
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
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SOLICITUDES_REVISAR')")
    public ResponseEntity<?> crear(@PathVariable Long estudianteId, @RequestBody Solicitud datos) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(solicitudService.crearSolicitud(estudianteId, datos), "Solicitud creada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * Crear solicitud — el backend resuelve el usuario desde el JWT,
     * ignorando el usuarioId del path para evitar inconsistencias.
     */
    @PostMapping("/crear-por-usuario/{usuarioId}")
    public ResponseEntity<?> crearPorUsuario(@PathVariable Long usuarioId, @RequestBody Solicitud datos) {
        try {
            // Obtener email desde el JWT (más seguro que el id del path)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            Long realUsuarioId = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado en el sistema"))
                    .getId();
            return ResponseEntity.ok(ResponseWrapper.success(solicitudService.crearSolicitudPorUsuario(realUsuarioId, datos), "Solicitud creada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
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
            return ResponseEntity.ok(ResponseWrapper.success(solicitudService.listarPorUsuario(usuarioId)));
        } catch (RuntimeException e) {
            log.error("Error al listar mis-solicitudes: {}", e.getMessage(), e);
            return ResponseEntity.ok(ResponseWrapper.success(java.util.List.of()));
        }
    }

    /** Listar solicitudes del usuario logueado (por usuarioId) — se mantiene por compatibilidad */
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SOLICITUDES_REVISAR')")
    public ResponseEntity<?> listarPorUsuario(@PathVariable Long usuarioId) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(solicitudService.listarPorUsuario(usuarioId)));
        } catch (RuntimeException e) {
            log.error("Error al listar solicitudes por usuario {}: {}", usuarioId, e.getMessage(), e);
            return ResponseEntity.ok(ResponseWrapper.success(java.util.List.of()));
        }
    }

    @PostMapping("/enviar/{id}")
    public ResponseEntity<?> enviar(@PathVariable Long id) {
        try {
            // Verificar propiedad o permiso
            validarAccesoSolicitud(id);
            return ResponseEntity.ok(ResponseWrapper.success(solicitudService.enviarSolicitud(id), "Solicitud enviada a revisión"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PostMapping("/aprobar/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SOLICITUDES_REVISAR')")
    public ResponseEntity<?> aprobar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(solicitudService.aprobarSolicitud(id), "Solicitud aprobada"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PostMapping("/rechazar/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SOLICITUDES_REVISAR')")
    public ResponseEntity<?> rechazar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(solicitudService.rechazarSolicitud(id), "Solicitud rechazada"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PostMapping("/rechazar-con-observacion/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SOLICITUDES_REVISAR')")
    public ResponseEntity<?> rechazarConObservacion(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String observacion = body.getOrDefault("observacion", "");
            return ResponseEntity.ok(ResponseWrapper.success(solicitudService.rechazarConObservacion(id, observacion), "Solicitud rechazada con observaciones"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PostMapping("/suspender/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SOLICITUDES_SUSPENDER')")
    public ResponseEntity<?> suspender(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String motivo = body.get("motivo");
            return ResponseEntity.ok(ResponseWrapper.success(solicitudService.suspenderSolicitud(id, motivo), "Solicitud suspendida"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /** ADMIN, COORDINADOR y DOCENTE pueden ver TODAS las solicitudes */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SOLICITUDES_REVISAR')")
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(ResponseWrapper.success(solicitudService.listarSolicitudes()));
    }

    /**
     * Versión paginada de {@link #listar()} — evita cargar miles de filas de una sola vez,
     * que es lo que hacía colapsar la tabla de "Gestionar Solicitudes" en el frontend.
     * ERR-01: agrega búsqueda de texto libre (parámetro "q") combinable con el filtro de
     * estado, mismo patrón que GET /api/v1/usuarios/paginado.
     */
    @GetMapping("/paginado")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SOLICITUDES_REVISAR')")
    public ResponseEntity<?> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaDesde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaHasta) {
        Page<Solicitud> resultado = solicitudService.listarSolicitudesPaginado(page, size, estado, q, fechaDesde, fechaHasta);
        return ResponseEntity.ok(ResponseWrapper.success(Map.of(
                "content", resultado.getContent(),
                "totalElements", resultado.getTotalElements(),
                "totalPages", resultado.getTotalPages(),
                "page", resultado.getNumber(),
                "size", resultado.getSize()
        )));
    }

    /** Conteo de solicitudes por estado, para los contadores de las pestañas de filtro */
    @GetMapping("/contar-por-estado")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SOLICITUDES_REVISAR')")
    public ResponseEntity<?> contarPorEstado() {
        return ResponseEntity.ok(ResponseWrapper.success(solicitudService.contarPorEstado()));
    }

    @GetMapping("/estudiante/{estudianteId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SOLICITUDES_REVISAR')")
    public ResponseEntity<?> listarPorEstudiante(@PathVariable Long estudianteId) {
        return ResponseEntity.ok(ResponseWrapper.success(solicitudService.listarPorEstudiante(estudianteId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            validarAccesoSolicitud(id);
            return solicitudService.obtenerPorId(id)
                    .map(s -> ResponseEntity.ok(ResponseWrapper.success(s)))
                    .orElse(ResponseEntity.status(404).body(ResponseWrapper.error("Solicitud no encontrada")));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * SP (Fase 3): Reporte consolidado de defensas por carrera.
     * Llama a presus.sp_generar_reporte_defensas(p_carrera)
     * Flujo: GET → SolicitudController → SolicitudService → SolicitudRepository → SP → PostgreSQL
     *
     * @param carrera nombre o parte del nombre de la carrera (búsqueda ILIKE)
     */
    @GetMapping("/reporte-defensas")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SOLICITUDES_REVISAR')")
    public ResponseEntity<?> reporteDefensas(
            @RequestParam(defaultValue = "") String carrera) {
        try {
            List<Map<String, Object>> reporte = solicitudService.generarReporteDefensasSP(carrera);
            return ResponseEntity.ok(ResponseWrapper.success(reporte));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * Valida que el usuario actual tenga permisos de revisión O sea el propietario de la solicitud.
     */
    private void validarAccesoSolicitud(Long solicitudId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean esRevisor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SOLICITUDES_REVISAR") || a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!esRevisor) {
            Solicitud solicitud = solicitudService.obtenerPorId(solicitudId)
                    .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
            if (!solicitud.getEstudiante().getUsuario().getEmail().equals(email)) {
                throw new RuntimeException("Acceso denegado: no eres propietario de esta solicitud");
            }
        }
    }
}