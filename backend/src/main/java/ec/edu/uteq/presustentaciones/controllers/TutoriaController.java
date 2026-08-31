package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.NuevoMensajeRequest;
import ec.edu.uteq.presustentaciones.dto.TutoriaFaseDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaMensajeDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaResumenDTO;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.TutoriaService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tutorias")
@CrossOrigin(origins = "http://localhost:4200")
public class TutoriaController {

    private final TutoriaService tutoriaService;
    private final UsuarioRepository usuarioRepository;

    public TutoriaController(TutoriaService tutoriaService, UsuarioRepository usuarioRepository) {
        this.tutoriaService = tutoriaService;
        this.usuarioRepository = usuarioRepository;
    }

    // ── Listados por usuario ──────────────────────────────────────────────────

    @GetMapping("/estudiante/{usuarioId}")
    public ResponseEntity<?> obtenerTutoriasEstudiante(@PathVariable Long usuarioId) {
        try {
            Long realUsuarioId = resolverUsuarioId(usuarioId);
            List<TutoriaResumenDTO> resultado = tutoriaService.obtenerTutoriasEstudiante(realUsuarioId);
            return ResponseEntity.ok(ResponseWrapper.success(resultado));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @GetMapping("/docente/{usuarioId}")
    public ResponseEntity<?> obtenerTutoriasDocente(@PathVariable Long usuarioId) {
        try {
            Long realUsuarioId = resolverUsuarioId(usuarioId);
            List<TutoriaResumenDTO> resultado = tutoriaService.obtenerTutoriasDocente(realUsuarioId);
            return ResponseEntity.ok(ResponseWrapper.success(resultado));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    // ── Resumen y fases ───────────────────────────────────────────────────────

    @GetMapping("/{tutorId}/resumen")
    public ResponseEntity<?> obtenerResumen(@PathVariable Long tutorId,
                                            @RequestParam(required = false) Long usuarioId) {
        try {
            Long realUsuarioId = resolverUsuarioId(usuarioId);
            TutoriaResumenDTO resumen = tutoriaService.obtenerResumen(tutorId, realUsuarioId);
            return ResponseEntity.ok(ResponseWrapper.success(resumen));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @GetMapping("/{tutorId}/fases")
    public ResponseEntity<?> obtenerFases(@PathVariable Long tutorId,
                                          @RequestParam(required = false) Long usuarioId) {
        try {
            Long realUsuarioId = resolverUsuarioId(usuarioId);
            List<TutoriaFaseDTO> fases = tutoriaService.obtenerFases(tutorId, realUsuarioId);
            return ResponseEntity.ok(ResponseWrapper.success(fases));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    // ── Operaciones sobre fases ───────────────────────────────────────────────

    @PostMapping("/{tutorId}/nueva-fase")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TUTORIA_GESTIONAR')")
    public ResponseEntity<?> crearFaseConObservacion(@PathVariable Long tutorId,
                                                     @RequestParam String observacion,
                                                     @RequestParam(required = false) Long tutorUsuarioId) {
        try {
            Long realTutorUsuarioId = obtenerUsuarioAutenticado().getId();
            TutoriaFaseDTO fase = tutoriaService.crearFaseConObservacion(tutorId, realTutorUsuarioId, observacion);
            return ResponseEntity.ok(ResponseWrapper.success(fase, "Fase creada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PostMapping(value = "/fases/{faseId}/subir-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirPdfCorregido(@PathVariable Long faseId,
                                               @RequestParam("archivo") MultipartFile archivo,
                                               @RequestParam(required = false) Long estudianteUsuarioId) {
        try {
            Long realEstudianteUsuarioId = obtenerUsuarioAutenticado().getId();
            TutoriaFaseDTO fase = tutoriaService.subirPdfCorregido(faseId, archivo, realEstudianteUsuarioId);
            return ResponseEntity.ok(ResponseWrapper.success(fase, "PDF subido exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PostMapping("/fases/{faseId}/aprobar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TUTORIA_GESTIONAR')")
    public ResponseEntity<?> aprobarFase(@PathVariable Long faseId,
                                         @RequestParam(required = false) Long tutorUsuarioId,
                                         @RequestParam(required = false) String comentario) {
        try {
            Long realTutorUsuarioId = obtenerUsuarioAutenticado().getId();
            TutoriaFaseDTO fase = tutoriaService.aprobarFase(faseId, realTutorUsuarioId, comentario);
            return ResponseEntity.ok(ResponseWrapper.success(fase, "Fase aprobada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PostMapping("/fases/{faseId}/mensaje")
    public ResponseEntity<?> enviarMensaje(@PathVariable Long faseId,
                                           @RequestParam(required = false) Long remitenteId,
                                           @RequestBody NuevoMensajeRequest request) {
        try {
            Long realRemitenteId = obtenerUsuarioAutenticado().getId();
            TutoriaMensajeDTO mensaje = tutoriaService.enviarMensaje(
                    faseId, realRemitenteId, request.getContenido(), request.getTipo());
            return ResponseEntity.ok(ResponseWrapper.success(mensaje, "Mensaje enviado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PutMapping("/fases/{faseId}/leer")
    public ResponseEntity<?> marcarMensajesLeidos(@PathVariable Long faseId,
                                                  @RequestParam(required = false) Long usuarioId) {
        try {
            Long realUsuarioId = obtenerUsuarioAutenticado().getId();
            tutoriaService.marcarMensajesLeidos(faseId, realUsuarioId);
            return ResponseEntity.ok(ResponseWrapper.success(null, "Mensajes marcados como leídos"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    @GetMapping("/fases/{faseId}/pdf")
    public ResponseEntity<?> obtenerPdfFase(@PathVariable Long faseId,
                                            @RequestParam(required = false) Long usuarioId) {
        try {
            Long realUsuarioId = resolverUsuarioId(usuarioId);
            Resource resource = tutoriaService.obtenerPdfFase(faseId, realUsuarioId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    // ── Helpers de seguridad ──────────────────────────────────────────────────

    /**
     * SP (Fase 3): Registra o actualiza el avance de una fase de tutoría vía stored procedure.
     * Llama a presus.sp_registrar_tutoria_avance(p_tutor_id, p_numero_fase, p_archivo_pdf, p_tamano_bytes, p_sha256)
     * Flujo: POST → TutoriaController → TutoriaService → TutoriaFaseRepository → SP → PostgreSQL
     *
     * Body: { "numeroFase": 1, "archivoPdf": "archivo.pdf", "tamanoBytes": 12345, "sha256": "abc..." }
     */
    @PostMapping("/{tutorId}/registrar-avance")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TUTORIA_AVANCE_ESTUDIANTE')")
    public ResponseEntity<?> registrarAvanceSP(
            @PathVariable Long tutorId,
            @RequestBody Map<String, Object> body) {
        try {
            Integer numeroFase = (Integer) body.get("numeroFase");
            String archivoPdf = (String) body.get("archivoPdf");
            Long tamanoBytes = body.get("tamanoBytes") instanceof Number
                    ? ((Number) body.get("tamanoBytes")).longValue() : null;
            String sha256 = (String) body.get("sha256");

            if (numeroFase == null || archivoPdf == null) {
                return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Se requieren 'numeroFase' y 'archivoPdf'"));
            }

            Long realUsuarioId = obtenerUsuarioAutenticado().getId();
            tutoriaService.registrarAvanceSP(tutorId, numeroFase, archivoPdf, tamanoBytes, sha256, realUsuarioId);
            return ResponseEntity.ok(ResponseWrapper.success(Map.of(
                    "mensaje", "Avance de fase registrado correctamente vía stored procedure",
                    "tutorId", tutorId,
                    "numeroFase", numeroFase
            ), "Avance de fase registrado correctamente vía stored procedure"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Usuario no autenticado");
        }
        return usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en el sistema"));
    }

    private Long resolverUsuarioId(Long usuarioIdSolicitado) {
        Usuario autenticado = obtenerUsuarioAutenticado();
        boolean esAdminOCoord = "ADMIN".equalsIgnoreCase(autenticado.getRol())
                || "COORDINADOR".equalsIgnoreCase(autenticado.getRol());
        if (esAdminOCoord && usuarioIdSolicitado != null) {
            return usuarioIdSolicitado;
        }
        return autenticado.getId();
    }
}
