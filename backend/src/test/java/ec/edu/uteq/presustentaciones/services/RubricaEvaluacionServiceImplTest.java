package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Cubre calcularNotaTribunal(), el cálculo real de promedio de evaluación
 * (equivalente en Java a sp_calcular_promedio_evaluacion mencionado en OBSERVACIONES.md).
 */
@ExtendWith(MockitoExtension.class)
class RubricaEvaluacionServiceImplTest {

    @Mock private EvaluacionCriterioRepository evalCriterioRepo;
    @Mock private CriterioRubricaRepository criterioRepo;
    @Mock private JuradoRepository juradoRepo;
    @Mock private SolicitudRepository solicitudRepo;
    @Mock private RubricaRepository rubricaRepo;
    @Mock private TutorRepository tutorRepo;
    @Mock private EvaluacionFinalRepository evaluacionFinalRepo;
    @Mock private EvaluacionJuradoRepository javaEvaluacionJuradoRepo;
    @Mock private EvaluadorRepository evaluadorRepo;
    @Mock private TipoEvaluadorRepository tipoEvaluadorRepo;

    @InjectMocks
    private RubricaEvaluacionServiceImpl service;

    private static Object[] fila(long evaluadorId, double suma) {
        return new Object[]{evaluadorId, suma};
    }

    @BeforeEach
    void setUp() {}

    @Test
    void calcularNotaTribunalPromediaLasSumasDeCadaJurado() {
        // 3 jurados, cada uno con su suma de notas ponderadas por criterio: (90 + 85 + 78) / 3 = 84.33
        List<Object[]> filas = Arrays.asList(fila(1L, 90.0), fila(2L, 85.0), fila(3L, 78.0));
        when(evalCriterioRepo.sumaPorEvaluador(10L)).thenReturn(filas);

        Double nota = service.calcularNotaTribunal(10L);

        assertEquals(84.33, nota, 0.001);
    }

    @Test
    void calcularNotaTribunalRedondeaADosDecimales() {
        List<Object[]> filas = Arrays.asList(fila(1L, 100.0), fila(2L, 100.0), fila(3L, 66.0));
        when(evalCriterioRepo.sumaPorEvaluador(11L)).thenReturn(filas);

        Double nota = service.calcularNotaTribunal(11L);

        // (100 + 100 + 66) / 3 = 88.666... -> 88.67
        assertEquals(88.67, nota);
    }

    @Test
    void calcularNotaTribunalRetornaNullSiNoHayEvaluaciones() {
        when(evalCriterioRepo.sumaPorEvaluador(12L)).thenReturn(Collections.emptyList());

        assertNull(service.calcularNotaTribunal(12L));
    }

    @Test
    void calcularNotaTribunalConUnSoloJuradoDevuelveSuPropiaNota() {
        when(evalCriterioRepo.sumaPorEvaluador(13L)).thenReturn(Collections.singletonList(fila(1L, 95.5)));

        assertEquals(95.5, service.calcularNotaTribunal(13L));
    }
}
