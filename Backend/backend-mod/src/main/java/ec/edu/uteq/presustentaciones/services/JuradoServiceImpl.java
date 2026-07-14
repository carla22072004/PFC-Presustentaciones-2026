package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.entities.Jurado;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.entities.Tutor;
import ec.edu.uteq.presustentaciones.enums.EstadoSolicitud;
import ec.edu.uteq.presustentaciones.repositories.DocenteRepository;
import ec.edu.uteq.presustentaciones.repositories.JuradoRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import ec.edu.uteq.presustentaciones.repositories.TutorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JuradoServiceImpl implements JuradoService {

    private final JuradoRepository juradoRepository;
    private final TutorRepository tutorRepository;
    private final DocenteRepository docenteRepository;
    private final SolicitudRepository solicitudRepository;
    private final NotificacionService notificacionService;
    private final EmailService emailService;

    // ── Jurados ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Jurado asignarJurado(Long solicitudId, Long docenteId, String rol) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + solicitudId));
        Docente docente = docenteRepository.findById(docenteId)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado: " + docenteId));

        // Validar que la tutoría esté COMPLETADA antes de asignar tribunal
        Tutor tutor = tutorRepository.findBySolicitudId(solicitudId)
                .orElseThrow(() -> new RuntimeException(
                        "No puedes asignar tribunal: esta solicitud no tiene tutor asignado"));
        if (!"COMPLETADA".equals(tutor.getEstado())) {
            throw new RuntimeException(
                    "No puedes asignar tribunal: la tutoría aún no ha completado las 3 revisiones obligatorias");
        }

        boolean yaAsignado = juradoRepository.findBySolicitudId(solicitudId).stream()
                .anyMatch(j -> j.getDocente().getId().equals(docenteId));
        if (yaAsignado) {
            throw new RuntimeException("El docente ya está asignado como jurado en esta solicitud.");
        }

        List<String> rolesValidos = List.of("PRESIDENTE", "VOCAL_1", "VOCAL_2");
        if (!rolesValidos.contains(rol.toUpperCase())) {
            throw new RuntimeException("Rol inválido. Use: PRESIDENTE, VOCAL_1 o VOCAL_2");
        }

        boolean rolOcupado = juradoRepository.findBySolicitudId(solicitudId).stream()
                .anyMatch(j -> j.getRol().equalsIgnoreCase(rol));
        if (rolOcupado) {
            throw new RuntimeException("El rol '" + rol + "' ya está asignado en esta solicitud.");
        }

        Jurado guardado = crearJuradoSinNotificar(solicitud, docente, rol);

        // Cambiar estado a EVALUACION
        solicitud.setEstado(EstadoSolicitud.EVALUACION);
        solicitudRepository.save(solicitud);

        // Notificar al docente asignado como jurado
        notificarDocenteJurado(docente, solicitud, rol);

        // Notificar al estudiante que se le asignó un jurado
        notificarEstudianteJurado(solicitud, docente, rol);

        return guardado;
    }

    @Override
    public List<Jurado> listarPorSolicitud(Long solicitudId) {
        return juradoRepository.findBySolicitudId(solicitudId);
    }

    @Override
    public List<Jurado> listarTodos() {
        return juradoRepository.findAll();
    }

    @Override
    @Transactional
    public void eliminarJurado(Long juradoId) {
        Jurado jurado = juradoRepository.findById(juradoId)
                .orElseThrow(() -> new RuntimeException("Jurado no encontrado: " + juradoId));
        Docente docente = jurado.getDocente();
        int nuevaCarga = Math.max(0, docente.getCargaHorariaSemanal() - 1);
        docente.setCargaHorariaSemanal(nuevaCarga);
        docenteRepository.save(docente);
        juradoRepository.deleteById(juradoId);
    }

    // ── Tutor ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Tutor asignarTutor(Long solicitudId, Long docenteId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + solicitudId));
        Docente docente = docenteRepository.findById(docenteId)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado: " + docenteId));

        Tutor tutor = tutorRepository.findBySolicitudId(solicitudId)
                .orElse(Tutor.builder().solicitud(solicitud).build());

        tutor.setDocente(docente);
        tutor.setEstado("ACTIVO");

        // Re-fetch tras save para garantizar que todas las asociaciones estén cargadas
        Tutor guardado = tutorRepository.findById(tutorRepository.save(tutor).getId())
                .orElseThrow(() -> new RuntimeException("Error al recuperar el tutor guardado"));

        // Cambiar estado a TUTORIA
        solicitud.setEstado(EstadoSolicitud.TUTORIA);
        solicitudRepository.save(solicitud);

        // Notificar al docente asignado como tutor
        notificarDocenteTutor(docente, solicitud);

        // Notificar al estudiante que tiene tutor asignado
        notificarEstudianteTutor(solicitud, docente);

        return guardado;
    }

    @Override
    public Optional<Tutor> obtenerTutorDeSolicitud(Long solicitudId) {
        return tutorRepository.findBySolicitudId(solicitudId)
                .filter(t -> "ACTIVO".equals(t.getEstado()));
    }

    @Override
    @Transactional
    public void eliminarTutor(Long tutorId) {
        tutorRepository.deleteById(tutorId);
    }

    // ── Sugerencia automática ─────────────────────────────────────────────────

    @Override
    public List<Docente> sugerirDocentes(Long solicitudId, int cantidad) {
        List<Long> idsOcupados = new ArrayList<>();
        juradoRepository.findBySolicitudId(solicitudId)
                .forEach(j -> idsOcupados.add(j.getDocente().getId()));
        tutorRepository.findBySolicitudId(solicitudId)
                .ifPresent(t -> idsOcupados.add(t.getDocente().getId()));

        List<Docente> candidatos = docenteRepository.findDisponiblesOrdenadosPorCarga().stream()
                .filter(d -> !idsOcupados.contains(d.getId()))
                .collect(Collectors.toList());

        if (candidatos.size() < cantidad) {
            candidatos = docenteRepository.findTodosOrdenadosPorCarga().stream()
                    .filter(d -> !idsOcupados.contains(d.getId()))
                    .collect(Collectors.toList());
        }

        return candidatos.stream().limit(cantidad).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void asignarJuradosAutomaticamente(Long solicitudId) {
        // Validar tutoría completada (asignarJurado ya no se llama, validamos aquí)
        Tutor tutor = tutorRepository.findBySolicitudId(solicitudId)
                .orElseThrow(() -> new RuntimeException(
                        "No puedes asignar tribunal: esta solicitud no tiene tutor asignado"));
        if (!"COMPLETADA".equals(tutor.getEstado())) {
            throw new RuntimeException(
                    "No puedes asignar tribunal: la tutoría aún no ha completado las 3 revisiones obligatorias");
        }

        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + solicitudId));

        List<String> rolesOcupados = juradoRepository.findBySolicitudId(solicitudId)
                .stream().map(Jurado::getRol).collect(Collectors.toList());
        List<String> rolesFaltantes = new ArrayList<>(List.of("PRESIDENTE", "VOCAL_1", "VOCAL_2"))
                .stream().filter(r -> !rolesOcupados.contains(r)).collect(Collectors.toList());

        if (rolesFaltantes.isEmpty()) return;

        List<Docente> sugeridos = sugerirDocentes(solicitudId, rolesFaltantes.size());
        if (sugeridos.size() < rolesFaltantes.size()) {
            throw new RuntimeException(
                    "No hay suficientes docentes para asignar automáticamente. " +
                            "Disponibles: " + sugeridos.size() + ", requeridos: " + rolesFaltantes.size());
        }

        // Asignar sin notificar al estudiante en cada iteración
        for (int i = 0; i < rolesFaltantes.size(); i++) {
            Docente docente = sugeridos.get(i);
            String rol = rolesFaltantes.get(i);
            crearJuradoSinNotificar(solicitud, docente, rol);
            notificarDocenteJurado(docente, solicitud, rol);  // cada docente es destinatario distinto
        }

        // Cambiar estado a EVALUACION
        solicitud.setEstado(EstadoSolicitud.EVALUACION);
        solicitudRepository.save(solicitud);

        // Una sola notificación + correo agrupado al estudiante
        List<Jurado> todosJurados = juradoRepository.findBySolicitudId(solicitudId);
        notificarEstudianteTribunalCompleto(solicitud, todosJurados);
    }

    @Override
    public List<Jurado> listarPorDocente(Long docenteId) {
        return juradoRepository.findByDocenteId(docenteId);
    }

    @Override
    public List<Tutor> listarTutoriasPorDocente(Long docenteId) {
        return tutorRepository.findByDocenteId(docenteId);
    }

    @Override
    public Optional<Jurado> obtenerInfoJurado(Long solicitudId, Long usuarioId) {
        return juradoRepository.findBySolicitudIdAndUsuarioId(solicitudId, usuarioId);
    }

    // ── Helpers internos ─────────────────────────────────────────────────────

    /** Guarda el jurado y actualiza la carga del docente. No envía ninguna notificación. */
    private Jurado crearJuradoSinNotificar(Solicitud solicitud, Docente docente, String rol) {
        docente.setCargaHorariaSemanal(docente.getCargaHorariaSemanal() + 1);
        docenteRepository.save(docente);
        return juradoRepository.save(Jurado.builder()
                .solicitud(solicitud)
                .docente(docente)
                .rol(rol.toUpperCase())
                .confirmado(false)
                .build());
    }

    /** Una sola notificación en BD + un solo correo al estudiante con el tribunal completo. */
    private void notificarEstudianteTribunalCompleto(Solicitud solicitud, List<Jurado> jurados) {
        try {
            String presidente = jurados.stream().filter(j -> "PRESIDENTE".equals(j.getRol()))
                    .map(j -> j.getDocente().getUsuario().getNombre() + " " + j.getDocente().getUsuario().getApellido())
                    .findFirst().orElse("-");
            String vocal1 = jurados.stream().filter(j -> "VOCAL_1".equals(j.getRol()))
                    .map(j -> j.getDocente().getUsuario().getNombre() + " " + j.getDocente().getUsuario().getApellido())
                    .findFirst().orElse("-");
            String vocal2 = jurados.stream().filter(j -> "VOCAL_2".equals(j.getRol()))
                    .map(j -> j.getDocente().getUsuario().getNombre() + " " + j.getDocente().getUsuario().getApellido())
                    .findFirst().orElse("-");

            String mensaje = String.format(
                    "⚖️ Se ha asignado tu tribunal completo para tu pre-sustentación \"%s\". " +
                    "Presidente: %s, Vocal 1: %s, Vocal 2: %s. Tu solicitud ahora está en fase de evaluación.",
                    solicitud.getTituloTema(), presidente, vocal1, vocal2);

            Long estudianteUsuarioId = solicitud.getEstudiante().getUsuario().getId();
            notificacionService.crearNotificacion(estudianteUsuarioId, mensaje);

            String email = solicitud.getEstudiante().getUsuario().getEmailNotificaciones();
            if (email == null || email.isBlank()) {
                email = solicitud.getEstudiante().getUsuario().getEmail();
            }
            emailService.enviarNotificacion(email, mensaje);
        } catch (Exception e) {
            log.warn("No se pudo notificar al estudiante sobre tribunal completo: {}", e.getMessage());
        }
    }

    // ── Helpers de notificación ───────────────────────────────────────────────

    private void notificarDocenteJurado(Docente docente, Solicitud solicitud, String rol) {
        try {
            String rolLabel = switch (rol.toUpperCase()) {
                case "PRESIDENTE" -> "Presidente del tribunal";
                case "VOCAL_1"    -> "Vocal 1 del tribunal";
                case "VOCAL_2"    -> "Vocal 2 del tribunal";
                default           -> rol;
            };
            notificacionService.crearNotificacion(docente.getUsuario().getId(),
                    String.format("⚖️ Has sido asignado como %s para evaluar la pre-sustentación \"%s\" " +
                                    "del estudiante %s %s. Por favor ingresa al sistema para confirmar tu participación.",
                            rolLabel,
                            solicitud.getTituloTema(),
                            solicitud.getEstudiante().getUsuario().getNombre(),
                            solicitud.getEstudiante().getUsuario().getApellido()));
        } catch (Exception e) {
            log.warn("No se pudo notificar al docente jurado: {}", e.getMessage());
        }
    }

    private void notificarEstudianteJurado(Solicitud solicitud, Docente docente, String rol) {
        try {
            String rolLabel = switch (rol.toUpperCase()) {
                case "PRESIDENTE" -> "Presidente";
                case "VOCAL_1"    -> "Vocal 1";
                case "VOCAL_2"    -> "Vocal 2";
                default           -> rol;
            };
            notificacionService.crearNotificacion(solicitud.getEstudiante().getUsuario().getId(),
                    String.format("👨‍🏫 Se ha asignado al docente %s %s como %s del tribunal para tu pre-sustentación \"%s\".",
                            docente.getUsuario().getNombre(),
                            docente.getUsuario().getApellido(),
                            rolLabel,
                            solicitud.getTituloTema()));
        } catch (Exception e) {
            log.warn("No se pudo notificar al estudiante sobre jurado: {}", e.getMessage());
        }
    }

    private void notificarDocenteTutor(Docente docente, Solicitud solicitud) {
        try {
            notificacionService.crearNotificacion(docente.getUsuario().getId(),
                    String.format("📚 Has sido asignado como tutor del anteproyecto \"%s\" " +
                                    "del estudiante %s %s. Ingresa al sistema para revisar los detalles.",
                            solicitud.getTituloTema(),
                            solicitud.getEstudiante().getUsuario().getNombre(),
                            solicitud.getEstudiante().getUsuario().getApellido()));
        } catch (Exception e) {
            log.warn("No se pudo notificar al docente tutor: {}", e.getMessage());
        }
    }

    private void notificarEstudianteTutor(Solicitud solicitud, Docente docente) {
        try {
            notificacionService.crearNotificacion(solicitud.getEstudiante().getUsuario().getId(),
                    String.format("🎓 El docente %s %s ha sido asignado como tu tutor para el anteproyecto \"%s\". " +
                                    "Tu solicitud ahora está en fase de tutoría. Puedes ponerte en contacto con él a través del sistema.",
                            docente.getUsuario().getNombre(),
                            docente.getUsuario().getApellido(),
                            solicitud.getTituloTema()));
        } catch (Exception e) {
            log.warn("No se pudo notificar al estudiante sobre tutor: {}", e.getMessage());
        }
    }
}
