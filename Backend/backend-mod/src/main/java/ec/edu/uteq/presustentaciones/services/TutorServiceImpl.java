package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.entities.Tutor;
import ec.edu.uteq.presustentaciones.enums.EstadoSolicitud;
import ec.edu.uteq.presustentaciones.repositories.DocenteRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import ec.edu.uteq.presustentaciones.repositories.TutorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TutorServiceImpl implements TutorService {

    private final TutorRepository tutorRepository;
    private final SolicitudRepository solicitudRepository;
    private final DocenteRepository docenteRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public Tutor asignarTutor(Long solicitudId, Long docenteId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + solicitudId));
        Docente docente = docenteRepository.findById(docenteId)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado: " + docenteId));

        tutorRepository.findBySolicitudId(solicitudId).ifPresent(t -> tutorRepository.delete(t));

        Tutor tutor = Tutor.builder()
                .solicitud(solicitud)
                .docente(docente)
                .estado("ACTIVO")
                .build();
        Tutor guardado = tutorRepository.save(tutor);

        solicitud.setEstado(EstadoSolicitud.TUTORIA);
        solicitudRepository.save(solicitud);

        // Notificar al docente asignado
        try {
            notificacionService.crearNotificacion(docente.getUsuario().getId(),
                    String.format("📚 Has sido asignado como tutor del anteproyecto \"%s\" " +
                                    "del estudiante %s %s.",
                            solicitud.getTituloTema(),
                            solicitud.getEstudiante().getUsuario().getNombre(),
                            solicitud.getEstudiante().getUsuario().getApellido()));
        } catch (Exception e) {
            log.warn("No se pudo notificar al docente tutor: {}", e.getMessage());
        }

        // Notificar al estudiante
        try {
            notificacionService.crearNotificacion(solicitud.getEstudiante().getUsuario().getId(),
                    String.format("🎓 El docente %s %s ha sido asignado como tu tutor para \"%s\". Tu solicitud ahora está en fase de tutoría.",
                            docente.getUsuario().getNombre(),
                            docente.getUsuario().getApellido(),
                            solicitud.getTituloTema()));
        } catch (Exception e) {
            log.warn("No se pudo notificar al estudiante sobre tutor: {}", e.getMessage());
        }

        return guardado;
    }

    @Override
    public Optional<Tutor> buscarPorSolicitud(Long solicitudId) {
        return tutorRepository.findBySolicitudId(solicitudId);
    }

    @Override
    public List<Tutor> listarTodos() {
        return tutorRepository.findAll();
    }

    @Override
    public void eliminarTutor(Long tutorId) {
        tutorRepository.deleteById(tutorId);
    }
}