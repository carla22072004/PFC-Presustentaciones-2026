package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.DepuracionBitacoraLog;
import ec.edu.uteq.presustentaciones.repositories.AuditoriaRepository;
import ec.edu.uteq.presustentaciones.repositories.DepuracionBitacoraLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * RNF-19: depuración automática de la bitácora. No expone ningún endpoint -- ver
 * {@code DepuracionBitacoraScheduler}, {@code @Scheduled} puro -- y deja traza verificable de
 * cuántas entradas borró y hasta qué fecha.
 */
class DepuracionBitacoraSchedulerTest {

    private AuditoriaRepository auditoriaRepository;
    private DepuracionBitacoraLogRepository logRepository;
    private DepuracionBitacoraScheduler scheduler;

    @BeforeEach
    void setUp() {
        auditoriaRepository = mock(AuditoriaRepository.class);
        logRepository = mock(DepuracionBitacoraLogRepository.class);
        scheduler = new DepuracionBitacoraScheduler(auditoriaRepository, logRepository);
        ReflectionTestUtils.setField(scheduler, "retencionDias", 730);
    }

    @Test
    void depurarBorraLoAnteriorAlCorteYDejaTrazaConElConteoExacto() {
        when(auditoriaRepository.borrarAnterioresA(any())).thenReturn(42);

        scheduler.depurar();

        verify(auditoriaRepository).borrarAnterioresA(any(LocalDateTime.class));

        org.mockito.ArgumentCaptor<DepuracionBitacoraLog> captor =
                org.mockito.ArgumentCaptor.forClass(DepuracionBitacoraLog.class);
        verify(logRepository).save(captor.capture());
        DepuracionBitacoraLog traza = captor.getValue();
        assertEquals(42, traza.getEntradasEliminadas());
        assertNotNull(traza.getFechaCorte());
        assertNotNull(traza.getFechaEjecucion());
        // El corte es ~730 dias atras (margen amplio por el tiempo de ejecucion de la prueba).
        long diasDeMargen = ChronoUnit.DAYS.between(traza.getFechaCorte(), LocalDateTime.now());
        assertTrue(diasDeMargen >= 729 && diasDeMargen <= 731,
                "el corte debe ser ~730 dias atras, fue hace " + diasDeMargen + " dias");
    }

    @Test
    void depurarSinNadaQueBorrarTambienDejaTraza() {
        when(auditoriaRepository.borrarAnterioresA(any())).thenReturn(0);

        scheduler.depurar();

        verify(logRepository).save(argThat(traza -> traza.getEntradasEliminadas() == 0));
    }

    @Test
    void elPeriodoDeRetencionEsConfigurable() {
        ReflectionTestUtils.setField(scheduler, "retencionDias", 30);
        when(auditoriaRepository.borrarAnterioresA(any())).thenReturn(5);

        scheduler.depurar();

        org.mockito.ArgumentCaptor<LocalDateTime> corteCaptor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(auditoriaRepository).borrarAnterioresA(corteCaptor.capture());
        long dias = ChronoUnit.DAYS.between(corteCaptor.getValue(), LocalDateTime.now());
        assertTrue(dias >= 29 && dias <= 31);
    }
}
