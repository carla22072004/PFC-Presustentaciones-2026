package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.PromedioEvaluacionResult;
import ec.edu.uteq.presustentaciones.entities.EvaluacionFinal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface EvaluacionService {

    /**
     * Agrega las notas por criterio del tribunal (evaluaciones_criterio) con la nota del
     * instructor y persiste nota_final/estado_resultado vía sp_calcular_promedio_evaluacion
     * (Fase 3 / Criterio P1, categoría "cálculos agregados").
     */
    PromedioEvaluacionResult calcularPromedioSp(Long solicitudId);

    /** Registra evaluación con notas separadas de instructor y jurado (RF-09) */
    EvaluacionFinal evaluarSolicitud(Long solicitudId, Long rubricaId,
                                 Double notaInstructor, Double notaJurado,
                                 String observaciones,
                                 Double pesoInstructor, Double pesoJurado);

    /** Compatibilidad: evalúa pasando nota final directa (para uso legacy) */
    EvaluacionFinal evaluarSolicitud(Long solicitudId, Long rubricaId,
                                 Double notaFinal, String observaciones);

    Page<EvaluacionFinal> listarEvaluaciones(Pageable pageable);
    List<EvaluacionFinal> listarPorEstudiante(Long estudianteId);
    List<EvaluacionFinal> listarPorUsuario(Long usuarioId);
    Optional<EvaluacionFinal> buscarPorSolicitud(Long solicitudId);
}
