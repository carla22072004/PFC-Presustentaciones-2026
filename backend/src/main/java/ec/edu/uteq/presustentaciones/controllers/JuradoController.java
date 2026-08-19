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
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<?> asignarJurado(
            @RequestParam Long solicitudId,
            @RequestParam Long docenteId,
            @RequestParam String rol) {
        try {
            Jurado j = juradoService.asignarJurado(solicitudId, docenteId, rol);
            return ResponseEntity.ok(j);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Asignación automática de los 3 jurados (PRESIDENTE, VOCAL_1, VOCAL_2) */
    @PostMapping("/asignar-automatico/{solicitudId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<?> asignarAutomaticamente(@PathVariable Long solicitudId) {
        try {
            juradoService.asignarJuradosAutomaticamente(solicitudId);
            List<Jurado> jurados = juradoService.listarPorSolicitud(solicitudId);
            return ResponseEntity.ok(jurados);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Asignación masiva de jurados vía procedimiento almacenado sp_asignar_jurado_masivo.
     * Ejemplo de body: {"solicitudIds":[1,2,3],"docenteIds":[10,11,12],"rolCodigo":"VOCAL"}
     * Toda la operación es una única transacción: si un par falla, se revierte el lote completo.
     */
    @PostMapping("/asignar-masivo")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<?> asignarJuradoMasivo(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Number> solicitudIdsRaw = (List<Number>) body.get("solicitudIds");
            @SuppressWarnings("unchecked")
            List<Number> docenteIdsRaw = (List<Number>) body.get("docenteIds");
            String rolCodigo = (String) body.get("rolCodigo");

            List<Long> solicitudIds = solicitudIdsRaw.stream().map(Number::longValue).toList();
            List<Long> docenteIds = docenteIdsRaw.stream().map(Number::longValue).toList();

            juradoService.asignarJuradoMasivo(solicitudIds, docenteIds, rolCodigo);
            return ResponseEntity.ok(Map.of("asignados", solicitudIds.size()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Listar jurados de una solicitud */
    @GetMapping("/solicitud/{solicitudId}")
    public List<Jurado> listarPorSolicitud(@PathVariable Long solicitudId) {
        return juradoService.listarPorSolicitud(solicitudId);
    }

    /** Listar todos los jurados */
    @GetMapping
    public Page<Jurado> listarTodos(Pageable pageable) {
        return juradoService.listarTodos(pageable);
    }

    /** Eliminar un jurado */
    @DeleteMapping("/{juradoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<Void> eliminarJurado(@PathVariable Long juradoId) {
        juradoService.eliminarJurado(juradoId);
        return ResponseEntity.noContent().build();
    }

    /** Sugerir docentes disponibles para asignar (sin los ya asignados) */
    @GetMapping("/sugerencias/{solicitudId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public List<Docente> sugerirDocentes(
            @PathVariable Long solicitudId,
            @RequestParam(defaultValue = "5") int cantidad) {
        return juradoService.sugerirDocentes(solicitudId, cantidad);
    }

    // ── Tutor ─────────────────────────────────────────────────────────────────

    /** Asignar tutor a una solicitud */
    @PostMapping("/tutor/asignar")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<?> asignarTutor(
            @RequestParam Long solicitudId,
            @RequestParam Long docenteId) {
        try {
            Tutor t = juradoService.asignarTutor(solicitudId, docenteId);
            return ResponseEntity.ok(t);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Obtener el tutor activo de una solicitud */
    @GetMapping("/tutor/solicitud/{solicitudId}")
    public ResponseEntity<Tutor> obtenerTutor(@PathVariable Long solicitudId) {
        return juradoService.obtenerTutorDeSolicitud(solicitudId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Eliminar tutor */
    @DeleteMapping("/tutor/{tutorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<Void> eliminarTutor(@PathVariable Long tutorId) {
        juradoService.eliminarTutor(tutorId);
        return ResponseEntity.noContent().build();
    }

    // ── Vistas del docente como jurado ────────────────────────────────────────

    /** Listar todas las asignaciones de un docente como jurado */
    @GetMapping("/docente/{docenteId}")
    public List<Jurado> listarPorDocente(@PathVariable Long docenteId) {
        return juradoService.listarPorDocente(docenteId);
    }

    /** Listar tutorias de un docente */
    @GetMapping("/tutor/docente/{docenteId}")
    public List<Tutor> listarTutoriasPorDocente(@PathVariable Long docenteId) {
        return juradoService.listarTutoriasPorDocente(docenteId);
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
            return ResponseEntity.ok(Map.of(
                    "id", jurado.getId(),
                    "rol", jurado.getRol() != null ? jurado.getRol() : "",
                    "confirmado", jurado.isConfirmado(),
                    "nombreDocente", nombreDocente
            ));
        }
        return ResponseEntity.ok(null);
    }
}
