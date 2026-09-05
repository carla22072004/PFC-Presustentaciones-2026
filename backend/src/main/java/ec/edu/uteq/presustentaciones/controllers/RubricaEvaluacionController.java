package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaRequest;
import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaResponse;
import ec.edu.uteq.presustentaciones.dto.ObservacionesSolicitudDTO;
import ec.edu.uteq.presustentaciones.entities.CriterioRubrica;
import ec.edu.uteq.presustentaciones.repositories.CriterioRubricaRepository;
import ec.edu.uteq.presustentaciones.services.RubricaEvaluacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/rubrica-evaluacion")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RubricaEvaluacionController {

    private final RubricaEvaluacionService service;
    private final CriterioRubricaRepository criterioRepo;

    /**
     * RF-07: Un jurado registra su calificacion criterio por criterio segun la escala de la
     * rubrica.
     *
     * @param request solicitud, jurado y escala elegida en cada criterio
     * @return 200 con la evaluacion registrada, o 400 con el motivo del rechazo
     */
    @PostMapping("/registrar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'EVALUACION_RUBRICA_REGISTRAR')")
    public ResponseEntity<?> registrar(@RequestBody EvaluacionRubricaRequest request) {
        try {
            EvaluacionRubricaResponse resp = service.registrarEvaluacion(request);
            return ResponseEntity.ok(resp);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // deja que GlobalExceptionHandler lo traduzca a 403, no a 400
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Evaluacion registrada por un jurado concreto.
     *
     * @param solicitudId solicitud consultada
     * @param juradoId    jurado del que se quiere la evaluacion
     * @return 200 con la evaluacion, o 400 si no existe
     */
    @GetMapping("/solicitud/{solicitudId}/jurado/{juradoId}")
    public ResponseEntity<?> obtenerJurado(
            @PathVariable Long solicitudId,
            @PathVariable Long juradoId) {
        try {
            return ResponseEntity.ok(service.obtenerEvaluacionJurado(solicitudId, juradoId));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // deja que GlobalExceptionHandler lo traduzca a 403, no a 404
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * @param solicitudId solicitud consultada
     * @return evaluaciones de todos los jurados del tribunal para esa solicitud
     */
    @GetMapping("/solicitud/{solicitudId}")
    public List<EvaluacionRubricaResponse> obtenerSolicitud(@PathVariable Long solicitudId) {
        return service.obtenerEvaluacionesSolicitud(solicitudId);
    }

    /**
     * Nota promedio del tribunal, que es la que entra con peso 40 % en la evaluacion final.
     *
     * @param solicitudId solicitud consultada
     * @return 200 siempre: si ya hay evaluaciones devuelve {@code {nota}}; si todavia no hay
     *         ninguna devuelve {@code {nota: null, mensaje}} en vez de un error, para que el
     *         formulario de evaluacion final pueda mostrar el aviso sin tratarlo como fallo
     */
    @GetMapping("/nota-tribunal/{solicitudId}")
    public ResponseEntity<?> notaTribunal(@PathVariable Long solicitudId) {
        Double nota = service.calcularNotaTribunal(solicitudId);
        if (nota == null) {
            return ResponseEntity.ok(Map.of("nota", (Object) null,
                    "mensaje", "No hay evaluaciones registradas aún."));
        }
        return ResponseEntity.ok(Map.of("nota", nota));
    }

    /**
     * @param rubricaId rubrica consultada
     * @return criterios de esa rubrica, para pintar el formulario de calificacion
     */
    @GetMapping("/criterios/{rubricaId}")
    public List<CriterioRubrica> criteriosPorRubrica(@PathVariable Long rubricaId) {
        return criterioRepo.findByRubricaIdOrderByOrdenAsc(rubricaId);
    }

    /**
     * Observaciones de todos los actores sobre una solicitud (tutor, jurados y coordinador),
     * consolidadas en una sola respuesta.
     *
     * @param solicitudId solicitud consultada
     * @return 200 con las observaciones consolidadas, o 400 si no existe la solicitud
     */
    @GetMapping("/observaciones/{solicitudId}")
    public ResponseEntity<?> obtenerObservaciones(@PathVariable Long solicitudId) {
        try {
            ObservacionesSolicitudDTO obs = service.obtenerObservacionesSolicitud(solicitudId);
            return ResponseEntity.ok(obs);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // deja que GlobalExceptionHandler lo traduzca a 403, no a 400
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
