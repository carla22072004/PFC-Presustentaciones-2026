package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeguimientoDTO {
    private Long solicitudId;
    private String tituloProyecto;
    private String estadoActual; // Estado general de la solicitud
    private int porcentajeProgreso;
    private List<EtapaSeguimientoDTO> etapas;
}
