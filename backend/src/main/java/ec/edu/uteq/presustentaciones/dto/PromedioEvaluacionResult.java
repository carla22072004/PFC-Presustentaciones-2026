package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Proyección del resultado de sp_calcular_promedio_evaluacion (función SQL, RETURNS TABLE),
 * invocada vía @NamedStoredProcedureQuery + @SqlResultSetMapping desde EvaluacionRepository.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PromedioEvaluacionResult {
    private Long solicitudId;
    private Double notaFinal;
    private String estadoResultado;
}
