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

    /**
     * Tutorías en las que el usuario participa como estudiante.
     *
     * @param usuarioId usuario consultado; sólo se respeta si quien pregunta es ADMIN o
     *                  COORDINADOR, en caso contrario se ignora y se usa el del token
     * @return 200 con las tutorías, o 400 si no hay sesión válida
     */
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

    /**
     * Tutorías en las que el usuario participa como docente tutor.
     *
     * @param usuarioId usuario consultado; sólo se respeta para ADMIN o COORDINADOR
     * @return 200 con las tutorías, o 400 si no hay sesión válida
     */
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

    /**
     * Resumen de una tutoría: fases, avance y datos del estudiante.
     *
     * @param tutorId   tutoría consultada
     * @param usuarioId usuario en cuyo nombre se consulta; sólo se respeta para ADMIN o COORDINADOR
     * @return 200 con el resumen, o 400 si no hay acceso a esa tutoría
     */
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

    /**
     * Fases registradas de una tutoría, con su estado y sus archivos.
     *
     * @param tutorId   tutoría consultada
     * @param usuarioId usuario en cuyo nombre se consulta; sólo se respeta para ADMIN o COORDINADOR
     * @return 200 con las fases, o 400 si no hay acceso a esa tutoría
     */
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

    /**
     * Abre una nueva fase de tutoría con la observación del tutor.
     *
     * @param tutorId        tutoría a la que se agrega la fase
     * @param observacion    indicación del tutor para el estudiante
     * @param tutorUsuarioId ignorado; el tutor se resuelve siempre desde el token
     * @return 200 con la fase creada, o 400 con el motivo del rechazo
     */
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

    /**
     * Sube el PDF corregido del estudiante para una fase.
     *
     * @param faseId              fase a la que corresponde el archivo
     * @param archivo             PDF enviado como multipart
     * @param estudianteUsuarioId ignorado; el estudiante se resuelve desde el token
     * @return 200 con la fase actualizada, o 400 si el archivo no es válido
     */
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

    /**
     * Aprueba una fase, habilitando que el estudiante avance a la siguiente.
     *
     * @param faseId         fase a aprobar
     * @param tutorUsuarioId ignorado; el tutor se resuelve desde el token
     * @param comentario     comentario opcional del tutor
     * @return 200 con la fase aprobada, o 400 con el motivo del rechazo
     */
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

    /**
     * Envía un mensaje en el hilo de conversación de una fase.
     *
     * @param faseId      fase sobre la que se conversa
     * @param remitenteId ignorado; el remitente se resuelve desde el token
     * @param request     cuerpo con el contenido y el tipo de mensaje
     * @return 200 con el mensaje creado, o 400 con el motivo del rechazo
     */
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

    /**
     * Marca como leídos los mensajes que el usuario autenticado tiene pendientes en la fase.
     *
     * @param faseId    fase cuyos mensajes se marcan
     * @param usuarioId ignorado; el usuario se resuelve desde el token
     * @return 200 sin datos, o 400 con el motivo del rechazo
     */
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

    /**
     * Descarga en línea el PDF asociado a una fase.
     *
     * @param faseId    fase consultada
     * @param usuarioId usuario en cuyo nombre se consulta; sólo se respeta para ADMIN o COORDINADOR
     * @return 200 con el PDF y cabecera inline, o 400 si la fase no tiene archivo o no hay acceso
     */
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
     *
     * @param tutorId tutoría sobre la que se registra el avance
     * @param body    numeroFase y archivoPdf son obligatorios; tamanoBytes y sha256 son
     *                opcionales y viajan como números/cadenas JSON
     * @return 200 con el tutor y la fase registrados; 400 si faltan los campos obligatorios
     *         o si el procedimiento rechaza el avance (por ejemplo, cuando la fase anterior
     *         todavía no está aprobada)
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

    /**
     * Resuelve el usuario de la sesión actual a partir del token.
     *
     * @return el {@link Usuario} autenticado
     * @throws RuntimeException si no hay sesión, es anónima, o el usuario del token ya no
     *                          existe en la base
     */
    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Usuario no autenticado");
        }
        return usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en el sistema"));
    }

    /**
     * Decide sobre qué usuario se responde: ADMIN y COORDINADOR pueden consultar el de
     * otra persona, cualquier otro rol queda restringido al suyo aunque mande otro id.
     *
     * @param usuarioIdSolicitado usuario pedido por el cliente, puede ser null
     * @return el id sobre el que realmente se debe consultar
     * @throws RuntimeException si no hay un usuario autenticado válido
     */
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
