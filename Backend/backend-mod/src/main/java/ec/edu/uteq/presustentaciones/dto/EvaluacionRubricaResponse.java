package ec.edu.uteq.presustentaciones.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class EvaluacionRubricaResponse {
    private Long solicitudId;
    private Long juradoId;
    private String nombreJurado;
    private String rolJurado;
    /** Detalle por criterio */
    private List<CriterioResultado> detalles;
    /** Nota total de este jurado (suma de notaObtenida de cada criterio) */
    private Double notaTotalJurado;
    /** Nota promedio del tribunal completo (todos los jurados), sobre 10 */
    private Double notaPromedioTribunal;
    /** true si todos los jurados asignados ya evaluaron */
    private boolean tribunalCompleto;

    @Data @Builder
    public static class CriterioResultado {
        private Long criterioId;
        private String nombreCriterio;
        private Double ponderacion;
        private Integer escala;
        private String rangoDescripcion;
        private Double notaObtenida;
        private String observacionAuto;
        private String observacionManual;
        private String observaciones;
    }
}
