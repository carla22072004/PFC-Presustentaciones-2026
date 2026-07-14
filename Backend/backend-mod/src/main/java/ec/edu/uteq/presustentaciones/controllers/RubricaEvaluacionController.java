package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaRequest;
import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaResponse;
import ec.edu.uteq.presustentaciones.dto.ObservacionesSolicitudDTO;
import ec.edu.uteq.presustentaciones.entities.CriterioRubrica;
import ec.edu.uteq.presustentaciones.repositories.CriterioRubricaRepository;
import ec.edu.uteq.presustentaciones.services.RubricaEvaluacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/rubrica-evaluacion")
@RequiredArgsConstructor
public class RubricaEvaluacionController {

    private final RubricaEvaluacionService service;
    private final CriterioRubricaRepository criterioRepo;

    /** RF-07: Jurado registra sus escalas por criterio */
    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody EvaluacionRubricaRequest request) {
        try {
            EvaluacionRubricaResponse resp = service.registrarEvaluacion(request);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Obtener evaluación de un jurado específico */
    @GetMapping("/solicitud/{solicitudId}/jurado/{juradoId}")
    public ResponseEntity<?> obtenerJurado(
            @PathVariable Long solicitudId,
            @PathVariable Long juradoId) {
        try {
            return ResponseEntity.ok(service.obtenerEvaluacionJurado(solicitudId, juradoId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Obtener evaluaciones de todos los jurados para una solicitud */
    @GetMapping("/solicitud/{solicitudId}")
    public List<EvaluacionRubricaResponse> obtenerSolicitud(@PathVariable Long solicitudId) {
        return service.obtenerEvaluacionesSolicitud(solicitudId);
    }

    /** Nota promedio del tribunal lista para usar en la evaluación final (40%) */
    @GetMapping("/nota-tribunal/{solicitudId}")
    public ResponseEntity<?> notaTribunal(@PathVariable Long solicitudId) {
        Double nota = service.calcularNotaTribunal(solicitudId);
        if (nota == null) {
            return ResponseEntity.ok(Map.of("nota", (Object) null,
                    "mensaje", "No hay evaluaciones registradas aún."));
        }
        return ResponseEntity.ok(Map.of("nota", nota));
    }

    /** Listar criterios de una rúbrica */
    @GetMapping("/criterios/{rubricaId}")
    public List<CriterioRubrica> criteriosPorRubrica(@PathVariable Long rubricaId) {
        return criterioRepo.findByRubricaIdOrderByOrdenAsc(rubricaId);
    }

    /** Obtener todas las observaciones de una solicitud (tutor, jurados, coordinador) */
    @GetMapping("/observaciones/{solicitudId}")
    public ResponseEntity<?> obtenerObservaciones(@PathVariable Long solicitudId) {
        try {
            ObservacionesSolicitudDTO obs = service.obtenerObservacionesSolicitud(solicitudId);
            return ResponseEntity.ok(obs);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
