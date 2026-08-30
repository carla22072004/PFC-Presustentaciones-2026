package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.PromedioEvaluacionResult;
import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * calcularPromedioSp() invoca sp_calcular_promedio_evaluacion (Fase 3 / Criterio P1) via
 * EvaluacionRepository.calcularPromedioEvaluacion() (@NamedStoredProcedureQuery) -- sin test
 * dedicado pese a que COVERAGE.md lo declara explicitamente como brecha ("solo se verifico
 * manualmente contra Docker"). evaluarSolicitud() tambien tiene reglas de negocio reales
 * (pesos deben sumar 100, notas entre 0-10) sin cobertura.
 */
@ExtendWith(MockitoExtension.class)
class EvaluacionServiceImplTest {

    @Mock private EvaluacionFinalRepository evaluacionRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private RubricaRepository rubricaRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private EstadoSolicitudRepository estadoSolicitudRepository;
    @Mock private ResultadoEvaluacionRepository resultadoEvaluacionRepository;
    @Mock private EvaluacionRepository evaluacionSpRepository;

    @InjectMocks
    private EvaluacionServiceImpl evaluacionService;

    private Solicitud solicitud;
    private Usuario usuarioEstudiante;

    @BeforeEach
    void setUp() {
        usuarioEstudiante = Usuario.builder().id(10L).nombre("Ana").apellido("Torres").build();
        Estudiante estudiante = Estudiante.builder().id(3L).usuario(usuarioEstudiante).build();
        solicitud = Solicitud.builder().id(7L).estudiante(estudiante).tituloTema("Sistema X").build();
    }

    @Test
    void calcularPromedioSpLanzaExcepcionSiLaSolicitudNoExiste() {
        when(solicitudRepository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> evaluacionService.calcularPromedioSp(7L));
    }

    @Test
    void calcularPromedioSpCreaLaFilaBaseSiNoExisteYLuegoInvocaElProcedimiento() {
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(solicitud));
        when(evaluacionSpRepository.findBySolicitudId(7L)).thenReturn(Optional.empty());
        when(evaluacionRepository.findBySolicitudId(7L)).thenReturn(Optional.empty());
        when(evaluacionSpRepository.save(any(Evaluacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(evaluacionSpRepository.calcularPromedioEvaluacion(7L))
                .thenReturn(List.of(new PromedioEvaluacionResult(7L, 8.5, "APROBADO")));

        PromedioEvaluacionResult resultado = evaluacionService.calcularPromedioSp(7L);

        assertEquals(7L, resultado.getSolicitudId());
        assertEquals(8.5, resultado.getNotaFinal());
        assertEquals("APROBADO", resultado.getEstadoResultado());
        verify(evaluacionSpRepository).save(any(Evaluacion.class));
        verify(evaluacionSpRepository).calcularPromedioEvaluacion(7L);
    }

    @Test
    void calcularPromedioSpNoCreaFilaBaseSiYaExiste() {
        Evaluacion existente = Evaluacion.builder().id(1L).solicitud(solicitud).notaInstructor(8.0).build();
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(solicitud));
        when(evaluacionSpRepository.findBySolicitudId(7L)).thenReturn(Optional.of(existente));
        when(evaluacionSpRepository.calcularPromedioEvaluacion(7L))
                .thenReturn(List.of(new PromedioEvaluacionResult(7L, 6.0, "REPROBADO")));

        evaluacionService.calcularPromedioSp(7L);

        verify(evaluacionSpRepository, never()).save(any(Evaluacion.class));
    }

    @Test
    void calcularPromedioSpLanzaExcepcionSiElProcedimientoNoDevuelveFilas() {
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(solicitud));
        when(evaluacionSpRepository.findBySolicitudId(7L)).thenReturn(
                Optional.of(Evaluacion.builder().id(1L).solicitud(solicitud).build()));
        when(evaluacionSpRepository.calcularPromedioEvaluacion(7L)).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> evaluacionService.calcularPromedioSp(7L));
        assertTrue(ex.getMessage().contains("no devolvió resultado"));
    }

    @Test
    void evaluarSolicitudRechazaPesosQueNoSumanCien() {
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(solicitud));
        when(rubricaRepository.findById(1L)).thenReturn(Optional.of(Rubrica.builder().id(1L).build()));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                evaluacionService.evaluarSolicitud(7L, 1L, 8.0, 7.0, "obs", 50.0, 40.0));
        assertTrue(ex.getMessage().contains("deben sumar 100"));
    }

    @Test
    void evaluarSolicitudRechazaNotasFueraDeRango() {
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(solicitud));
        when(rubricaRepository.findById(1L)).thenReturn(Optional.of(Rubrica.builder().id(1L).build()));

        assertThrows(RuntimeException.class, () ->
                evaluacionService.evaluarSolicitud(7L, 1L, 11.0, 7.0, "obs", 60.0, 40.0));
    }

    @Test
    void evaluarSolicitudCalculaNotaFinalYCambiaEstadoACalificada() {
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(solicitud));
        when(rubricaRepository.findById(1L)).thenReturn(Optional.of(Rubrica.builder().id(1L).build()));
        when(resultadoEvaluacionRepository.findByCodigo("APROBADO"))
                .thenReturn(Optional.of(ResultadoEvaluacion.builder().codigo("APROBADO").nombre("Aprobado").build()));
        when(evaluacionRepository.save(any(EvaluacionFinal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estadoSolicitudRepository.findByCodigo("CALIFICADA"))
                .thenReturn(Optional.of(EstadoSolicitud.builder().codigo("CALIFICADA").nombre("Calificada").build()));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        EvaluacionFinal resultado = evaluacionService.evaluarSolicitud(7L, 1L, 8.0, 9.0, "Excelente", 60.0, 40.0);

        assertEquals("CALIFICADA", solicitud.getEstado().getCodigo());
        assertEquals("APROBADO", resultado.getResultado().getCodigo());
        verify(notificacionService).crearNotificacion(eq(10L), any());
    }

    @Test
    void generarComentarioPorRangoRetornaVacioSiNotaEsNull() {
        assertEquals("", evaluacionService.generarComentarioPorRango(null));
    }

    @Test
    void generarComentarioPorRangoDistingueLosTresNiveles() {
        assertTrue(evaluacionService.generarComentarioPorRango(2.0).contains("falencias significativas"));
        assertTrue(evaluacionService.generarComentarioPorRango(5.0).contains("aspectos que requieren mejoras"));
        assertTrue(evaluacionService.generarComentarioPorRango(9.0).contains("cumple satisfactoriamente"));
    }
}
