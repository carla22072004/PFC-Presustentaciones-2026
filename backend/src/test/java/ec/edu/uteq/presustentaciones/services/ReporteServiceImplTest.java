package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.ReporteActividadDocenteDTO;
import ec.edu.uteq.presustentaciones.dto.ReporteConteoDTO;
import ec.edu.uteq.presustentaciones.dto.ReporteResumenDTO;
import ec.edu.uteq.presustentaciones.repositories.ActaRepository;
import ec.edu.uteq.presustentaciones.repositories.DocenteRepository;
import ec.edu.uteq.presustentaciones.repositories.JuradoRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import ec.edu.uteq.presustentaciones.repositories.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ReporteServiceImpl agrega las cifras del proceso de pre-sustentaciones a partir de
 * consultas GROUP BY (COUNT en la base, nunca cargando la tabla). Estos tests verifican
 * el mapeo Object[] -> DTO y la combinación de actividad por docente.
 */
@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    @Mock private SolicitudRepository solicitudRepository;
    @Mock private ActaRepository actaRepository;
    @Mock private JuradoRepository juradoRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private DocenteRepository docenteRepository;

    @InjectMocks private ReporteServiceImpl reporteService;

    @Test
    void solicitudesPorEstadoMapeaLasFilasAgrupadas() {
        when(solicitudRepository.contarPorEstado(any(), any(), any())).thenReturn(java.util.List.<Object[]>of(
                new Object[]{"COMPLETADA", 9L},
                new Object[]{"RECHAZADA", 2L}));

        List<ReporteConteoDTO> r = reporteService.solicitudesPorEstado(null, null, null);

        assertEquals(2, r.size());
        assertEquals("COMPLETADA", r.get(0).getEtiqueta());
        assertEquals(9L, r.get(0).getCantidad());
    }

    @Test
    void resumenActasRellenaLosEstadosFaltantesConCero() {
        when(actaRepository.contarPorEstado(any(), any())).thenReturn(java.util.List.<Object[]>of(
                new Object[]{"FINALIZADA", 5L}));
        when(actaRepository.countByFirmadaFalse()).thenReturn(3L);

        Map<String, Long> r = reporteService.resumenActas(null, null);

        assertEquals(5L, r.get("FINALIZADA"));
        assertEquals(0L, r.get("GENERADA"));
        assertEquals(0L, r.get("ANULADA"));
        assertEquals(5L, r.get("total"));
        assertEquals(3L, r.get("pendientesFirma"));
    }

    @Test
    void actividadPorDocenteCombinaJuradoTutorYActasFirmadas() {
        when(juradoRepository.contarAsignacionesPorDocente()).thenReturn(java.util.List.<Object[]>of(
                new Object[]{1L, "Luis", "Pérez", 4L}));
        when(tutorRepository.contarTutoriasPorDocente()).thenReturn(java.util.List.<Object[]>of(
                new Object[]{1L, 2L}));
        when(juradoRepository.contarActasFirmadasPorDocente()).thenReturn(java.util.List.<Object[]>of(
                new Object[]{1L, 3L}));

        List<ReporteActividadDocenteDTO> r = reporteService.actividadPorDocente();

        assertEquals(1, r.size());
        ReporteActividadDocenteDTO d = r.get(0);
        assertEquals("Luis Pérez", d.getDocente());
        assertEquals(4L, d.getComoJurado());
        assertEquals(2L, d.getComoTutor());
        assertEquals(3L, d.getActasFirmadas());
        verify(docenteRepository, never()).findNombresByIds(any());
    }

    @Test
    void resumenCalculaTotalesYEnProceso() {
        when(solicitudRepository.contarPorEstado(any(), any(), any())).thenReturn(java.util.List.<Object[]>of(
                new Object[]{"COMPLETADA", 10L},
                new Object[]{"EVALUACION", 5L},
                new Object[]{"RECHAZADA", 3L}));
        when(actaRepository.contarPorEstado(any(), any())).thenReturn(java.util.List.<Object[]>of());
        when(actaRepository.countByFirmadaFalse()).thenReturn(0L);
        when(solicitudRepository.contarPorPeriodo(any(), any())).thenReturn(java.util.List.<Object[]>of());

        ReporteResumenDTO r = reporteService.resumen(null, null, null);

        assertEquals(18L, r.getTotalSolicitudes());
        assertEquals(10L, r.getSolicitudesCompletadas());
        assertEquals(3L, r.getSolicitudesRechazadas());
        assertEquals(5L, r.getSolicitudesEnProceso());
    }
}
