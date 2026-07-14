package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.EvaluacionFinal;
import ec.edu.uteq.presustentaciones.entities.Rubrica;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.repositories.EvaluacionFinalRepository;
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

    private final EvaluacionFinalRepository evaluacionRepository;
    private final SolicitudRepository solicitudRepository;
    private final RubricaRepository rubricaRepository;
    private final NotificacionService notificacionService;
    private final ec.edu.uteq.presustentaciones.repositories.EstadoSolicitudRepository estadoSolicitudRepository;
    private final ec.edu.uteq.presustentaciones.repositories.ResultadoEvaluacionRepository resultadoEvaluacionRepository;

    @Override
    @Transactional
    public EvaluacionFinal evaluarSolicitud(Long solicitudId, Long rubricaId,
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

        EvaluacionFinal e = EvaluacionFinal.builder()
                .solicitud(solicitud)
                .rubrica(rubrica)
                .notaInstructor(notaInstructor)
                .notaJuradoPromedio(notaJurado)
                .pesoInstructor((pesoInstructor != null ? pesoInstructor : 60.0) / 100.0)
                .pesoJurado((pesoJurado != null ? pesoJurado : 40.0) / 100.0)
                .observaciones(observaciones)
                .build();

        e.calcularNotaFinal();
        
        String resCod = e.getNotaFinal() >= 7.0 ? "APROBADO" : "REPROBADO";
        ec.edu.uteq.presustentaciones.entities.ResultadoEvaluacion res = resultadoEvaluacionRepository.findByCodigo(resCod)
                .orElseGet(() -> resultadoEvaluacionRepository.save(ec.edu.uteq.presustentaciones.entities.ResultadoEvaluacion.builder()
                        .codigo(resCod).nombre(resCod.substring(0,1) + resCod.substring(1).toLowerCase()).build()));
        
        e.setResultado(res);
        e.setComentarioPreestablecido(generarComentarioPorRango(e.getNotaFinal()));
        EvaluacionFinal guardada = evaluacionRepository.save(e);

        // Cambiar estado a CALIFICADA
        ec.edu.uteq.presustentaciones.entities.EstadoSolicitud estadoCalificada = estadoSolicitudRepository.findByCodigo("CALIFICADA")
                .orElseGet(() -> estadoSolicitudRepository.save(ec.edu.uteq.presustentaciones.entities.EstadoSolicitud.builder()
                        .codigo("CALIFICADA").nombre("Calificada").build()));
        solicitud.setEstado(estadoCalificada);
        solicitudRepository.save(solicitud);

        notificarNotaFinal(solicitud, guardada);

        return guardada;
    }

    @Override
    @Transactional
    public EvaluacionFinal evaluarSolicitud(Long solicitudId, Long rubricaId,
                                       Double notaFinal, String observaciones) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        Rubrica rubrica = rubricaRepository.findById(rubricaId)
                .orElseThrow(() -> new RuntimeException("Rúbrica no encontrada"));

        String resCod = notaFinal >= 7 ? "APROBADO" : "REPROBADO";
        ec.edu.uteq.presustentaciones.entities.ResultadoEvaluacion res = resultadoEvaluacionRepository.findByCodigo(resCod)
                .orElseGet(() -> resultadoEvaluacionRepository.save(ec.edu.uteq.presustentaciones.entities.ResultadoEvaluacion.builder()
                        .codigo(resCod).nombre(resCod.substring(0,1) + resCod.substring(1).toLowerCase()).build()));

        EvaluacionFinal e = EvaluacionFinal.builder()
                .solicitud(solicitud).rubrica(rubrica)
                .notaFinal(notaFinal).observaciones(observaciones)
                .pesoInstructor(0.6).pesoJurado(0.4)
                .resultado(res)
                .build();
        e.setComentarioPreestablecido(generarComentarioPorRango(notaFinal));
        EvaluacionFinal guardada = evaluacionRepository.save(e);

        // Cambiar estado a CALIFICADA
        ec.edu.uteq.presustentaciones.entities.EstadoSolicitud estadoCalificada = estadoSolicitudRepository.findByCodigo("CALIFICADA")
                .orElseGet(() -> estadoSolicitudRepository.save(ec.edu.uteq.presustentaciones.entities.EstadoSolicitud.builder()
                        .codigo("CALIFICADA").nombre("Calificada").build()));
        solicitud.setEstado(estadoCalificada);
        solicitudRepository.save(solicitud);

        notificarNotaFinal(solicitud, guardada);

        return guardada;
    }

    @Override
    public List<EvaluacionFinal> listarEvaluaciones() {
        return evaluacionRepository.findAll();
    }

    @Override
    public List<EvaluacionFinal> listarPorEstudiante(Long estudianteId) {
        return evaluacionRepository.findByEstudianteId(estudianteId);
    }

    @Override
    public List<EvaluacionFinal> listarPorUsuario(Long usuarioId) {
        return evaluacionRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public Optional<EvaluacionFinal> buscarPorSolicitud(Long solicitudId) {
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

    private void notificarNotaFinal(Solicitud solicitud, EvaluacionFinal evaluacion) {
        try {
            Long usuarioId = solicitud.getEstudiante().getUsuario().getId();
            String titulo  = solicitud.getTituloTema();
            Double nota    = evaluacion.getNotaFinal();
            
            String resNombre = evaluacion.getResultado() != null ? evaluacion.getResultado().getNombre() : "";
            String resCodigo = evaluacion.getResultado() != null ? evaluacion.getResultado().getCodigo() : "";
            if (resCodigo.isEmpty()) {
                resCodigo = nota != null && nota >= 7 ? "APROBADO" : "REPROBADO";
                resNombre = "APROBADO".equals(resCodigo) ? "Aprobado" : "Reprobado";
            }

            String emoji = "APROBADO".equals(resCodigo) ? "🎉" : "😔";
            String msg;

            if (nota != null) {
                msg = String.format(
                        "%s Tu pre-sustentación \"%s\" ha sido evaluada. " +
                                "Nota final: %.2f / 10 — Resultado: %s. Tu solicitud ahora está en fase de calificación.",
                        emoji, titulo, nota, resNombre);
            } else {
                msg = String.format(
                        "%s Tu pre-sustentación \"%s\" ha sido evaluada. Resultado: %s. Tu solicitud ahora está en fase de calificación.",
                        emoji, titulo, resNombre);
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
