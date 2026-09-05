package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.ObservacionesSolicitudDTO;
import ec.edu.uteq.presustentaciones.services.RubricaEvaluacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/observaciones")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ObservacionesController {

    private final RubricaEvaluacionService rubricaEvaluacionService;

    /**
     * Observaciones registradas sobre una solicitud (las que el revisor deja al rechazar o
     * al pedir correcciones).
     *
     * @param solicitudId solicitud consultada
     * @return 200 con las observaciones de esa solicitud
     */
    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<?> obtenerObservaciones(@PathVariable Long solicitudId) {
        try {
            ObservacionesSolicitudDTO obs = rubricaEvaluacionService.obtenerObservacionesSolicitud(solicitudId);
            return ResponseEntity.ok(obs);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
