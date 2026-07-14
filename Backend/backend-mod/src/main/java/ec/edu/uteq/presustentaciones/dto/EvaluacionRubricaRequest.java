package ec.edu.uteq.presustentaciones.dto;

import lombok.Data;
import java.util.List;

@Data
public class EvaluacionRubricaRequest {
    private Long solicitudId;
    private Long juradoId;
    private Long rubricaId;
    /** Una entrada por cada criterio de la rúbrica */
    private List<EscalaCriterioDTO> criterios;
    private String observaciones;
}
