package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RF-03: Endpoint de polling para estado en tiempo real.
 * El frontend consulta cada N segundos para reflejar cambios sin WebSocket.
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/estado")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class EstadoTiempoRealController {

    private final SolicitudRepository solicitudRepo;
    private final AnteproyectoRepository anteproyectoRepo;
    private final CronogramaRepository cronogramaRepo;
    private final ActaRepository actaRepo;
    private final EvaluacionRepository evaluacionRepo;

    /**
     * Devuelve el estado completo de una solicitud en un solo request: estado de la
     * solicitud, anteproyecto, cronograma, evaluación y acta. Pensado para que el frontend
     * haga polling cada 15 s sin encadenar cinco llamadas distintas.
     *
     * @param id solicitud consultada
     * @return 200 con el mapa de estado. Las claves de un módulo que todavía no existe para
     *         esa solicitud (sin anteproyecto, sin cronograma, sin evaluación) se
     *         <b>omiten</b> del mapa en vez de venir en null, así que el frontend debe
     *         comprobar presencia. La única excepción es {@code actaGenerada}, que siempre
     *         viene (false si aún no hay acta), más {@code timestamp} con la marca de tiempo
     *         del servidor
     */
    @GetMapping("/solicitud/{id}")
    public ResponseEntity<Map<String, Object>> estadoSolicitud(@PathVariable Long id) {
        Map<String, Object> estado = new HashMap<>();

        solicitudRepo.findById(id).ifPresent(s -> {
            estado.put("solicitudEstado", s.getEstado());
            estado.put("solicitudId", s.getId());
        });

        anteproyectoRepo.findBySolicitudId(id).ifPresent(a -> {
            estado.put("anteproyectoEstado", a.getEstado());
            estado.put("anteproyectoSha256", a.getSha256Hash() != null ? a.getSha256Hash() : null);
            estado.put("anteproyectoIntegridadVerificada", a.getSha256Hash() != null);
        });

        cronogramaRepo.findBySolicitudId(id).ifPresent(c -> {
            estado.put("cronogramaFecha", c.getFechaInicio());
            estado.put("cronogramaSala", c.getSala() != null ? c.getSala().getNombre() : null);
            estado.put("cronogramaEstado", c.getEstado());
        });

        evaluacionRepo.findBySolicitudId(id).ifPresent(e -> {
            estado.put("evaluacionNota", e.getNotaFinal());
            estado.put("evaluacionResultado", e.getResultado());
        });

        actaRepo.findBySolicitudId(id).ifPresent(a -> {
            estado.put("actaGenerada", true);
            estado.put("actaFirmadaPresidente", a.isFirmadaPresidente());
            estado.put("actaFirmadaVocal1", a.isFirmadaVocal1());
            estado.put("actaFirmadaVocal2", a.isFirmadaVocal2());
            estado.put("actaFirmadaTutor", a.isFirmadaTutor());
            estado.put("actaCompleta", a.isFirmada());
        });

        if (!estado.containsKey("actaGenerada")) estado.put("actaGenerada", false);
        estado.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(estado);
    }

    /**
     * Estado resumido de varias solicitudes en un solo request, para la lista de
     * "mis asignaciones" del docente: evita una llamada por fila de la tabla.
     *
     * @param solicitudIds identificadores de las solicitudes a consultar
     * @return 200 con un mapa de id de solicitud a su estado resumido
     */
    @PostMapping("/solicitudes/batch")
    public ResponseEntity<Map<Long, Map<String, Object>>> estadoBatch(
            @RequestBody java.util.List<Long> solicitudIds) {
        Map<Long, Map<String, Object>> resultado = new HashMap<>();
        for (Long id : solicitudIds) {
            Map<String, Object> est = new HashMap<>();
            solicitudRepo.findById(id).ifPresent(s -> est.put("estado", s.getEstado()));
            evaluacionRepo.findBySolicitudId(id).ifPresent(e -> est.put("evaluada", true));
            if (!est.containsKey("evaluada")) est.put("evaluada", false);
            resultado.put(id, est);
        }
        return ResponseEntity.ok(resultado);
    }
}
