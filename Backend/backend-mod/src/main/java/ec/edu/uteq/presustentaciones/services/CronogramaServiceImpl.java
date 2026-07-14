package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Cronograma;
import ec.edu.uteq.presustentaciones.entities.Jurado;
import ec.edu.uteq.presustentaciones.entities.Sala;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.entities.Tutor;
import ec.edu.uteq.presustentaciones.repositories.CronogramaRepository;
import ec.edu.uteq.presustentaciones.repositories.JuradoRepository;
import ec.edu.uteq.presustentaciones.repositories.SalaRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import ec.edu.uteq.presustentaciones.repositories.TutorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CronogramaServiceImpl implements CronogramaService {

    private final CronogramaRepository cronogramaRepository;
    private final SolicitudRepository solicitudRepository;
    private final SalaRepository salaRepository;
    private final JuradoRepository juradoRepository;
    private final TutorRepository tutorRepository;
    private final NotificacionService notificacionService;

    private static final LocalTime HORA_INICIO = LocalTime.of(8, 0);
    private static final LocalTime HORA_FIN    = LocalTime.of(17, 0);
    private static final int DURACION = 45;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    @Override
    @Transactional
    public Cronograma crearCronograma(Long solicitudId, Long salaId, LocalDate fecha, LocalTime hora) {
        validarPrerequisitosParaCronograma(solicitudId);

        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        Sala sala = salaRepository.findById(salaId)
                .orElseThrow(() -> new RuntimeException("Sala no encontrada"));

        LocalDateTime inicio = LocalDateTime.of(fecha, hora);
        LocalDateTime fin = inicio.plusMinutes(DURACION);

        List<Cronograma> conflictos = cronogramaRepository.findConflictos(salaId, inicio, fin);
        if (!conflictos.isEmpty()) {
            throw new RuntimeException(
                    "Conflicto de horario: la sala '" + sala.getNombre() +
                            "' ya tiene una pre-sustentación programada en esa franja.");
        }

        Cronograma cronograma = cronogramaRepository.save(Cronograma.builder()
                .solicitud(solicitud).sala(sala)
                .fechaInicio(inicio).duracionMin(DURACION).estado("ACTIVO").build());

        notificarProgramacion(cronograma);
        return cronograma;
    }

    @Override
    @Transactional
    public Cronograma asignarAutomatico(Long solicitudId) {
        validarPrerequisitosParaCronograma(solicitudId);

        solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        Optional<Cronograma> existente = cronogramaRepository.findBySolicitudId(solicitudId);
        if (existente.isPresent() && "ACTIVO".equals(existente.get().getEstado())) {
            return existente.get();
        }

        List<Sala> salas = salaRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getDisponible())).toList();
        if (salas.isEmpty()) throw new RuntimeException("No hay salas disponibles.");

        for (int diasAdelantar = 1; diasAdelantar <= 30; diasAdelantar++) {
            LocalDate fecha = LocalDate.now().plusDays(diasAdelantar);
            if (fecha.getDayOfWeek().getValue() >= 6) continue;

            List<LocalDateTime> franjas = franjasDisponibles(fecha, DURACION);
            for (LocalDateTime franja : franjas) {
                for (Sala sala : salas) {
                    List<Cronograma> conflictos = cronogramaRepository
                            .findConflictos(sala.getId(), franja, franja.plusMinutes(DURACION));
                    if (conflictos.isEmpty()) {
                        Solicitud solicitud = solicitudRepository.findById(solicitudId).get();
                        Cronograma cronograma = cronogramaRepository.save(Cronograma.builder()
                                .solicitud(solicitud).sala(sala)
                                .fechaInicio(franja).duracionMin(DURACION).estado("ACTIVO")
                                .build());
                        notificarProgramacion(cronograma);
                        return cronograma;
                    }
                }
            }
        }
        throw new RuntimeException(
                "No se encontró disponibilidad en los próximos 30 días. Verifique las salas o el calendario.");
    }

    private void validarPrerequisitosParaCronograma(Long solicitudId) {
        // 1. Tribunal completo: los 3 roles deben estar asignados
        List<String> rolesAsignados = juradoRepository.findBySolicitudId(solicitudId)
                .stream().map(Jurado::getRol).toList();
        boolean tribunalCompleto = rolesAsignados.contains("PRESIDENTE")
                && rolesAsignados.contains("VOCAL_1")
                && rolesAsignados.contains("VOCAL_2");
        if (!tribunalCompleto) {
            throw new RuntimeException(
                    "No se puede programar la presentación: el tribunal no está completo. " +
                    "Se requieren Presidente, Vocal 1 y Vocal 2.");
        }

        // 2. Tutoría COMPLETADA
        Tutor tutor = tutorRepository.findBySolicitudId(solicitudId)
                .orElseThrow(() -> new RuntimeException(
                        "No se puede programar la presentación: la tutoría no ha sido completada."));
        if (!"COMPLETADA".equals(tutor.getEstado())) {
            throw new RuntimeException(
                    "No se puede programar la presentación: la tutoría no ha sido completada.");
        }
    }

    /** Notifica al estudiante y a los jurados asignados cuando se programa la exposición */
    private void notificarProgramacion(Cronograma c) {
        try {
            Solicitud s = c.getSolicitud();
            String fechaStr = c.getFechaInicio().format(FMT);
            String sala     = c.getSala().getNombre();
            String titulo   = s.getTituloTema();

            // Notificar al estudiante
            Long estudianteUsuarioId = s.getEstudiante().getUsuario().getId();
            notificacionService.crearNotificacion(estudianteUsuarioId,
                    String.format("📅 Tu pre-sustentación \"%s\" ha sido programada para el %s en la sala %s. " +
                            "Duración estimada: %d minutos.", titulo, fechaStr, sala, c.getDuracionMin()));
        } catch (Exception e) {
            log.warn("No se pudo enviar notificación de programación: {}", e.getMessage());
        }
    }

    @Override
    public boolean estaDisponible(Long salaId, LocalDateTime inicio, int duracionMin) {
        return cronogramaRepository.findConflictos(salaId, inicio, inicio.plusMinutes(duracionMin)).isEmpty();
    }

    @Override
    public List<LocalDateTime> franjasDisponibles(LocalDate fecha, int duracionMin) {
        List<LocalDateTime> franjas = new ArrayList<>();
        LocalDateTime cursor = LocalDateTime.of(fecha, HORA_INICIO);
        LocalDateTime limite = LocalDateTime.of(fecha, HORA_FIN);
        while (!cursor.plusMinutes(duracionMin).isAfter(limite)) {
            franjas.add(cursor);
            cursor = cursor.plusMinutes(duracionMin);
        }
        return franjas;
    }

    @Override public List<Cronograma> listarCronogramas() { return cronogramaRepository.findAll(); }
    @Override public List<Cronograma> listarPorEstudiante(Long id) { return cronogramaRepository.findByEstudianteId(id); }
    @Override public List<Cronograma> listarPorUsuario(Long id) { return cronogramaRepository.findByUsuarioId(id); }
    @Override public Optional<Cronograma> buscarPorSolicitud(Long id) { return cronogramaRepository.findBySolicitudId(id); }
    @Override public void eliminar(Long id) { cronogramaRepository.deleteById(id); }
}