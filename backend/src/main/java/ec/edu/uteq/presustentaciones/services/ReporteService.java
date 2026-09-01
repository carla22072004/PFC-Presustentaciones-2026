package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.ReporteActividadDocenteDTO;
import ec.edu.uteq.presustentaciones.dto.ReporteConteoDTO;
import ec.edu.uteq.presustentaciones.dto.ReporteResumenDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reportes agregados para COORDINADOR y ADMINISTRADOR (permiso REPORTES_VER, id 18).
 * Todas las cifras se calculan con COUNT/GROUP BY en PostgreSQL — nunca se carga una
 * tabla completa en memoria para contar.
 */
public interface ReporteService {

    /** Resumen general del proceso de pre-sustentaciones (dashboard). */
    ReporteResumenDTO resumen(LocalDate desde, LocalDate hasta, String carrera);

    /** Cantidad de solicitudes/pre-sustentaciones por estado. */
    List<ReporteConteoDTO> solicitudesPorEstado(LocalDate desde, LocalDate hasta, String carrera);

    /** Cantidad de pre-sustentaciones por período académico. */
    List<ReporteConteoDTO> sustentacionesPorPeriodo(LocalDate desde, LocalDate hasta);

    /** Estado de las actas: generadas, revisadas, observadas, finalizadas, anuladas, pendientes de firma. */
    Map<String, Long> resumenActas(LocalDate desde, LocalDate hasta);

    /** Actividad por docente: como jurado, como tutor y actas firmadas. */
    List<ReporteActividadDocenteDTO> actividadPorDocente();

    /** Estadísticas por carrera/programa: total, completadas y rechazadas. */
    List<Map<String, Object>> estadisticasPorCarrera();
}
