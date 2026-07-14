package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.EvaluacionFinal;

import java.util.List;
import java.util.Optional;

public interface EvaluacionService {

    /** Registra evaluación con notas separadas de instructor y jurado (RF-09) */
    EvaluacionFinal evaluarSolicitud(Long solicitudId, Long rubricaId,
                                 Double notaInstructor, Double notaJurado,
                                 String observaciones,
                                 Double pesoInstructor, Double pesoJurado);

    /** Compatibilidad: evalúa pasando nota final directa (para uso legacy) */
    EvaluacionFinal evaluarSolicitud(Long solicitudId, Long rubricaId,
                                 Double notaFinal, String observaciones);

    List<EvaluacionFinal> listarEvaluaciones();
    List<EvaluacionFinal> listarPorEstudiante(Long estudianteId);
    List<EvaluacionFinal> listarPorUsuario(Long usuarioId);
    Optional<EvaluacionFinal> buscarPorSolicitud(Long solicitudId);
}
