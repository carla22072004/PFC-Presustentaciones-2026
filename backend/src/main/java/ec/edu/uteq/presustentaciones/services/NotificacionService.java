package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificacionService {
    Notificacion crearNotificacion(Long usuarioId, String mensaje);
    Page<Notificacion> listarNotificaciones(Pageable pageable);
    Page<Notificacion> listarPorUsuario(Long usuarioId, Pageable pageable);
    long contarNoLeidas(Long usuarioId);
    Notificacion marcarComoLeida(Long notificacionId);
    void marcarTodasLeidas(Long usuarioId);
}
