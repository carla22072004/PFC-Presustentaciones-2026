package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaRequest;
import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaResponse;
import ec.edu.uteq.presustentaciones.dto.ObservacionesSolicitudDTO;

import java.util.List;

public interface RubricaEvaluacionService {
    /** El jurado registra sus escalas por criterio
     * @param request calificación por criterio de rúbrica emitida por un jurado
     * @return la evaluación registrada, con la nota calculada para ese jurado
     * @throws RuntimeException si la solicitud, la rúbrica o el jurado no existen
     */
    EvaluacionRubricaResponse registrarEvaluacion(EvaluacionRubricaRequest request);

    /** Estado de la evaluación de un jurado para una solicitud
     * @param solicitudId id de la solicitud
     * @param juradoId    id del jurado
     * @return la evaluación de ese jurado para esa solicitud, si ya la registró
     */
    EvaluacionRubricaResponse obtenerEvaluacionJurado(Long solicitudId, Long juradoId);

    /** Resumen de todos los jurados para una solicitud
     * @param solicitudId id de la solicitud
     * @return las evaluaciones registradas por cada jurado de esa solicitud
     */
    List<EvaluacionRubricaResponse> obtenerEvaluacionesSolicitud(Long solicitudId);

    /** Nota promedio del tribunal (40%) lista para usar en la evaluación final
     * @param solicitudId id de la solicitud
     * @return el promedio, redondeado a 2 decimales, de las notas de los jurados que ya
     *         evaluaron; {@code 0.0} si ninguno ha evaluado todavía
     */
    Double calcularNotaTribunal(Long solicitudId);

    /** Obtener todas las observaciones de una solicitud (tutor, jurados, coordinador)
     * @param solicitudId id de la solicitud
     * @return observaciones consolidadas de todos los actores que han evaluado la solicitud
     */
    ObservacionesSolicitudDTO obtenerObservacionesSolicitud(Long solicitudId);
}
