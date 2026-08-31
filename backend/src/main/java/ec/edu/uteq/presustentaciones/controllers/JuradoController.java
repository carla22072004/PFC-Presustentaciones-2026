package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.entities.Jurado;
import ec.edu.uteq.presustentaciones.entities.Tutor;
import ec.edu.uteq.presustentaciones.services.JuradoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/jurados")
public class JuradoController {

    private final JuradoService juradoService;

    public JuradoController(JuradoService juradoService) {
        this.juradoService = juradoService;
    }

    // ── Jurados ───────────────────────────────────────────────────────────────

    /** Asignar un jurado manualmente a una solicitud */
    @PostMapping("/asignar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> asignarJurado(
            @RequestParam Long solicitudId,
            @RequestParam Long docenteId,
            @RequestParam String rol) {
        try {
            Jurado j = juradoService.asignarJurado(solicitudId, docenteId, rol);
            return ResponseEntity.ok(ResponseWrapper.success(j, "Jurado asignado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /** Asignación automática de los 3 jurados (PRESIDENTE, VOCAL_1, VOCAL_2) */
    @PostMapping("/asignar-automatico/{solicitudId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> asignarAutomaticamente(@PathVariable Long solicitudId) {
        try {
            juradoService.asignarJuradosAutomaticamente(solicitudId);
            List<Jurado> jurados = juradoService.listarPorSolicitud(solicitudId);
            return ResponseEntity.ok(ResponseWrapper.success(jurados, "Jurados asignados automáticamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /** Listar jurados de una solicitud */
    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<?> listarPorSolicitud(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(ResponseWrapper.success(juradoService.listarPorSolicitud(solicitudId)));
    }

    /** Listar todos los jurados */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> listarTodos(Pageable pageable) {
        return ResponseEntity.ok(ResponseWrapper.success(juradoService.listarTodos(pageable)));
    }

    /** Eliminar un jurado */
    @DeleteMapping("/{juradoId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<Void> eliminarJurado(@PathVariable Long juradoId) {
        juradoService.eliminarJurado(juradoId);
        return ResponseEntity.noContent().build();
    }

    /** Sugerir docentes disponibles para asignar (sin los ya asignados) */
    @GetMapping("/sugerencias/{solicitudId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> sugerirDocentes(
            @PathVariable Long solicitudId,
            @RequestParam(defaultValue = "5") int cantidad) {
        return ResponseEntity.ok(ResponseWrapper.success(juradoService.sugerirDocentes(solicitudId, cantidad)));
    }

    // ── Tutor ─────────────────────────────────────────────────────────────────

    /** Asignar tutor a una solicitud */
    @PostMapping("/tutor/asignar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> asignarTutor(
            @RequestParam Long solicitudId,
            @RequestParam Long docenteId) {
        try {
            Tutor t = juradoService.asignarTutor(solicitudId, docenteId);
            return ResponseEntity.ok(ResponseWrapper.success(t, "Tutor asignado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /** Obtener el tutor activo de una solicitud */
    @GetMapping("/tutor/solicitud/{solicitudId}")
    public ResponseEntity<?> obtenerTutor(@PathVariable Long solicitudId) {
        return juradoService.obtenerTutorDeSolicitud(solicitudId)
                .map(t -> ResponseEntity.ok(ResponseWrapper.success(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Eliminar tutor */
    @DeleteMapping("/tutor/{tutorId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<Void> eliminarTutor(@PathVariable Long tutorId) {
        juradoService.eliminarTutor(tutorId);
        return ResponseEntity.noContent().build();
    }

    // ── Vistas del docente como jurado ────────────────────────────────────────

    /** Listar todas las asignaciones de un docente como jurado */
    @GetMapping("/docente/{docenteId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR') or @permisoService.esPropioDocente(authentication, #docenteId)")
    public ResponseEntity<?> listarPorDocente(@PathVariable Long docenteId) {
        return ResponseEntity.ok(ResponseWrapper.success(juradoService.listarPorDocente(docenteId)));
    }

    /** Listar tutorias de un docente */
    @GetMapping("/tutor/docente/{docenteId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR') or @permisoService.esPropioDocente(authentication, #docenteId)")
    public ResponseEntity<?> listarTutoriasPorDocente(@PathVariable Long docenteId) {
        return ResponseEntity.ok(ResponseWrapper.success(juradoService.listarTutoriasPorDocente(docenteId)));
    }

    @GetMapping("/info/{solicitudId}/{usuarioId}")
    public ResponseEntity<?> obtenerInfoJurado(@PathVariable Long solicitudId, @PathVariable Long usuarioId) {
        Optional<Jurado> juradoOpt = juradoService.obtenerInfoJurado(solicitudId, usuarioId);
        if (juradoOpt.isPresent()) {
            Jurado jurado = juradoOpt.get();
            String nombreDocente = "";
            if (jurado.getDocente() != null && jurado.getDocente().getUsuario() != null) {
                nombreDocente = jurado.getDocente().getUsuario().getNombre() + " " 
                        + jurado.getDocente().getUsuario().getApellido();
            }
            return ResponseEntity.ok(ResponseWrapper.success(Map.of(
                    "id", jurado.getId(),
                    "rol", jurado.getRol() != null ? jurado.getRol() : "",
                    "confirmado", jurado.isConfirmado(),
                    "nombreDocente", nombreDocente
            )));
        }
        return ResponseEntity.ok(ResponseWrapper.success(null));
    }

    /**
     * SP (Fase 3): Asignación masiva de jurados por rol.
     * Llama a presus.sp_asignar_jurado_masivo(p_solicitud_ids, p_docente_ids, p_rol)
     * Flujo: POST → JuradoController → JuradoService → JuradoRepository → SP → PostgreSQL
     *
     * Body esperado: { "solicitudIds": [1,2,3], "docenteIds": [4,5,6], "rol": "PRESIDENTE" }
     */
    @PostMapping("/asignar-masivo")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> asignarMasivo(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> solicitudIdsList = (List<Integer>) body.get("solicitudIds");
            @SuppressWarnings("unchecked")
            List<Integer> docenteIdsList   = (List<Integer>) body.get("docenteIds");
            String rol = (String) body.get("rol");

            if (solicitudIdsList == null || docenteIdsList == null || rol == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Se requieren 'solicitudIds', 'docenteIds' y 'rol'"));
            }
            Long[] solicitudIds = solicitudIdsList.stream().map(i -> i.longValue()).toArray(Long[]::new);
            Long[] docenteIds   = docenteIdsList.stream().map(i -> i.longValue()).toArray(Long[]::new);

            juradoService.asignarJuradoMasivoSP(solicitudIds, docenteIds, rol);
            return ResponseEntity.ok(ResponseWrapper.success(Map.of(
                    "mensaje", "Asignación masiva ejecutada correctamente",
                    "asignados", solicitudIds.length,
                    "rol", rol
            ), "Asignación masiva ejecutada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }
}
