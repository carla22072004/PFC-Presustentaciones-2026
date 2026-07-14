package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Evaluacion;
import ec.edu.uteq.presustentaciones.entities.Rubrica;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.enums.EstadoSolicitud;
import ec.edu.uteq.presustentaciones.repositories.EvaluacionRepository;
import ec.edu.uteq.presustentaciones.repositories.RubricaRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluacionServiceImpl implements EvaluacionService {

    private final EvaluacionRepository evaluacionRepository;
    private final SolicitudRepository solicitudRepository;
    private final RubricaRepository rubricaRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public Evaluacion evaluarSolicitud(Long solicitudId, Long rubricaId,
                                       Double notaInstructor, Double notaJurado,
                                       String observaciones,
                                       Double pesoInstructor, Double pesoJurado) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + solicitudId));
        Rubrica rubrica = rubricaRepository.findById(rubricaId)
                .orElseThrow(() -> new RuntimeException("Rúbrica no encontrada: " + rubricaId));

        double sumaPesos = (pesoInstructor != null ? pesoInstructor : 60.0)
                + (pesoJurado != null ? pesoJurado : 40.0);
        if (Math.abs(sumaPesos - 100.0) > 0.01) {
            throw new RuntimeException("Los pesos deben sumar 100. Suma actual: " + sumaPesos);
        }

        if (notaInstructor < 0 || notaInstructor > 10 || notaJurado < 0 || notaJurado > 10) {
            throw new RuntimeException("Las notas deben estar entre 0 y 10.");
        }

        Evaluacion e = Evaluacion.builder()
                .solicitud(solicitud)
                .rubrica(rubrica)
                .notaInstructor(notaInstructor)
                .notaJurado(notaJurado)
                .pesoInstructor(pesoInstructor != null ? pesoInstructor : 60.0)
                .pesoJurado(pesoJurado != null ? pesoJurado : 40.0)
                .observaciones(observaciones)
                .build();

        e.calcularNotaFinal();
        e.setComentarioPreestablecido(generarComentarioPorRango(e.getNotaFinal()));
        Evaluacion guardada = evaluacionRepository.save(e);

        // Cambiar estado a CALIFICADA
        solicitud.setEstado(EstadoSolicitud.CALIFICADA);
        solicitudRepository.save(solicitud);

        notificarNotaFinal(solicitud, guardada);

        return guardada;
    }

    @Override
    @Transactional
    public Evaluacion evaluarSolicitud(Long solicitudId, Long rubricaId,
                                       Double notaFinal, String observaciones) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        Rubrica rubrica = rubricaRepository.findById(rubricaId)
                .orElseThrow(() -> new RuntimeException("Rúbrica no encontrada"));

        Evaluacion e = Evaluacion.builder()
                .solicitud(solicitud).rubrica(rubrica)
                .notaFinal(notaFinal).observaciones(observaciones)
                .pesoInstructor(60.0).pesoJurado(40.0)
                .resultado(notaFinal >= 7 ? "APROBADO" : "REPROBADO")
                .build();
        e.setComentarioPreestablecido(generarComentarioPorRango(notaFinal));
        Evaluacion guardada = evaluacionRepository.save(e);

        // Cambiar estado a CALIFICADA
        solicitud.setEstado(EstadoSolicitud.CALIFICADA);
        solicitudRepository.save(solicitud);

        notificarNotaFinal(solicitud, guardada);

        return guardada;
    }

    @Override
    public List<Evaluacion> listarEvaluaciones() {
        return evaluacionRepository.findAll();
    }

    @Override
    public List<Evaluacion> listarPorEstudiante(Long estudianteId) {
        return evaluacionRepository.findByEstudianteId(estudianteId);
    }

    @Override
    public List<Evaluacion> listarPorUsuario(Long usuarioId) {
        return evaluacionRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public Optional<Evaluacion> buscarPorSolicitud(Long solicitudId) {
        return evaluacionRepository.findBySolicitudId(solicitudId);
    }


    public String generarComentarioPorRango(Double notaFinal) {
        if (notaFinal == null) return "";
        if (notaFinal <= 3) {
            return "El trabajo no cumple con los requisitos mínimos esperados. Se evidencian falencias significativas que requieren correcciones sustanciales.";
        } else if (notaFinal <= 6) {
            return "El trabajo presenta un nivel aceptable pero con aspectos que requieren mejoras o correcciones para alcanzar los estándares esperados.";
        } else {
            return "El trabajo cumple satisfactoriamente con los objetivos y requisitos establecidos, demostrando un desempeño adecuado.";
        }
    }

    // ── Notificación nota final ───────────────────────────────────────────────

    private void notificarNotaFinal(Solicitud solicitud, Evaluacion evaluacion) {
        try {
            Long usuarioId = solicitud.getEstudiante().getUsuario().getId();
            String titulo  = solicitud.getTituloTema();
            Double nota    = evaluacion.getNotaFinal();
            String resultado = evaluacion.getResultado() != null ? evaluacion.getResultado()
                    : (nota != null && nota >= 7 ? "APROBADO" : "REPROBADO");

            String emoji = "APROBADO".equals(resultado) ? "🎉" : "😔";
            String msg;

            if (nota != null) {
                msg = String.format(
                        "%s Tu pre-sustentación \"%s\" ha sido evaluada. " +
                                "Nota final: %.2f / 10 — Resultado: %s. Tu solicitud ahora está en fase de calificación.",
                        emoji, titulo, nota, resultado);
            } else {
                msg = String.format(
                        "%s Tu pre-sustentación \"%s\" ha sido evaluada. Resultado: %s. Tu solicitud ahora está en fase de calificación.",
                        emoji, titulo, resultado);
            }

            if (evaluacion.getObservaciones() != null && !evaluacion.getObservaciones().isBlank()) {
                msg += " Observaciones: " + evaluacion.getObservaciones();
            }

            notificacionService.crearNotificacion(usuarioId, msg);
        } catch (Exception e) {
            log.warn("No se pudo notificar nota final al estudiante: {}", e.getMessage());
        }
    }
}
