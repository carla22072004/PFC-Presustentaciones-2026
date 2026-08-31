package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificacionService {

    /**
     * Crea y persiste una notificación para un usuario, y adicionalmente le envía un correo si
     * tiene configurado un {@code emailNotificaciones}. El remitente que figura en el correo se
     * resuelve del usuario autenticado en el contexto de seguridad actual, o uno genérico si no
     * hay ninguno.
     *
     * @param usuarioId id del usuario receptor de la notificación
     * @param mensaje   texto de la notificación
     * @return la notificación creada
     * @throws RuntimeException si el usuario receptor no existe
     */
    Notificacion crearNotificacion(Long usuarioId, String mensaje);

    /**
     * @param pageable configuración de paginación
     * @return página de todas las notificaciones del sistema
     */
    Page<Notificacion> listarNotificaciones(Pageable pageable);

    /**
     * @param usuarioId id del usuario
     * @param pageable  configuración de paginación
     * @return página de notificaciones de ese usuario, más recientes primero
     */
    Page<Notificacion> listarPorUsuario(Long usuarioId, Pageable pageable);

    /**
     * @param usuarioId id del usuario
     * @return cantidad de notificaciones no leídas de ese usuario
     */
    long contarNoLeidas(Long usuarioId);

    /**
     * @param notificacionId id de la notificación a marcar
     * @return la notificación actualizada con {@code leida = true}
     * @throws RuntimeException si la notificación no existe
     */
    Notificacion marcarComoLeida(Long notificacionId);

    /** @param usuarioId id del usuario cuyas notificaciones se marcan todas como leídas */
    void marcarTodasLeidas(Long usuarioId);
}
