package ec.edu.uteq.presustentaciones.dto;

import lombok.Data;

@Data
public class GuardarCarreraRequest {
    private String codigo;
    private String nombre;
    private Integer facultadId;
    private String modalidadEstudio;
}
