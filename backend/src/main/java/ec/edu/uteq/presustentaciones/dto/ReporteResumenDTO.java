package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resumen general del proceso de pre-sustentaciones para el dashboard de
 * coordinador/administrador. Todo se calcula con COUNT/GROUP BY en la base.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteResumenDTO {

    private long totalSolicitudes;
    private long solicitudesCompletadas;
    private long solicitudesEnProceso;
    private long solicitudesRechazadas;

    private long totalActas;
    private long actasGeneradas;
    private long actasRevisadas;
    private long actasObservadas;
    private long actasFinalizadas;
    private long actasAnuladas;
    private long actasPendientesFirma;

    private List<ReporteConteoDTO> solicitudesPorEstado;
    private List<ReporteConteoDTO> sustentacionesPorPeriodo;
}
