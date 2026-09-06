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
        sala = Sala.builder().id(1L).nombre("Aula 1").disponible(true).build();
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

    // ── asignarAutomatico ────────────────────────────────────────────────────

    @Test
    void asignarAutomaticoDevuelveElExistenteSiYaEstaProgramado() {
        when(juradoRepository.findBySolicitudId(10L)).thenReturn(List.of(presidente, vocal, secretario));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.of(tutorCompletado));
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        Cronograma existente = Cronograma.builder().id(5L)
                .estado(EstadoCronograma.builder().codigo("PROGRAMADO").build()).build();
        when(cronogramaRepository.findBySolicitudId(10L)).thenReturn(Optional.of(existente));

        Cronograma resultado = cronogramaService.asignarAutomatico(10L);

        assertSame(existente, resultado);
        verify(salaRepository, never()).findAll();
    }

    @Test
    void asignarAutomaticoLanzaSiNoHaySalasDisponibles() {
        when(juradoRepository.findBySolicitudId(10L)).thenReturn(List.of(presidente, vocal, secretario));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.of(tutorCompletado));
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(cronogramaRepository.findBySolicitudId(10L)).thenReturn(Optional.empty());
        when(salaRepository.findAll()).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> cronogramaService.asignarAutomatico(10L));
        assertTrue(ex.getMessage().contains("No hay salas disponibles"));
    }

    @Test
    void asignarAutomaticoEncuentraLaPrimeraFranjaLibre() {
        when(juradoRepository.findBySolicitudId(10L)).thenReturn(List.of(presidente, vocal, secretario));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.of(tutorCompletado));
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(cronogramaRepository.findBySolicitudId(10L)).thenReturn(Optional.empty());
        when(salaRepository.findAll()).thenReturn(List.of(sala));
        when(cronogramaRepository.findConflictos(anyLong(), any(), any())).thenReturn(List.of());
        when(estadoCronogramaRepository.findByCodigo("PROGRAMADO"))
                .thenReturn(Optional.of(EstadoCronograma.builder().codigo("PROGRAMADO").build()));
        when(cronogramaRepository.save(any(Cronograma.class))).thenAnswer(inv -> {
            Cronograma c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        Cronograma resultado = cronogramaService.asignarAutomatico(10L);

        assertNotNull(resultado);
        assertEquals("PROGRAMADO", resultado.getEstado().getCodigo());
        assertFalse(resultado.getFechaInicio().getDayOfWeek().getValue() >= 6, "no debe caer en fin de semana");
    }

    @Test
    void asignarAutomaticoLanzaSiNoHayDisponibilidadEn30Dias() {
        when(juradoRepository.findBySolicitudId(10L)).thenReturn(List.of(presidente, vocal, secretario));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.of(tutorCompletado));
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(cronogramaRepository.findBySolicitudId(10L)).thenReturn(Optional.empty());
        when(salaRepository.findAll()).thenReturn(List.of(sala));
        // toda franja tiene conflicto -> nunca se libera un slot
        when(cronogramaRepository.findConflictos(anyLong(), any(), any()))
                .thenReturn(List.of(Cronograma.builder().id(1L).build()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> cronogramaService.asignarAutomatico(10L));
        assertTrue(ex.getMessage().contains("No se encontró disponibilidad"));
    }

    @Test
    void asignarAutomaticoValidaPrerequisitosPrimero() {
        when(juradoRepository.findBySolicitudId(10L)).thenReturn(List.of(presidente, vocal)); // tribunal incompleto
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cronogramaService.asignarAutomatico(10L));
        assertTrue(ex.getMessage().contains("tribunal no está completo"));
        verifyNoInteractions(salaRepository);
    }

    // ── estaDisponible / franjasDisponibles ──────────────────────────────────

    @Test
    void estaDisponibleEsFalsoSiHayConflicto() {
        LocalDateTime inicio = LocalDateTime.of(2026, 9, 10, 9, 0);
        when(cronogramaRepository.findConflictos(1L, inicio, inicio.plusMinutes(45)))
                .thenReturn(List.of(Cronograma.builder().id(1L).build()));
        assertFalse(cronogramaService.estaDisponible(1L, inicio, 45));
    }

    @Test
    void estaDisponibleEsVerdaderoSinConflictos() {
        LocalDateTime inicio = LocalDateTime.of(2026, 9, 10, 9, 0);
        when(cronogramaRepository.findConflictos(1L, inicio, inicio.plusMinutes(45))).thenReturn(List.of());
        assertTrue(cronogramaService.estaDisponible(1L, inicio, 45));
    }

    @Test
    void franjasDisponiblesGeneraSlotsDe8a17ConLaDuracionIndicada() {
        List<LocalDateTime> franjas = cronogramaService.franjasDisponibles(LocalDate.of(2026, 9, 10), 45);

        assertFalse(franjas.isEmpty());
        assertEquals(LocalTime.of(8, 0), franjas.get(0).toLocalTime());
        franjas.forEach(f -> assertFalse(f.plusMinutes(45).toLocalTime().isAfter(LocalTime.of(17, 0))));
    }

    // ── delegados simples ────────────────────────────────────────────────────

    @Test
    void listarCronogramasDelega() {
        org.springframework.data.domain.Pageable pageable = mock(org.springframework.data.domain.Pageable.class);
        org.springframework.data.domain.Page<Cronograma> pagina = org.springframework.data.domain.Page.empty();
        when(cronogramaRepository.findAll(pageable)).thenReturn(pagina);
        assertSame(pagina, cronogramaService.listarCronogramas(pageable));
    }

    @Test
    void listarPorEstudianteDelega() {
        when(cronogramaRepository.findByEstudianteId(5L)).thenReturn(List.of());
        assertTrue(cronogramaService.listarPorEstudiante(5L).isEmpty());
    }

    @Test
    void listarPorUsuarioDelega() {
        when(cronogramaRepository.findByUsuarioId(50L)).thenReturn(List.of());
        assertTrue(cronogramaService.listarPorUsuario(50L).isEmpty());
    }

    @Test
    void buscarPorSolicitudDelega() {
        when(cronogramaRepository.findBySolicitudId(10L)).thenReturn(Optional.empty());
        assertTrue(cronogramaService.buscarPorSolicitud(10L).isEmpty());
    }

    @Test
    void eliminarDelega() {
        cronogramaService.eliminar(5L);
        verify(cronogramaRepository).deleteById(5L);
    }

    @Test
    void crearCronogramaNoPropagaFalloDeNotificacion() {
        when(juradoRepository.findBySolicitudId(10L)).thenReturn(List.of(presidente, vocal, secretario));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.of(tutorCompletado));
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(salaRepository.findById(1L)).thenReturn(Optional.of(sala));
        when(cronogramaRepository.findConflictos(anyLong(), any(), any())).thenReturn(List.of());
        when(juradoRepository.validarConflictoJurado(anyLong(), anyLong(), any(), anyInt(), isNull()))
                .thenReturn(Boolean.TRUE);
        when(estadoCronogramaRepository.findByCodigo("PROGRAMADO"))
                .thenReturn(Optional.of(EstadoCronograma.builder().codigo("PROGRAMADO").build()));
        when(cronogramaRepository.save(any(Cronograma.class))).thenAnswer(inv -> inv.getArgument(0));
        // La solicitud del fixture no tiene estudiante asociado: notificarProgramacion() falla
        // con NPE real al intentar leerlo, y esa excepcion debe quedar atrapada sin propagarse
        // (mismo efecto que un fallo real de notificacion, sin necesitar un mock adicional).

        assertDoesNotThrow(() ->
                cronogramaService.crearCronograma(10L, 1L, LocalDate.now().plusDays(5), LocalTime.of(9, 0)));
    }
}
