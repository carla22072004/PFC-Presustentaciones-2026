package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecursoTitulacionDTO {
    private Integer id;
    private String titulo;
    private String categoria;
    private String urlArchivo;
    private Integer carreraId;
    private String carreraNombre;
}
