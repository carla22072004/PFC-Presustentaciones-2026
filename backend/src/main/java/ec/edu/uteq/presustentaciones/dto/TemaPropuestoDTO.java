package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemaPropuestoDTO {
    private Integer id;
    private String titulo;
    private String problema;
    private String objetivoGeneral;
    private String objetivosEspecificos;
    private String justificacion;
    private String beneficiarios;
    private String nivelDificultad;
    private Integer carreraId;
    private String carreraNombre;
    private Integer lineaInvestigacionId;
    private String lineaInvestigacionNombre;
    private Integer areaId;
    private String areaNombre;
    /** true si el estudiante autenticado ya guardó este tema (solo se rellena en listados del estudiante). */
    private Boolean guardado;
}
