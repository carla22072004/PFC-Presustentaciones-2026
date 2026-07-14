package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Evaluacion;
import ec.edu.uteq.presustentaciones.services.EvaluacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/evaluaciones")
public class EvaluacionController {

    private final EvaluacionService evaluacionService;

    public EvaluacionController(EvaluacionService evaluacionService) {
        this.evaluacionService = evaluacionService;
    }

    /**
     * RF-09: Registrar evaluación con ponderación 60/40 configurable.
     * notaInstructor: nota del docente de Titulación (60% por defecto)
     * notaJurado: nota promedio del tribunal (40% por defecto)
     * pesoInstructor / pesoJurado: pesos configurables (deben sumar 100)
     */
    @PostMapping("/evaluar-ponderado")
    public ResponseEntity<?> evaluarPonderado(
            @RequestParam Long solicitudId,
            @RequestParam Long rubricaId,
            @RequestParam Double notaInstructor,
            @RequestParam Double notaJurado,
            @RequestParam String observaciones,
            @RequestParam(defaultValue = "60.0") Double pesoInstructor,
            @RequestParam(defaultValue = "40.0") Double pesoJurado) {
        try {
            Evaluacion e = evaluacionService.evaluarSolicitud(
                    solicitudId, rubricaId,
                    notaInstructor, notaJurado,
                    observaciones, pesoInstructor, pesoJurado);
            return ResponseEntity.ok(e);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /** Endpoint legado: recibe nota final directa */
    @PostMapping("/evaluar")
    public Evaluacion evaluar(@RequestParam Long solicitudId,
                              @RequestParam Long rubricaId,
                              @RequestParam Double notaFinal,
                              @RequestParam String observaciones) {
        return evaluacionService.evaluarSolicitud(solicitudId, rubricaId, notaFinal, observaciones);
    }

    @GetMapping
    public List<Evaluacion> listar() {
        return evaluacionService.listarEvaluaciones();
    }

    @GetMapping("/estudiante/{estudianteId}")
    public List<Evaluacion> listarPorEstudiante(@PathVariable Long estudianteId) {
        return evaluacionService.listarPorEstudiante(estudianteId);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Evaluacion> listarPorUsuario(@PathVariable Long usuarioId) {
        return evaluacionService.listarPorUsuario(usuarioId);
    }

    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<Evaluacion> porSolicitud(@PathVariable Long solicitudId) {
        return evaluacionService.buscarPorSolicitud(solicitudId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
