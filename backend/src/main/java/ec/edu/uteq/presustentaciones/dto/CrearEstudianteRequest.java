package ec.edu.uteq.presustentaciones.dto;

import lombok.Data;

@Data
public class CrearEstudianteRequest {
    private String nombre;
    private String apellido;
    private String email;
    private String password;
    private String telefono;
    private Integer carreraId;
    private Integer periodoIngresoId;
    private Short semestreActual;
}
