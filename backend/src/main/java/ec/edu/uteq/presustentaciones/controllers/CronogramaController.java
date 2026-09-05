package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Cronograma;
import ec.edu.uteq.presustentaciones.services.CronogramaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/cronogramas")
public class CronogramaController {

    private final CronogramaService cronogramaService;
    public CronogramaController(CronogramaService s) { this.cronogramaService = s; }

    /**
     * RF-04: Programa manualmente una defensa, validando contra sp_validar_conflicto_jurado
     * que ningún jurado ya asignado tenga otra defensa solapada en ese horario.
     *
     * @param solicitudId solicitud que se va a programar
     * @param salaId      sala donde se realizará la defensa
     * @param fecha       día de la defensa
     * @param hora        hora de inicio
     * @return 200 con el {@link Cronograma} creado, o 400 con el motivo del rechazo si la
     *         sala está ocupada o algún jurado tiene conflicto de horario
     */
    @PostMapping("/crear")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'CRONOGRAMA_GESTIONAR')")
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

    /**
     * RF-04: Busca la primera franja libre sin conflictos y programa la defensa ahí.
     *
     * @param solicitudId solicitud que se va a programar
     * @return 200 con el {@link Cronograma} creado, o 400 con el motivo si no queda
     *         ninguna franja disponible
     */
    @PostMapping("/auto/{solicitudId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'CRONOGRAMA_GESTIONAR')")
    public ResponseEntity<?> asignarAutomatico(@PathVariable Long solicitudId) {
        try {
            return ResponseEntity.ok(cronogramaService.asignarAutomatico(solicitudId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * RF-04: Franjas horarias libres de un día, para poblar el selector del frontend.
     *
     * @param fecha    día consultado
     * @param duracion duración de cada franja en minutos (45 por defecto)
     * @return 200 con fecha, duracionMin y la lista de franjas libres
     */
    @GetMapping("/disponibilidad")
    public ResponseEntity<Map<String, Object>> disponibilidad(
            @RequestParam LocalDate fecha,
            @RequestParam(defaultValue = "45") int duracion) {
        List<LocalDateTime> franjas = cronogramaService.franjasDisponibles(fecha, duracion);
        return ResponseEntity.ok(Map.of("fecha", fecha, "duracionMin", duracion, "franjas", franjas));
    }

    /**
     * RF-04: Comprueba si una sala concreta está libre en una franja.
     *
     * @param salaId   sala consultada
     * @param inicio   inicio de la franja
     * @param duracion duración en minutos (45 por defecto)
     * @return 200 con el indicador de disponibilidad y un mensaje legible para la UI
     */
    @GetMapping("/verificar-disponibilidad")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidad(
            @RequestParam Long salaId,
            @RequestParam LocalDateTime inicio,
            @RequestParam(defaultValue = "45") int duracion) {
        boolean disponible = cronogramaService.estaDisponible(salaId, inicio, duracion);
        return ResponseEntity.ok(Map.of("disponible", disponible,
                "mensaje", disponible ? "✓ Sala disponible en esa franja" : "✗ Sala ocupada en esa franja"));
    }

    /**
     * @param pageable página y tamaño solicitados
     * @return 200 con la página de cronogramas programados
     */
    @GetMapping public ResponseEntity<Page<Cronograma>> listar(Pageable pageable) { return ResponseEntity.ok(cronogramaService.listarCronogramas(pageable)); }

    /**
     * @param id identificador del perfil de estudiante
     * @return cronogramas de ese estudiante, vacío si aún no tiene defensa programada
     */
    @GetMapping("/estudiante/{id}") public List<Cronograma> porEstudiante(@PathVariable Long id) { return cronogramaService.listarPorEstudiante(id); }

    /**
     * @param id identificador del usuario autenticable
     * @return cronogramas asociados a ese usuario
     */
    @GetMapping("/usuario/{id}") public List<Cronograma> porUsuario(@PathVariable Long id) { return cronogramaService.listarPorUsuario(id); }

    /**
     * @param id solicitud consultada
     * @return 200 con el cronograma de la solicitud, o 404 si no tiene defensa programada
     */
    @GetMapping("/solicitud/{id}") public ResponseEntity<Cronograma> porSolicitud(@PathVariable Long id) {
        return cronogramaService.buscarPorSolicitud(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    /**
     * Cancela una defensa programada liberando su franja y su sala.
     *
     * @param id cronograma a eliminar
     * @return 204 sin cuerpo
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'CRONOGRAMA_GESTIONAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cronogramaService.eliminar(id); return ResponseEntity.noContent().build();
    }
}
