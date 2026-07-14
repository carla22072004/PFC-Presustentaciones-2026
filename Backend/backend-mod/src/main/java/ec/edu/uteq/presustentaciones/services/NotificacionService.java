package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Notificacion;
import java.util.List;

public interface NotificacionService {
    Notificacion crearNotificacion(Long usuarioId, String mensaje);
    List<Notificacion> listarNotificaciones();
    List<Notificacion> listarPorUsuario(Long usuarioId);
    long contarNoLeidas(Long usuarioId);
    Notificacion marcarComoLeida(Long notificacionId);
    void marcarTodasLeidas(Long usuarioId);
}
