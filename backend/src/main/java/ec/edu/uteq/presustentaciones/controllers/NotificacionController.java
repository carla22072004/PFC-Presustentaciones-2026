package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Notificacion;
import ec.edu.uteq.presustentaciones.services.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @PostMapping("/crear")
    @org.springframework.security.access.prepost.PreAuthorize("@permisoService.tienePermiso(authentication, 'NOTIFICACIONES_ENVIAR')")
    public ResponseEntity<?> crear(@RequestParam Long usuarioId, @RequestParam String mensaje) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(notificacionService.crearNotificacion(usuarioId, mensaje)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("@permisoService.tienePermiso(authentication, 'NOTIFICACIONES_GLOBAL_VER')")
    public ResponseEntity<?> listar(Pageable pageable) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(notificacionService.listarNotificaciones(pageable)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listarPorUsuario(@PathVariable Long usuarioId, Pageable pageable) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(notificacionService.listarPorUsuario(usuarioId, pageable)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @GetMapping("/usuario/{usuarioId}/no-leidas")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> contarNoLeidas(@PathVariable Long usuarioId) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(java.util.Map.of("total", notificacionService.contarNoLeidas(usuarioId))));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/marcar-leida")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> marcarLeida(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(notificacionService.marcarComoLeida(id), "Notificación marcada como leída"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PatchMapping("/usuario/{usuarioId}/marcar-todas-leidas")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> marcarTodasLeidas(@PathVariable Long usuarioId) {
        try {
            notificacionService.marcarTodasLeidas(usuarioId);
            return ResponseEntity.ok(ResponseWrapper.success(null, "Todas las notificaciones marcadas como leídas"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            notificacionService.eliminarNotificacion(id);
            return ResponseEntity.ok(ResponseWrapper.success(null, "Notificación eliminada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }
}
