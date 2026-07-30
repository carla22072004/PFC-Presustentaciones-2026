package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Cronograma;
import ec.edu.uteq.presustentaciones.services.CronogramaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/cronogramas")
public class CronogramaController {

    private final CronogramaService cronogramaService;
    public CronogramaController(CronogramaService s) { this.cronogramaService = s; }

    /** Manual con validación de conflictos */
    @PostMapping("/crear")
    public ResponseEntity<?> crear(@RequestParam Long solicitudId,
                                   @RequestParam Long salaId,
                                   @RequestParam LocalDate fecha,
                                   @RequestParam LocalTime hora) {
        try {
            return ResponseEntity.ok(cronogramaService.crearCronograma(solicitudId, salaId, fecha, hora));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** RF-04: Asignación automática sin conflictos */
    @PostMapping("/auto/{solicitudId}")
    public ResponseEntity<?> asignarAutomatico(@PathVariable Long solicitudId) {
        try {
            return ResponseEntity.ok(cronogramaService.asignarAutomatico(solicitudId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** RF-04: Franjas disponibles para una fecha (para mostrar selector en UI) */
    @GetMapping("/disponibilidad")
    public ResponseEntity<Map<String, Object>> disponibilidad(
            @RequestParam LocalDate fecha,
            @RequestParam(defaultValue = "45") int duracion) {
        List<LocalDateTime> franjas = cronogramaService.franjasDisponibles(fecha, duracion);
        return ResponseEntity.ok(Map.of("fecha", fecha, "duracionMin", duracion, "franjas", franjas));
    }

    /** RF-04: Verificar si sala está disponible */
    @GetMapping("/verificar-disponibilidad")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidad(
            @RequestParam Long salaId,
            @RequestParam LocalDateTime inicio,
            @RequestParam(defaultValue = "45") int duracion) {
        boolean disponible = cronogramaService.estaDisponible(salaId, inicio, duracion);
        return ResponseEntity.ok(Map.of("disponible", disponible,
                "mensaje", disponible ? "✓ Sala disponible en esa franja" : "✗ Sala ocupada en esa franja"));
    }

    @GetMapping public List<Cronograma> listar() { return cronogramaService.listarCronogramas(); }
    @GetMapping("/estudiante/{id}") public List<Cronograma> porEstudiante(@PathVariable Long id) { return cronogramaService.listarPorEstudiante(id); }
    @GetMapping("/usuario/{id}") public List<Cronograma> porUsuario(@PathVariable Long id) { return cronogramaService.listarPorUsuario(id); }
    @GetMapping("/solicitud/{id}") public ResponseEntity<Cronograma> porSolicitud(@PathVariable Long id) {
        return cronogramaService.buscarPorSolicitud(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cronogramaService.eliminar(id); return ResponseEntity.noContent().build();
    }
}
