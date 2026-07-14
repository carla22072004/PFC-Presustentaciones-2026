package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaRequest;
import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaResponse;
import ec.edu.uteq.presustentaciones.dto.ObservacionesSolicitudDTO;

import java.util.List;

public interface RubricaEvaluacionService {
    /** El jurado registra sus escalas por criterio */
    EvaluacionRubricaResponse registrarEvaluacion(EvaluacionRubricaRequest request);

    /** Estado de la evaluación de un jurado para una solicitud */
    EvaluacionRubricaResponse obtenerEvaluacionJurado(Long solicitudId, Long juradoId);

    /** Resumen de todos los jurados para una solicitud */
    List<EvaluacionRubricaResponse> obtenerEvaluacionesSolicitud(Long solicitudId);

    /** Nota promedio del tribunal (40%) lista para usar en la evaluación final */
    Double calcularNotaTribunal(Long solicitudId);

    /** Obtener todas las observaciones de una solicitud (tutor, jurados, coordinador) */
    ObservacionesSolicitudDTO obtenerObservacionesSolicitud(Long solicitudId);
}
