package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionJuradoDTO {
    private Long id;
    private Long solicitudId;
    private Long juradoId;
    private Double notaJurado;
    private String observaciones;
    private String resultado;
    private String comentarioPreestablecido;
    private String nombreJurado;
    private String rolJurado;
}
