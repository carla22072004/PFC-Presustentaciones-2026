package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.EvaluacionJuradoDTO;
import ec.edu.uteq.presustentaciones.services.EvaluacionJuradoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/evaluaciones-jurado")
@RequiredArgsConstructor
public class EvaluacionJuradoController {

    private final EvaluacionJuradoService service;

    /**
     * Registra o actualiza la calificación que un jurado da a una solicitud. El jurado se
     * comprueba contra el usuario autenticado: un docente no puede guardar notas a nombre de
     * otro miembro del tribunal.
     *
     * @param request cuerpo con la solicitud, el jurado y las notas por criterio
     * @return 200 con la evaluación guardada, o 400 con el motivo del rechazo
     */
    @PostMapping("/guardar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'EVALUACION_RUBRICA_REGISTRAR')")
    public ResponseEntity<?> guardar(@RequestBody Map<String, Object> request) {
        try {
            Long solicitudId = Long.valueOf(request.get("solicitudId").toString());
            Long juradoId = Long.valueOf(request.get("juradoId").toString());
            Double notaJurado = Double.valueOf(request.get("notaJurado").toString());
            String observaciones = request.get("observaciones") != null 
                    ? request.get("observaciones").toString() : "";

            EvaluacionJuradoDTO dto = service.guardarEvaluacion(solicitudId, juradoId, notaJurado, observaciones);
            return ResponseEntity.ok(dto);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // deja que GlobalExceptionHandler lo traduzca a 403, no a 400
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Calificación registrada por un jurado concreto sobre una solicitud.
     *
     * @param solicitudId solicitud consultada
     * @param juradoId    jurado del que se quiere ver la calificación
     * @return 200 con la evaluación, o 400 si no existe o no hay acceso
     */
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
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // deja que GlobalExceptionHandler lo traduzca a 403, no a 400
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Calificaciones de todo el tribunal para una solicitud, usadas para calcular la nota
     * promedio del jurado.
     *
     * @param solicitudId solicitud consultada
     * @return 200 con una entrada por miembro del tribunal
     */
    @GetMapping("/tribunal/{solicitudId}")
    public ResponseEntity<List<EvaluacionJuradoDTO>> obtenerTribunal(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(service.obtenerTribunal(solicitudId));
    }
}
