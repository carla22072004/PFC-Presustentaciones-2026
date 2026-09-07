package ec.edu.uteq.presustentaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Alta de una prueba de restauración en la bitácora. */
@Data
public class RegistrarPruebaRestauracionRequest {

    @NotBlank(message = "Indica sobre qué respaldo se hizo la prueba")
    private String respaldoNombre;

    @NotBlank
    @Pattern(regexp = "EXITOSA|FALLIDA", message = "El resultado debe ser EXITOSA o FALLIDA")
    private String resultado;

    @Size(max = 200)
    private String responsable;

    @Size(max = 4000)
    private String notas;
}
