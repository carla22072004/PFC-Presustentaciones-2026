package ec.edu.uteq.presustentaciones.dto;

import lombok.Data;

@Data
public class NuevoMensajeRequest {

    private Long faseId;
    private String contenido;
    private String tipo;
}
