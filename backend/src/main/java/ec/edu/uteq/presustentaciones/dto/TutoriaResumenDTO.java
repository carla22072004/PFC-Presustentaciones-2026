package ec.edu.uteq.presustentaciones.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class TutoriaResumenDTO {

    private Long tutorId;
    private Long solicitudId;
    private String tituloTema;
    private String nombreEstudiante;
    private String nombreTutor;
    private long totalFases;
    private long fasesAprobadas;
    private String estadoTutoria;
    private long mensajesNoLeidos;
    private boolean solicitudSuspendida;
}
