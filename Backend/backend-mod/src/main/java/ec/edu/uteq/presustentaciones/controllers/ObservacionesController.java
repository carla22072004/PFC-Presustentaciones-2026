package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.ObservacionesSolicitudDTO;
import ec.edu.uteq.presustentaciones.services.RubricaEvaluacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/observaciones")
@RequiredArgsConstructor
public class ObservacionesController {

    private final RubricaEvaluacionService rubricaEvaluacionService;

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
