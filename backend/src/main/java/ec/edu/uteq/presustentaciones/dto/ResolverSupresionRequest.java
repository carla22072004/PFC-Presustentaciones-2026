package ec.edu.uteq.presustentaciones.dto;

import lombok.Data;

/** RNF-19: resolución de una solicitud de supresión de datos personales. */
@Data
public class ResolverSupresionRequest {
    private boolean aceptar;
    private String notas;
}
