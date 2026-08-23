package ec.edu.uteq.presustentaciones.dto;

import lombok.Data;

@Data
public class ActualizarEstudianteRequest {
    private Integer carreraId;
    private Integer periodoIngresoId;
    private Short semestreActual;
    private String telefono;
    private String estadoAcademicoCodigo;
}
