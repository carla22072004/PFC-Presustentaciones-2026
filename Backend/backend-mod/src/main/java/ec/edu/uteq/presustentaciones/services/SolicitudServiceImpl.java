package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.enums.EstadoSolicitud;
import ec.edu.uteq.presustentaciones.repositories.AnteproyectoRepository;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final EstudianteRepository estudianteRepository;
    private final AnteproyectoRepository anteproyectoRepository;
    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void notificarAdmins(String mensaje) {
        List<Usuario> admins = usuarioRepository.findByRol("ADMIN");
        for (Usuario admin : admins) {
            try {
                notificacionService.crearNotificacion(admin.getId(), mensaje);
            } catch (Exception e) {
                log.warn("No se pudo notificar al coordinador ID {}: {}", admin.getId(), e.getMessage());
            }
        }
    }

    private void notificarEstudiante(Solicitud solicitud, String mensaje) {
        try {
            Long usuarioId = solicitud.getEstudiante().getUsuario().getId();
            notificacionService.crearNotificacion(usuarioId, mensaje);
        } catch (Exception e) {
            log.warn("No se pudo notificar al estudiante de solicitud ID {}: {}", solicitud.getId(), e.getMessage());
        }
    }

    // ─── Métodos ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Solicitud crearSolicitud(Long estudianteId, Solicitud datos) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado con ID: " + estudianteId));
        datos.setEstado(EstadoSolicitud.CREADA);
        datos.setEstudiante(estudiante);
        datos.setCreadoPor(estudiante.getUsuario());
        datos.setActualizadoPor(estudiante.getUsuario());
        datos.setFechaRegistro(LocalDateTime.now());
        datos.setActualizadoEn(LocalDateTime.now());
        return solicitudRepository.save(datos);
    }

    @Override
    @Transactional
    public Solicitud crearSolicitudPorUsuario(Long usuarioId, Solicitud datos) {
        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RuntimeException("No existe perfil de estudiante para el usuario ID: " + usuarioId));
        return crearSolicitud(estudiante.getId(), datos);
    }

    @Override
    public List<Solicitud> listarPorUsuario(Long usuarioId) {
        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RuntimeException("No existe perfil de estudiante para el usuario ID: " + usuarioId));
        return solicitudRepository.findByEstudianteId(estudiante.getId());
    }

    @Override
    @Transactional
    public Solicitud enviarSolicitud(Long solicitudId) {
        Solicitud s = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        boolean tienePdf = anteproyectoRepository.findBySolicitudId(solicitudId)
                .map(a -> a.getArchivoPdf() != null && !a.getArchivoPdf().isBlank())
                .orElse(false);

        if (!tienePdf) {
            throw new RuntimeException("Debes cargar el PDF del anteproyecto antes de enviar la solicitud a revisión.");
        }

        s.setEstado(EstadoSolicitud.ENVIADA);
        Solicitud guardada = solicitudRepository.save(s);

        String nombreEstudiante = s.getEstudiante().getUsuario().getNombre()
                + " " + s.getEstudiante().getUsuario().getApellido();

        notificarAdmins(String.format(
                "📋 Nueva solicitud de %s: \"%s\" está pendiente de revisión.",
                nombreEstudiante, s.getTituloTema()));

        return guardada;
    }

    @Override
    @Transactional
    public Solicitud aprobarSolicitud(Long solicitudId) {
        Solicitud s = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        s.setEstado(EstadoSolicitud.APROBADA);
        Solicitud guardada = solicitudRepository.save(s);

        notificarEstudiante(s, String.format(
                "✅ Tu solicitud \"%s\" ha sido APROBADA. Pronto se te asignará fecha y tribunal.",
                s.getTituloTema()));

        return guardada;
    }

    @Override
    @Transactional
    public Solicitud rechazarSolicitud(Long solicitudId) {
        return rechazarConObservacion(solicitudId, null);
    }

    @Override
    @Transactional
    public Solicitud rechazarConObservacion(Long solicitudId, String observacion) {
        Solicitud s = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        s.setEstado(EstadoSolicitud.RECHAZADA);
        if (observacion != null && !observacion.isBlank()) {
            s.setObservaciones(observacion);
        }
        Solicitud guardada = solicitudRepository.save(s);

        String obs = (s.getObservaciones() != null && !s.getObservaciones().isBlank())
                ? " Motivo: " + s.getObservaciones() : "";
        notificarEstudiante(s, String.format(
                "❌ Tu solicitud \"%s\" ha sido RECHAZADA.%s Revisa las observaciones.",
                s.getTituloTema(), obs));

        return guardada;
    }

    @Override
    public List<Solicitud> listarSolicitudes() {
        return solicitudRepository.findAllWithEstudiante();
    }

    @Override
    public List<Solicitud> listarPorEstudiante(Long estudianteId) {
        return solicitudRepository.findByEstudianteId(estudianteId);
    }

    @Override
    public Optional<Solicitud> obtenerPorId(Long id) {
        return solicitudRepository.findById(id);
    }

    @Override
    @Transactional
    public Solicitud suspenderSolicitud(Long solicitudId, String motivo) {
        Solicitud s = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (!s.getEstado().esSuspendible()) {
            throw new RuntimeException("La solicitud no puede ser suspendida en su estado actual: " + s.getEstado());
        }

        if (motivo == null || motivo.isBlank()) {
            throw new RuntimeException("Debe especificar el motivo de la suspensión");
        }

        s.setEstado(EstadoSolicitud.SUSPENDIDA);
        s.setMotivoSuspension(motivo);
        s.setSuspendidoEn(LocalDateTime.now());

        Solicitud guardada = solicitudRepository.save(s);
        log.info("Solicitud {} suspendida desde estado {} por motivo: {}", solicitudId, s.getEstado(), motivo);

        notificarEstudiante(s, String.format(
                "🚫 Tu trabajo \"%s\" ha sido SUSPENDIDO. Motivo: %s. No podrás continuar.",
                s.getTituloTema(), motivo));

        return guardada;
    }
}