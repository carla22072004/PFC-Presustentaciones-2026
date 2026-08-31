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
     *
     * @param solicitudId id de la solicitud a calcular
     * @return solicitudId, nota final ponderada y estado del resultado ("APROBADO"/"REPROBADO")
     * @throws RuntimeException si la solicitud no existe o el procedimiento no devuelve fila
     */
    PromedioEvaluacionResult calcularPromedioSp(Long solicitudId);

    /** Registra evaluación con notas separadas de instructor y jurado (RF-09)
     * @param solicitudId    id de la solicitud a evaluar
     * @param rubricaId      id de la rúbrica aplicada
     * @param notaInstructor nota del instructor del curso, entre 0 y 10
     * @param notaJurado     nota promedio del tribunal, entre 0 y 10
     * @param observaciones  observaciones opcionales de la evaluación
     * @param pesoInstructor peso del instructor en la ponderación (0-100); {@code null} usa 60
     * @param pesoJurado     peso del jurado en la ponderación (0-100); {@code null} usa 40
     * @return la evaluación final persistida, con nota final calculada y resultado asignado
     * @throws RuntimeException si la solicitud o la rúbrica no existen, los pesos no suman
     *                          100, o alguna nota está fuera de 0-10
     */
    EvaluacionFinal evaluarSolicitud(Long solicitudId, Long rubricaId,
                                 Double notaInstructor, Double notaJurado,
                                 String observaciones,
                                 Double pesoInstructor, Double pesoJurado);

    /** Compatibilidad: evalúa pasando nota final directa (para uso legacy)
     * @param solicitudId   id de la solicitud a evaluar
     * @param rubricaId     id de la rúbrica aplicada
     * @param notaFinal     nota final ya calculada externamente
     * @param observaciones observaciones opcionales
     * @return la evaluación final persistida
     * @throws RuntimeException si la solicitud o la rúbrica no existen
     */
    EvaluacionFinal evaluarSolicitud(Long solicitudId, Long rubricaId,
                                 Double notaFinal, String observaciones);

    /**
     * @param pageable configuración de paginación
     * @return página de todas las evaluaciones finales del sistema
     */
    Page<EvaluacionFinal> listarEvaluaciones(Pageable pageable);

    /**
     * @param estudianteId id del estudiante
     * @return las evaluaciones finales de las solicitudes de ese estudiante
     */
    List<EvaluacionFinal> listarPorEstudiante(Long estudianteId);

    /**
     * @param usuarioId id del usuario autenticado
     * @return las evaluaciones finales visibles para ese usuario
     */
    List<EvaluacionFinal> listarPorUsuario(Long usuarioId);

    /**
     * @param solicitudId id de la solicitud
     * @return la evaluación final de esa solicitud, si ya fue calificada
     */
    Optional<EvaluacionFinal> buscarPorSolicitud(Long solicitudId);

    /**
     * Variante de {@link #calcularPromedioSp} que devuelve el resultado como un mapa
     * genérico en vez de un DTO tipado, para consumo directo desde el controlador.
     *
     * @param solicitudId id de la solicitud a calcular
     * @return mapa con las claves {@code solicitudId}, {@code notaFinal} y
     *         {@code estadoResultado}; vacío si el procedimiento no devolvió filas
     */
    java.util.Map<String, Object> calcularPromedioSP(Long solicitudId);
}
