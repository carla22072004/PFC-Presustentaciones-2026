package ec.edu.uteq.presustentaciones.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerarTemaRequest {
    @NotNull(message = "El ID de la carrera es obligatorio")
    private Integer carreraId;
    
    private Integer lineaInvestigacionId;
    private Integer areaId;
    private String areaInteres;
    private String tipoProblema;
    private String poblacion;
}
