package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.EvaluacionJuradoDTO;
import ec.edu.uteq.presustentaciones.services.EvaluacionJuradoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/evaluaciones-jurado")
@RequiredArgsConstructor
public class EvaluacionJuradoController {

    private final EvaluacionJuradoService service;

    @PostMapping("/guardar")
    public ResponseEntity<?> guardar(@RequestBody Map<String, Object> request) {
        try {
            Long solicitudId = Long.valueOf(request.get("solicitudId").toString());
            Long juradoId = Long.valueOf(request.get("juradoId").toString());
            Double notaJurado = Double.valueOf(request.get("notaJurado").toString());
            String observaciones = request.get("observaciones") != null 
                    ? request.get("observaciones").toString() : "";

            EvaluacionJuradoDTO dto = service.guardarEvaluacion(solicitudId, juradoId, notaJurado, observaciones);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{solicitudId}/{juradoId}")
    public ResponseEntity<?> obtener(
            @PathVariable Long solicitudId,
            @PathVariable Long juradoId) {
        try {
            EvaluacionJuradoDTO dto = service.obtenerEvaluacion(solicitudId, juradoId);
            if (dto == null) {
                return ResponseEntity.ok(null);
            }
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tribunal/{solicitudId}")
    public ResponseEntity<List<EvaluacionJuradoDTO>> obtenerTribunal(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(service.obtenerTribunal(solicitudId));
    }
}
