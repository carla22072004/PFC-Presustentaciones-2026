package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.NuevoMensajeRequest;
import ec.edu.uteq.presustentaciones.dto.TutoriaFaseDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaMensajeDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaResumenDTO;
import ec.edu.uteq.presustentaciones.services.TutoriaService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tutorias")
@CrossOrigin(origins = "http://localhost:4200")
public class TutoriaController {

    private final TutoriaService tutoriaService;

    public TutoriaController(TutoriaService tutoriaService) {
        this.tutoriaService = tutoriaService;
    }

    // ── Listados por usuario ──────────────────────────────────────────────────

    @GetMapping("/estudiante/{usuarioId}")
    public ResponseEntity<?> obtenerTutoriasEstudiante(@PathVariable Long usuarioId) {
        try {
            List<TutoriaResumenDTO> resultado = tutoriaService.obtenerTutoriasEstudiante(usuarioId);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/docente/{usuarioId}")
    public ResponseEntity<?> obtenerTutoriasDocente(@PathVariable Long usuarioId) {
        try {
            List<TutoriaResumenDTO> resultado = tutoriaService.obtenerTutoriasDocente(usuarioId);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Resumen y fases ───────────────────────────────────────────────────────

    @GetMapping("/{tutorId}/resumen")
    public ResponseEntity<?> obtenerResumen(@PathVariable Long tutorId,
                                            @RequestParam Long usuarioId) {
        try {
            TutoriaResumenDTO resumen = tutoriaService.obtenerResumen(tutorId, usuarioId);
            return ResponseEntity.ok(resumen);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{tutorId}/fases")
    public ResponseEntity<?> obtenerFases(@PathVariable Long tutorId) {
        try {
            List<TutoriaFaseDTO> fases = tutoriaService.obtenerFases(tutorId);
            return ResponseEntity.ok(fases);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Operaciones sobre fases ───────────────────────────────────────────────

    @PostMapping("/{tutorId}/nueva-fase")
    public ResponseEntity<?> crearFaseConObservacion(@PathVariable Long tutorId,
                                                     @RequestParam String observacion,
                                                     @RequestParam Long tutorUsuarioId) {
        try {
            TutoriaFaseDTO fase = tutoriaService.crearFaseConObservacion(tutorId, tutorUsuarioId, observacion);
            return ResponseEntity.ok(fase);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/fases/{faseId}/subir-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirPdfCorregido(@PathVariable Long faseId,
                                               @RequestParam("archivo") MultipartFile archivo,
                                               @RequestParam Long estudianteUsuarioId) {
        try {
            TutoriaFaseDTO fase = tutoriaService.subirPdfCorregido(faseId, archivo, estudianteUsuarioId);
            return ResponseEntity.ok(fase);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/fases/{faseId}/aprobar")
    public ResponseEntity<?> aprobarFase(@PathVariable Long faseId,
                                         @RequestParam Long tutorUsuarioId,
                                         @RequestParam(required = false) String comentario) {
        try {
            TutoriaFaseDTO fase = tutoriaService.aprobarFase(faseId, tutorUsuarioId, comentario);
            return ResponseEntity.ok(fase);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/fases/{faseId}/mensaje")
    public ResponseEntity<?> enviarMensaje(@PathVariable Long faseId,
                                           @RequestParam Long remitenteId,
                                           @RequestBody NuevoMensajeRequest request) {
        try {
            TutoriaMensajeDTO mensaje = tutoriaService.enviarMensaje(
                    faseId, remitenteId, request.getContenido(), request.getTipo());
            return ResponseEntity.ok(mensaje);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/fases/{faseId}/leer")
    public ResponseEntity<?> marcarMensajesLeidos(@PathVariable Long faseId,
                                                  @RequestParam Long usuarioId) {
        try {
            tutoriaService.marcarMensajesLeidos(faseId, usuarioId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    @GetMapping("/fases/{faseId}/pdf")
    public ResponseEntity<?> obtenerPdfFase(@PathVariable Long faseId) {
        try {
            Resource resource = tutoriaService.obtenerPdfFase(faseId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
