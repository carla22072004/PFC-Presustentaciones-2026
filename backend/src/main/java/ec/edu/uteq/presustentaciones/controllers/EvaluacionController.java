package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.EvaluacionFinal;
import ec.edu.uteq.presustentaciones.services.EvaluacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     * RF-09: Registra la evaluación final con ponderación configurable entre la nota del
     * instructor y la del tribunal.
     *
     * @param solicitudId    solicitud de pre-sustentación que se está calificando
     * @param rubricaId      rúbrica con la que se evaluó
     * @param notaInstructor nota del docente de Titulación (pesa 60 % por defecto)
     * @param notaJurado     nota promedio del tribunal (pesa 40 % por defecto)
     * @param observaciones  comentario del evaluador, se persiste junto con la nota
     * @param pesoInstructor peso de la nota del instructor; junto con {@code pesoJurado} debe sumar 100
     * @param pesoJurado     peso de la nota del tribunal
     * @return 200 con la {@link EvaluacionFinal} persistida, o 400 con {@code {"error": ...}}
     *         si el servicio rechaza los pesos o el estado de la solicitud
     */
    @PostMapping("/evaluar-ponderado")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'EVALUACION_CALIFICAR')")
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

    /**
     * Endpoint legado: recibe la nota final ya calculada por el cliente, sin ponderar.
     * Se conserva por compatibilidad con la versión anterior del frontend.
     *
     * @param solicitudId   solicitud que se califica
     * @param rubricaId     rúbrica utilizada
     * @param notaFinal     nota final ya calculada
     * @param observaciones comentario del evaluador
     * @return la {@link EvaluacionFinal} persistida
     */
    @PostMapping("/evaluar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'EVALUACION_CALIFICAR')")
    public EvaluacionFinal evaluar(@RequestParam Long solicitudId,
                              @RequestParam Long rubricaId,
                              @RequestParam Double notaFinal,
                              @RequestParam String observaciones) {
        return evaluacionService.evaluarSolicitud(solicitudId, rubricaId, notaFinal, observaciones);
    }

    /**
     * Lista paginada de todas las evaluaciones finales registradas.
     *
     * @param pageable página y tamaño solicitados por el cliente
     * @return 200 con la página de evaluaciones
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'EVALUACION_CALIFICAR')")
    public ResponseEntity<Page<EvaluacionFinal>> listar(Pageable pageable) {
        return ResponseEntity.ok(evaluacionService.listarEvaluaciones(pageable));
    }

    /**
     * Evaluaciones de un estudiante concreto.
     *
     * @param estudianteId identificador del perfil de estudiante (no del usuario)
     * @return lista de evaluaciones, vacía si el estudiante aún no fue evaluado
     */
    @GetMapping("/estudiante/{estudianteId}")
    @PreAuthorize("isAuthenticated()")
    public List<EvaluacionFinal> listarPorEstudiante(@PathVariable Long estudianteId) {
        return evaluacionService.listarPorEstudiante(estudianteId);
    }

    /**
     * Evaluaciones asociadas a un usuario, resolviendo internamente su perfil de estudiante.
     *
     * @param usuarioId identificador del usuario autenticable
     * @return lista de evaluaciones, vacía si el usuario no tiene perfil de estudiante evaluado
     */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public List<EvaluacionFinal> listarPorUsuario(@PathVariable Long usuarioId) {
        return evaluacionService.listarPorUsuario(usuarioId);
    }

    /**
     * Evaluación final de una solicitud concreta.
     *
     * @param solicitudId solicitud consultada
     * @return 200 con la evaluación, o 404 si la solicitud todavía no fue evaluada
     */
    @GetMapping("/solicitud/{solicitudId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EvaluacionFinal> porSolicitud(@PathVariable Long solicitudId) {
        return evaluacionService.buscarPorSolicitud(solicitudId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * RF-09 (SP): Calcula la nota ponderada final vía stored procedure
     * presus.sp_calcular_promedio_evaluacion(p_solicitud_id).
     * Flujo: POST → EvaluacionController → EvaluacionService → EvaluacionFinalRepository → SP → PostgreSQL
     *
     * @param solicitudId solicitud cuyo promedio se recalcula en la base de datos
     * @return 200 con el mapa {@code {solicitudId, notaFinal, estadoResultado}} que devuelve
     *         el procedimiento, o 400 con {@code {"error": ...}} si el procedimiento no
     *         encuentra evaluaciones por criterio para esa solicitud
     */
    @PostMapping("/calcular-promedio/{solicitudId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'EVALUACION_CALIFICAR')")
    public ResponseEntity<?> calcularPromedio(@PathVariable Long solicitudId) {
        try {
            Map<String, Object> resultado = evaluacionService.calcularPromedioSP(solicitudId);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
