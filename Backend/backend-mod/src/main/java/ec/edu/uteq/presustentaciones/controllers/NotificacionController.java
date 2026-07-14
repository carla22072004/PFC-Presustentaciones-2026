package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Notificacion;
import ec.edu.uteq.presustentaciones.services.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @PostMapping("/crear")
    public Notificacion crear(@RequestParam Long usuarioId, @RequestParam String mensaje) {
        return notificacionService.crearNotificacion(usuarioId, mensaje);
    }

    @GetMapping
    public List<Notificacion> listar() {
        return notificacionService.listarNotificaciones();
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Notificacion> listarPorUsuario(@PathVariable Long usuarioId) {
        return notificacionService.listarPorUsuario(usuarioId);
    }

    @GetMapping("/usuario/{usuarioId}/no-leidas")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(Map.of("total", notificacionService.contarNoLeidas(usuarioId)));
    }

    @PatchMapping("/{id}/marcar-leida")
    public ResponseEntity<Notificacion> marcarLeida(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionService.marcarComoLeida(id));
    }

    @PatchMapping("/usuario/{usuarioId}/marcar-todas-leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@PathVariable Long usuarioId) {
        notificacionService.marcarTodasLeidas(usuarioId);
        return ResponseEntity.noContent().build();
    }
}
