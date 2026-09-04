package ec.edu.uteq.presustentaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Alta / edición de un tema propuesto desde el panel de administración del
 * Centro de Orientación (permiso ORIENTACION_CATALOGO_GESTIONAR).
 */
@Data
public class GuardarTemaPropuestoRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 500, message = "El título no puede superar los 500 caracteres")
    private String titulo;

    private String problema;
    private String objetivoGeneral;
    private String objetivosEspecificos;
    private String justificacion;
    private String beneficiarios;

    @Size(max = 50, message = "El nivel de dificultad no puede superar los 50 caracteres")
    private String nivelDificultad;

    private Integer carreraId;
    private Integer lineaInvestigacionId;
    private Integer areaId;
}
