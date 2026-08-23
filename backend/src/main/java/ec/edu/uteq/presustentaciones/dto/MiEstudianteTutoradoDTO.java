package ec.edu.uteq.presustentaciones.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** "Mis Estudiantes" (docente): roster de los estudiantes que el docente tiene
 * asignados como tutor, con sus datos académicos -- distinto de TutoriaResumenDTO,
 * que está enfocado en el progreso de fases/mensajes de la tutoría en sí. */
@Data
@Builder
public class MiEstudianteTutoradoDTO {
    private Long tutorId;
    private Long solicitudId;
    private Long estudianteUsuarioId;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String expedienteCodigo;
    private String carreraNombre;
    private Short semestreActual;
    private String estadoAcademicoCodigo;
    private String estadoAcademicoNombre;
    private String tituloTema;
    private String estadoSolicitudCodigo;
    private String estadoSolicitudNombre;
    private String estadoTutoria;
    private LocalDateTime fechaAsignacion;
}
