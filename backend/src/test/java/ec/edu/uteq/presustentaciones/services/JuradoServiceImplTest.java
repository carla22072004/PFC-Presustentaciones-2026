package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JuradoServiceImplTest {

    @Mock
    private JuradoRepository juradoRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private EmailService emailService;

    @Mock
    private RolJuradoRepository rolJuradoRepository;

    @Mock
    private EstadoSolicitudRepository estadoSolicitudRepository;

    @InjectMocks
    private JuradoServiceImpl juradoService;

    private Solicitud solicitud;
    private Estudiante estudiante;
    private Usuario usuarioEstudiante;
    private Docente docente1;
    private Docente docente2;
    private Usuario usuarioDocente1;
    private Usuario usuarioDocente2;
    private Tutor tutorCompletado;

    @BeforeEach
    void setUp() {
        usuarioDocente1 = Usuario.builder().id(101L).nombre("Ana").apellido("Gomez").email("agomez@uteq.edu.ec").build();
        docente1 = Docente.builder().id(1L).usuario(usuarioDocente1).disponible(true).cargaHorariaSemanal(0).build();

        usuarioDocente2 = Usuario.builder().id(102L).nombre("Luis").apellido("Vera").email("lvera@uteq.edu.ec").build();
        docente2 = Docente.builder().id(2L).usuario(usuarioDocente2).disponible(true).cargaHorariaSemanal(0).build();

        usuarioEstudiante = Usuario.builder().id(201L).nombre("Mario").apellido("Alvarado").email("malvarado@uteq.edu.ec").build();
        estudiante = Estudiante.builder().id(5L).usuario(usuarioEstudiante).build();

        solicitud = Solicitud.builder().id(50L).estudiante(estudiante).tituloTema("Tesis Inteligencia Artificial").build();
        tutorCompletado = Tutor.builder().id(1L).solicitud(solicitud).docente(docente1).estado("COMPLETADA").build();

        lenient().when(rolJuradoRepository.findByCodigo(anyString()))
                .thenAnswer(inv -> Optional.of(RolJurado.builder().codigo(inv.getArgument(0)).build()));
    }

    @Test
    void testAsignarJuradoExitoso() {
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(2L)).thenReturn(Optional.of(docente2));
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(new ArrayList<>());
        when(juradoRepository.save(any(Jurado.class))).thenAnswer(inv -> {
            Jurado j = inv.getArgument(0);
            j.setId(1L);
            return j;
        });

        Jurado jurado = juradoService.asignarJurado(50L, 2L, "PRESIDENTE");

        assertNotNull(jurado);
        assertEquals("PRESIDENTE", jurado.getRol());
        assertEquals(docente2, jurado.getDocente());
        verify(juradoRepository).save(any(Jurado.class));
    }

    @Test
    void testAsignarJuradoFallaSiTutoriaNoEstaCompletada() {
        tutorCompletado.setEstado("EN_PROCESO");
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(2L)).thenReturn(Optional.of(docente2));
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                juradoService.asignarJurado(50L, 2L, "PRESIDENTE"));
        assertTrue(ex.getMessage().contains("la tutoría aún no ha completado las 3 revisiones"));
    }

    @Test
    void testAsignarJuradoFallaSiDocenteYaEstaAsignado() {
        Jurado existente = Jurado.builder().id(10L).solicitud(solicitud).docente(docente2)
                .rolJurado(RolJurado.builder().codigo("VOCAL").build()).build();
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(2L)).thenReturn(Optional.of(docente2));
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(List.of(existente));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                juradoService.asignarJurado(50L, 2L, "PRESIDENTE"));
        assertTrue(ex.getMessage().contains("ya está asignado como jurado"));
    }

    @Test
    void testEliminarJurado() {
        Jurado existente = Jurado.builder().id(10L).solicitud(solicitud).docente(docente2).build();
        when(juradoRepository.findById(10L)).thenReturn(Optional.of(existente));

        juradoService.eliminarJurado(10L);

        verify(juradoRepository).deleteById(10L);
    }

    @Test
    void testAsignarTutorExitoso() {
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente1));
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.empty());
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> {
            Tutor t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutorCompletado));

        Tutor tutor = juradoService.asignarTutor(50L, 1L);

        assertNotNull(tutor);
        verify(tutorRepository).save(any(Tutor.class));
    }

    // sp_asignar_jurado_masivo (Fase 3 / Criterio P1) -- sin test dedicado pese a ser el
    // unico punto del codigo que invoca ese procedimiento.
    @Test
    void testAsignarJuradoMasivoRechazaArreglosDeLongitudDistinta() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                juradoService.asignarJuradoMasivo(List.of(50L, 51L), List.of(1L), "PRESIDENTE"));

        assertTrue(ex.getMessage().contains("misma longitud"));
        verify(juradoRepository, never()).spAsignarJuradoMasivo(anyLong(), anyLong(), anyString());
    }

    @Test
    void testAsignarJuradoMasivoInvocaElProcedimientoUnaVezPorPar() {
        juradoService.asignarJuradoMasivo(List.of(50L, 51L), List.of(1L, 2L), "VOCAL_1");

        verify(juradoRepository).spAsignarJuradoMasivo(50L, 1L, "VOCAL_1");
        verify(juradoRepository).spAsignarJuradoMasivo(51L, 2L, "VOCAL_1");
        verify(juradoRepository, times(2)).spAsignarJuradoMasivo(anyLong(), anyLong(), anyString());
    }

    @Test
    void testAsignarJuradoMasivoSiUnParFallaNoSigueConLosSiguientes() {
        // Simula el rollback transaccional real: si el SP lanza excepcion en el segundo par,
        // el metodo debe propagarla (Spring revierte la transaccion @Transactional completa).
        // Mockito en modo estricto (default) exige stubear tambien la primera llamada:
        // sin esto, la interpreta como un posible error del test en vez de "sin comportamiento
        // especial" y lanza su propia excepcion de "stubbing argument mismatch" en su lugar.
        doNothing().when(juradoRepository).spAsignarJuradoMasivo(50L, 1L, "VOCAL_2");
        doThrow(new RuntimeException("El docente ya tiene otra defensa en ese horario"))
                .when(juradoRepository).spAsignarJuradoMasivo(51L, 2L, "VOCAL_2");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                juradoService.asignarJuradoMasivo(List.of(50L, 51L), List.of(1L, 2L), "VOCAL_2"));
        assertEquals("El docente ya tiene otra defensa en ese horario", ex.getMessage());

        verify(juradoRepository).spAsignarJuradoMasivo(50L, 1L, "VOCAL_2");
        verify(juradoRepository).spAsignarJuradoMasivo(51L, 2L, "VOCAL_2");
    }

    // ── asignarJurado: validaciones restantes ───────────────────────────────

    @Test
    void asignarJuradoLanzaSiSolicitudNoExiste() {
        when(solicitudRepository.findById(50L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> juradoService.asignarJurado(50L, 2L, "PRESIDENTE"));
        assertTrue(ex.getMessage().contains("Solicitud no encontrada"));
    }

    @Test
    void asignarJuradoLanzaSiDocenteNoExiste() {
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(2L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> juradoService.asignarJurado(50L, 2L, "PRESIDENTE"));
        assertTrue(ex.getMessage().contains("Docente no encontrado"));
    }

    @Test
    void asignarJuradoLanzaSiNoHayTutorAsignado() {
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(2L)).thenReturn(Optional.of(docente2));
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> juradoService.asignarJurado(50L, 2L, "PRESIDENTE"));
        assertTrue(ex.getMessage().contains("no tiene tutor asignado"));
    }

    @Test
    void asignarJuradoLanzaSiElDocenteEsElTutorDeLaSolicitud() {
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente1));
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado)); // tutor = docente1
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(new ArrayList<>());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> juradoService.asignarJurado(50L, 1L, "PRESIDENTE"));
        assertTrue(ex.getMessage().contains("conflicto de interés"));
    }

    @Test
    void asignarJuradoLanzaSiDocenteNoDisponible() {
        docente2.setDisponible(false);
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(2L)).thenReturn(Optional.of(docente2));
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(new ArrayList<>());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> juradoService.asignarJurado(50L, 2L, "PRESIDENTE"));
        assertTrue(ex.getMessage().contains("no está disponible"));
    }

    @Test
    void asignarJuradoLanzaSiRolEsNuloOInvalido() {
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(2L)).thenReturn(Optional.of(docente2));
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(new ArrayList<>());

        RuntimeException ex1 = assertThrows(RuntimeException.class,
                () -> juradoService.asignarJurado(50L, 2L, null));
        assertTrue(ex1.getMessage().contains("Rol inválido"));

        RuntimeException ex2 = assertThrows(RuntimeException.class,
                () -> juradoService.asignarJurado(50L, 2L, "SECRETARIO"));
        assertTrue(ex2.getMessage().contains("Rol inválido"));
    }

    @Test
    void asignarJuradoLanzaSiRolYaOcupado() {
        Jurado presidenteExistente = Jurado.builder().id(9L).solicitud(solicitud).docente(docente1)
                .rolJurado(RolJurado.builder().codigo("PRESIDENTE").build()).build();
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(2L)).thenReturn(Optional.of(docente2));
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(List.of(presidenteExistente));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> juradoService.asignarJurado(50L, 2L, "presidente"));
        assertTrue(ex.getMessage().contains("ya está asignado en esta solicitud"));
    }

    @Test
    void asignarJuradoCreaRolJuradoNuevoSiNoEstaEnElCatalogo() {
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(2L)).thenReturn(Optional.of(docente2));
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(new ArrayList<>());
        when(rolJuradoRepository.findByCodigo("VOCAL_1")).thenReturn(Optional.empty());
        when(rolJuradoRepository.save(any(RolJurado.class))).thenAnswer(inv -> inv.getArgument(0));
        when(juradoRepository.save(any(Jurado.class))).thenAnswer(inv -> {
            Jurado j = inv.getArgument(0);
            j.setId(1L);
            return j;
        });

        Jurado jurado = juradoService.asignarJurado(50L, 2L, "vocal_1");

        assertEquals("VOCAL_1", jurado.getRolJurado().getCodigo());
        verify(rolJuradoRepository).save(argThat(r -> "VOCAL_1".equals(r.getCodigo()) && "Vocal_1".equals(r.getNombre())));
    }

    // ── obtenerTutorDeSolicitud / eliminarTutor / delegados simples ─────────

    @Test
    void obtenerTutorDeSolicitudFiltraSoloActivos() {
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado)); // estado COMPLETADA
        assertTrue(juradoService.obtenerTutorDeSolicitud(50L).isEmpty());
    }

    @Test
    void obtenerTutorDeSolicitudDevuelveElActivo() {
        Tutor activo = Tutor.builder().id(2L).estado("ACTIVO").build();
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(activo));
        assertEquals(Optional.of(activo), juradoService.obtenerTutorDeSolicitud(50L));
    }

    @Test
    void eliminarTutorDelega() {
        juradoService.eliminarTutor(3L);
        verify(tutorRepository).deleteById(3L);
    }

    @Test
    void listarPorSolicitudDelega() {
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(List.of());
        assertTrue(juradoService.listarPorSolicitud(50L).isEmpty());
    }

    @Test
    void listarPorDocenteDelega() {
        when(juradoRepository.findByDocenteId(1L)).thenReturn(List.of());
        assertTrue(juradoService.listarPorDocente(1L).isEmpty());
    }

    @Test
    void listarTutoriasPorDocenteDelega() {
        when(tutorRepository.findByDocenteId(1L)).thenReturn(List.of());
        assertTrue(juradoService.listarTutoriasPorDocente(1L).isEmpty());
    }

    @Test
    void obtenerInfoJuradoDelega() {
        when(juradoRepository.findBySolicitudIdAndUsuarioId(50L, 101L)).thenReturn(Optional.empty());
        assertTrue(juradoService.obtenerInfoJurado(50L, 101L).isEmpty());
    }

    @Test
    void asignarJuradoMasivoSPDelega() {
        Long[] solicitudes = {50L, 51L};
        Long[] docentes = {1L, 2L};
        juradoService.asignarJuradoMasivoSP(solicitudes, docentes, "PRESIDENTE");
        verify(juradoRepository).spAsignarJuradoMasivo(solicitudes, docentes, "PRESIDENTE");
    }

    // ── sugerirDocentes ──────────────────────────────────────────────────────

    @Test
    void sugerirDocentesExcluyeJuradosYTutorYaAsignados() {
        Jurado juradoExistente = Jurado.builder().id(1L).docente(docente1).build();
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(List.of(juradoExistente));
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado)); // docente1 tambien
        when(docenteRepository.findDisponiblesOrdenadosPorCarga()).thenReturn(List.of(docente1, docente2));

        // cantidad=1: con docente1 excluido queda exactamente 1 candidato, sin activar el
        // fallback a findTodosOrdenadosPorCarga() (ese camino se prueba aparte).
        List<Docente> sugeridos = juradoService.sugerirDocentes(50L, 1);

        assertEquals(1, sugeridos.size());
        assertEquals(docente2, sugeridos.get(0));
    }

    @Test
    void sugerirDocentesUsaPoolCompletoSiNoHaySuficientesDisponibles() {
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(List.of());
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.empty());
        when(docenteRepository.findDisponiblesOrdenadosPorCarga()).thenReturn(List.of(docente1));
        when(docenteRepository.findTodosOrdenadosPorCarga()).thenReturn(List.of(docente1, docente2));

        List<Docente> sugeridos = juradoService.sugerirDocentes(50L, 2);

        assertEquals(2, sugeridos.size());
        verify(docenteRepository).findTodosOrdenadosPorCarga();
    }

    // ── asignarJuradosAutomaticamente ────────────────────────────────────────

    @Test
    void asignarJuradosAutomaticamenteLanzaSiNoHayTutor() {
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> juradoService.asignarJuradosAutomaticamente(50L));
        assertTrue(ex.getMessage().contains("no tiene tutor asignado"));
    }

    @Test
    void asignarJuradosAutomaticamenteLanzaSiTutoriaNoCompletada() {
        tutorCompletado.setEstado("EN_PROCESO");
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));
        assertThrows(RuntimeException.class, () -> juradoService.asignarJuradosAutomaticamente(50L));
    }

    @Test
    void asignarJuradosAutomaticamenteNoHaceNadaSiTribunalYaCompleto() {
        // Solicitud SI se consulta antes de revisar los roles (orden real del metodo);
        // el early-return ocurre despues, al ver que rolesFaltantes esta vacio.
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        Jurado p = Jurado.builder().id(1L).rolJurado(RolJurado.builder().codigo("PRESIDENTE").build()).build();
        Jurado v1 = Jurado.builder().id(2L).rolJurado(RolJurado.builder().codigo("VOCAL_1").build()).build();
        Jurado v2 = Jurado.builder().id(3L).rolJurado(RolJurado.builder().codigo("VOCAL_2").build()).build();
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(List.of(p, v1, v2));

        juradoService.asignarJuradosAutomaticamente(50L);

        verify(juradoRepository, never()).save(any());
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void asignarJuradosAutomaticamenteLanzaSiNoHaySuficientesDocentes() {
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(List.of()); // faltan los 3 roles
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findDisponiblesOrdenadosPorCarga()).thenReturn(List.of(docente1));
        when(docenteRepository.findTodosOrdenadosPorCarga()).thenReturn(List.of(docente1));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> juradoService.asignarJuradosAutomaticamente(50L));
        assertTrue(ex.getMessage().contains("No hay suficientes docentes"));
    }

    @Test
    void asignarJuradosAutomaticamenteAsignaLosTresRolesYNotificaUnaVez() {
        Docente docente3 = Docente.builder().id(3L).usuario(Usuario.builder().id(103L).nombre("Rosa").apellido("Diaz").build())
                .disponible(true).cargaHorariaSemanal(0).build();
        // docente1 es el tutor de la solicitud -- sugerirDocentes lo excluye tambien via
        // tutorRepository, asi que hacen falta 3 disponibles ADEMAS de el para cubrir los 3 roles.
        Docente docente4 = Docente.builder().id(4L).usuario(Usuario.builder().id(104L).nombre("Ivan").apellido("Solis").build())
                .disponible(true).cargaHorariaSemanal(0).build();
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findDisponiblesOrdenadosPorCarga()).thenReturn(List.of(docente1, docente2, docente3, docente4));
        when(juradoRepository.save(any(Jurado.class))).thenAnswer(inv -> {
            Jurado j = inv.getArgument(0);
            j.setId((long) (Math.random() * 1000));
            return j;
        });
        // 3 llamadas a findBySolicitudId: 1ra (roles ocupados, al inicio), 2da (idsOcupados
        // dentro de sugerirDocentes), 3ra (armar el tribunal ya completo, al final para notificar).
        Jurado pFinal = Jurado.builder().id(1L).docente(docente1).rolJurado(RolJurado.builder().codigo("PRESIDENTE").build()).build();
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(List.of(), List.of(), List.of(pFinal));

        juradoService.asignarJuradosAutomaticamente(50L);

        verify(juradoRepository, times(3)).save(any(Jurado.class));
        verify(notificacionService, atLeastOnce()).crearNotificacion(anyLong(), anyString());
        verify(emailService).enviarNotificacion(anyString(), anyString());
    }

    @Test
    void notificarEstudianteTribunalCompletoNoPropagaExcepcionSiFallaLaNotificacion() {
        Docente docente3 = Docente.builder().id(3L).usuario(Usuario.builder().id(103L).nombre("Rosa").apellido("Diaz").build())
                .disponible(true).cargaHorariaSemanal(0).build();
        // docente1 es el tutor de la solicitud -- sugerirDocentes lo excluye tambien via
        // tutorRepository, asi que hacen falta 3 disponibles ADEMAS de el para cubrir los 3 roles.
        Docente docente4 = Docente.builder().id(4L).usuario(Usuario.builder().id(104L).nombre("Ivan").apellido("Solis").build())
                .disponible(true).cargaHorariaSemanal(0).build();
        when(tutorRepository.findBySolicitudId(50L)).thenReturn(Optional.of(tutorCompletado));
        when(solicitudRepository.findById(50L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findDisponiblesOrdenadosPorCarga()).thenReturn(List.of(docente1, docente2, docente3, docente4));
        when(juradoRepository.save(any(Jurado.class))).thenAnswer(inv -> inv.getArgument(0));
        Jurado pFinal = Jurado.builder().id(1L).docente(docente1).rolJurado(RolJurado.builder().codigo("PRESIDENTE").build()).build();
        when(juradoRepository.findBySolicitudId(50L)).thenReturn(List.of(), List.of(), List.of(pFinal));
        doThrow(new RuntimeException("fallo notificacion")).when(notificacionService)
                .crearNotificacion(eq(201L), anyString());

        assertDoesNotThrow(() -> juradoService.asignarJuradosAutomaticamente(50L));
    }
}
