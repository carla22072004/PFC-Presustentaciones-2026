package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Actividad de un docente en el proceso de pre-sustentaciones (reporte). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteActividadDocenteDTO {
    private Long docenteId;
    private String docente;
    private long comoJurado;
    private long comoTutor;
    private long actasFirmadas;
}
