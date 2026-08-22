package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Tutor;
import ec.edu.uteq.presustentaciones.services.TutorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/tutores")
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @PostMapping("/asignar")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<Tutor> asignar(@RequestParam Long solicitudId,
                                         @RequestParam Long docenteId) {
        try {
            return ResponseEntity.ok(tutorService.asignarTutor(solicitudId, docenteId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<Tutor> porSolicitud(@PathVariable Long solicitudId) {
        return tutorService.buscarPorSolicitud(solicitudId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<Tutor>> listar(Pageable pageable) {
        return ResponseEntity.ok(tutorService.listarTodos(pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tutorService.eliminarTutor(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * SP (Fase 3): Estadísticas consolidadas del desempeño de tutores.
     * Llama a presus.sp_obtener_estadisticas_tutores()
     * Flujo: GET → TutorController → TutorService → TutorRepository → SP → PostgreSQL
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR', 'DOCENTE')")
    public ResponseEntity<?> estadisticas() {
        try {
            List<Map<String, Object>> stats = tutorService.obtenerEstadisticasTutoresSP();
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
