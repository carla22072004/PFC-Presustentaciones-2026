package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Notificacion;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.NotificacionRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    @Override
    public Notificacion crearNotificacion(Long usuarioId, String mensaje) {
        Usuario receptor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Notificacion notificacion = notificacionRepository.save(
                Notificacion.builder()
                        .usuario(receptor)
                        .mensaje(mensaje)
                        .fecha(LocalDateTime.now())
                        .leida(false)
                        .build());

        // Obtener remitente desde el contexto de seguridad (usuario logueado)
        String[] remitente = resolverRemitente();

        // Enviar email al correo de notificaciones del receptor (si está configurado)
        String destino = (receptor.getEmailNotificaciones() != null
                && !receptor.getEmailNotificaciones().isBlank())
                ? receptor.getEmailNotificaciones()
                : null;

        if (destino != null) {
            emailService.enviarNotificacion(destino, mensaje, remitente[0], remitente[1]);
        }

        return notificacion;
    }

    /**
     * Resuelve el nombre y email del usuario logueado para usarlo como remitente.
     * Retorna [nombre, email].
     */
    private String[] resolverRemitente() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
                Optional<Usuario> opt = usuarioRepository.findByEmail(auth.getName());
                if (opt.isPresent()) {
                    Usuario u = opt.get();
                    return new String[]{
                            u.getNombre() + " " + u.getApellido(),
                            u.getEmail()
                    };
                }
            }
        } catch (Exception ignored) {
            // Sin contexto de seguridad: usar valor genérico
        }
        return new String[]{"Sistema de Pre-Sustentaciones", "noreply@uteq.edu.ec"};
    }

    @Override
    public List<Notificacion> listarNotificaciones() {
        return notificacionRepository.findAll();
    }

    @Override
    public List<Notificacion> listarPorUsuario(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);
    }

    @Override
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Override
    public Notificacion marcarComoLeida(Long id) {
        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));
        n.setLeida(true);
        return notificacionRepository.save(n);
    }

    @Override
    public void marcarTodasLeidas(Long usuarioId) {
        List<Notificacion> pendientes = notificacionRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);
        pendientes.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(pendientes);
    }
}