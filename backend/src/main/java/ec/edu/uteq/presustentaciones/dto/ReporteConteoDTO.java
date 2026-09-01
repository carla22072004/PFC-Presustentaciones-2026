package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Par etiqueta/cantidad genérico para los reportes agregados (solicitudes por estado,
 * sustentaciones por período, actividad por docente, por carrera, ...). Lo llenan
 * consultas JPQL {@code GROUP BY} — nunca se carga la tabla completa en memoria.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteConteoDTO {
    private String etiqueta;
    private long cantidad;
}
