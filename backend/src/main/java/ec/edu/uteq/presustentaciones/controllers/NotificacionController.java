package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Notificacion;
import ec.edu.uteq.presustentaciones.services.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /**
     * Crea una notificacion dirigida a un usuario concreto.
     *
     * @param usuarioId destinatario de la notificacion
     * @param mensaje   texto que vera el usuario
     * @return 200 con la notificacion creada
     */
    @PostMapping("/crear")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'NOTIFICACIONES_ENVIAR')")
    public ResponseEntity<?> crear(@RequestParam Long usuarioId, @RequestParam String mensaje) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(notificacionService.crearNotificacion(usuarioId, mensaje)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * @param pageable pagina y tamano solicitados
     * @return 200 con la pagina de notificaciones del sistema
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'NOTIFICACIONES_GLOBAL_VER')")
    public ResponseEntity<?> listar(Pageable pageable) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(notificacionService.listarNotificaciones(pageable)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * @param usuarioId destinatario cuyas notificaciones se consultan
     * @param pageable  pagina y tamano solicitados
     * @return 200 con la pagina de notificaciones de ese usuario
     */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listarPorUsuario(@PathVariable Long usuarioId, Pageable pageable) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(notificacionService.listarPorUsuario(usuarioId, pageable)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * Contador para el badge de la campana del frontend.
     *
     * @param usuarioId usuario consultado
     * @return 200 con la cantidad de notificaciones sin leer
     */
    @GetMapping("/usuario/{usuarioId}/no-leidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> contarNoLeidas(@PathVariable Long usuarioId) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(java.util.Map.of("total", notificacionService.contarNoLeidas(usuarioId))));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * @param id notificacion a marcar como leida
     * @return 200 con la notificacion actualizada
     */
    @PatchMapping("/{id}/marcar-leida")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> marcarLeida(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(notificacionService.marcarComoLeida(id), "Notificación marcada como leída"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * @param usuarioId usuario cuyas notificaciones se marcan todas como leidas
     * @return 200 al confirmar la operacion
     */
    @PatchMapping("/usuario/{usuarioId}/marcar-todas-leidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> marcarTodasLeidas(@PathVariable Long usuarioId) {
        try {
            notificacionService.marcarTodasLeidas(usuarioId);
            return ResponseEntity.ok(ResponseWrapper.success(null, "Todas las notificaciones marcadas como leídas"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * @param id notificacion a eliminar
     * @return 200 al confirmar el borrado
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            notificacionService.eliminarNotificacion(id);
            return ResponseEntity.ok(ResponseWrapper.success(null, "Notificación eliminada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }
}
