package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Cubre validarPrerequisitosParaCronograma (tribunal completo + tutoría COMPLETADA) y la
 * validación cruzada sp_validar_conflicto_jurado (Fase 3 / Criterio P1) conectada en
 * crearCronograma -- ninguno de los dos tenía prueba dedicada (CronogramaServiceImplTest
 * no existía, ver docs/trazabilidad/matriz.csv RF-04).
 */
@ExtendWith(MockitoExtension.class)
class CronogramaServiceImplTest {

    @Mock private CronogramaRepository cronogramaRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private SalaRepository salaRepository;
    @Mock private JuradoRepository juradoRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private EstadoCronogramaRepository estadoCronogramaRepository;

    @InjectMocks
    private CronogramaServiceImpl cronogramaService;

    private Solicitud solicitud;
    private Sala sala;
    private Docente docente;
    private Jurado presidente;
    private Jurado vocal;
    private Jurado secretario;
    private Tutor tutorCompletado;

    @BeforeEach
    void setUp() {
        solicitud = Solicitud.builder().id(10L).tituloTema("Sistema X").build();
        sala = Sala.builder().id(1L).nombre("Aula 1").build();
        Usuario usuarioDocente = Usuario.builder().id(50L).nombre("Ana").apellido("Torres").build();
        docente = Docente.builder().id(1L).usuario(usuarioDocente).build();

        presidente = Jurado.builder().id(1L).solicitud(solicitud).docente(docente)
                .rolJurado(RolJurado.builder().codigo("PRESIDENTE").build()).build();
        // El tribunal real solo tiene 3 roles: PRESIDENTE, VOCAL_1, VOCAL_2 (sin secretario) --
        // ver JuradoServiceImpl.rolesValidos y CronogramaServiceImpl.validarPrerequisitosParaCronograma.
        vocal = Jurado.builder().id(2L).solicitud(solicitud).docente(docente)
                .rolJurado(RolJurado.builder().codigo("VOCAL_1").build()).build();
        secretario = Jurado.builder().id(3L).solicitud(solicitud).docente(docente)
                .rolJurado(RolJurado.builder().codigo("VOCAL_2").build()).build();

        tutorCompletado = Tutor.builder().id(1L).solicitud(solicitud).estado("COMPLETADA").build();
    }

    @Test
    void testCrearCronogramaFallaSiTribunalIncompleto() {
        when(juradoRepository.findBySolicitudId(10L)).thenReturn(List.of(presidente, vocal));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                cronogramaService.crearCronograma(10L, 1L, LocalDate.now().plusDays(5), LocalTime.of(9, 0)));
        assertTrue(ex.getMessage().contains("tribunal no está completo"));
        verify(cronogramaRepository, never()).save(any());
    }

    @Test
    void testCrearCronogramaFallaSiTutoriaNoCompletada() {
        when(juradoRepository.findBySolicitudId(10L)).thenReturn(List.of(presidente, vocal, secretario));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.of(Tutor.builder().estado("EN_PROCESO").build()));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                cronogramaService.crearCronograma(10L, 1L, LocalDate.now().plusDays(5), LocalTime.of(9, 0)));
        assertTrue(ex.getMessage().contains("tutoría no ha sido completada"));
    }

    @Test
    void testCrearCronogramaFallaPorConflictoDeJurado() {
        when(juradoRepository.findBySolicitudId(10L)).thenReturn(List.of(presidente, vocal, secretario));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.of(tutorCompletado));
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala));
        when(cronogramaRepository.findConflictos(anyLong(), any(), any())).thenReturn(List.of());
        // sp_validar_conflicto_jurado (Fase 3): el docente ya tiene otra defensa en ese horario
        when(juradoRepository.validarConflictoJurado(anyLong(), eq(1L), any(), anyInt(), isNull()))
                .thenReturn(Boolean.FALSE);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                cronogramaService.crearCronograma(10L, 1L, LocalDate.now().plusDays(5), LocalTime.of(9, 0)));
        assertTrue(ex.getMessage().contains("Conflicto de horario"));
        assertTrue(ex.getMessage().contains("Ana Torres"));
        verify(cronogramaRepository, never()).save(any());
    }

    @Test
    void testCrearCronogramaExitosoSinConflictos() {
        when(juradoRepository.findBySolicitudId(10L)).thenReturn(List.of(presidente, vocal, secretario));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.of(tutorCompletado));
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala));
        when(cronogramaRepository.findConflictos(anyLong(), any(), any())).thenReturn(List.of());
        when(juradoRepository.validarConflictoJurado(anyLong(), anyLong(), any(), anyInt(), isNull()))
                .thenReturn(Boolean.TRUE);
        when(estadoCronogramaRepository.findByCodigo("PROGRAMADO"))
                .thenReturn(Optional.of(EstadoCronograma.builder().codigo("PROGRAMADO").nombre("Programado").build()));
        when(cronogramaRepository.save(any(Cronograma.class))).thenAnswer(inv -> {
            Cronograma c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        Cronograma resultado = cronogramaService.crearCronograma(10L, 1L, LocalDate.now().plusDays(5), LocalTime.of(9, 0));

        assertNotNull(resultado);
        assertEquals("PROGRAMADO", resultado.getEstado().getCodigo());
        verify(juradoRepository, times(3)).validarConflictoJurado(anyLong(), anyLong(), any(), anyInt(), isNull());
    }
}
