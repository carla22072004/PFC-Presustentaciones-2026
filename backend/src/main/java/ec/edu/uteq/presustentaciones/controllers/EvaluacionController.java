package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.EvaluacionFinal;
import ec.edu.uteq.presustentaciones.services.EvaluacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    @org.springframework.security.access.prepost.PreAuthorize("@permisoService.tienePermiso(authentication, 'EVALUACION_CALIFICAR')")
    public ResponseEntity<?> evaluarPonderado(
            @RequestParam Long solicitudId,
            @RequestParam Long rubricaId,
            @RequestParam Double notaInstructor,
            @RequestParam Double notaJurado,
            @RequestParam String observaciones,
            @RequestParam(defaultValue = "60.0") Double pesoInstructor,
            @RequestParam(defaultValue = "40.0") Double pesoJurado) {
        try {
            EvaluacionFinal e = evaluacionService.evaluarSolicitud(
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
    @org.springframework.security.access.prepost.PreAuthorize("@permisoService.tienePermiso(authentication, 'EVALUACION_CALIFICAR')")
    public EvaluacionFinal evaluar(@RequestParam Long solicitudId,
                              @RequestParam Long rubricaId,
                              @RequestParam Double notaFinal,
                              @RequestParam String observaciones) {
        return evaluacionService.evaluarSolicitud(solicitudId, rubricaId, notaFinal, observaciones);
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("@permisoService.tienePermiso(authentication, 'EVALUACION_CALIFICAR')")
    public ResponseEntity<Page<EvaluacionFinal>> listar(Pageable pageable) {
        return ResponseEntity.ok(evaluacionService.listarEvaluaciones(pageable));
    }

    @GetMapping("/estudiante/{estudianteId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public List<EvaluacionFinal> listarPorEstudiante(@PathVariable Long estudianteId) {
        return evaluacionService.listarPorEstudiante(estudianteId);
    }

    @GetMapping("/usuario/{usuarioId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public List<EvaluacionFinal> listarPorUsuario(@PathVariable Long usuarioId) {
        return evaluacionService.listarPorUsuario(usuarioId);
    }

    @GetMapping("/solicitud/{solicitudId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<EvaluacionFinal> porSolicitud(@PathVariable Long solicitudId) {
        return evaluacionService.buscarPorSolicitud(solicitudId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * RF-09 (SP): Calcula la nota ponderada final vía stored procedure
     * presus.sp_calcular_promedio_evaluacion(p_solicitud_id)
     * Flujo: GET → EvaluacionController → EvaluacionService → EvaluacionFinalRepository → SP → PostgreSQL
     */
    @PostMapping("/calcular-promedio/{solicitudId}")
    @org.springframework.security.access.prepost.PreAuthorize("@permisoService.tienePermiso(authentication, 'EVALUACION_CALIFICAR')")
    public ResponseEntity<?> calcularPromedio(@PathVariable Long solicitudId) {
        try {
            Map<String, Object> resultado = evaluacionService.calcularPromedioSP(solicitudId);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
