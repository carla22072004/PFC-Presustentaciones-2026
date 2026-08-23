package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstudianteDTO {
    private Long id;
    private Long usuarioId;
    private String nombre;
    private String apellido;
    private String email;
    private Boolean activo;
    private String telefono;
    private String expedienteCodigo;
    private Integer carreraId;
    private String carreraNombre;
    private Integer periodoIngresoId;
    private String periodoIngresoNombre;
    private Short semestreActual;
    private String estadoAcademicoCodigo;
    private String estadoAcademicoNombre;
    /** Tema de la solicitud más reciente del estudiante, si tiene alguna. */
    private String proyectoTitulo;
    private String proyectoEstado;
}
