package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.MiEstudianteTutoradoDTO;
import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.DocenteRepository;
import ec.edu.uteq.presustentaciones.repositories.EstadoSolicitudRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import ec.edu.uteq.presustentaciones.repositories.TutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TutorServiceImpl no tenia ninguna prueba (2.67% lineas, 0% ramas antes de este archivo).
 */
@ExtendWith(MockitoExtension.class)
class TutorServiceImplTest {

    @Mock private TutorRepository tutorRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private EstadoSolicitudRepository estadoSolicitudRepository;

    @InjectMocks
    private TutorServiceImpl tutorService;

    private Usuario usuarioEstudiante;
    private Usuario usuarioDocente;
    private Estudiante estudiante;
    private Docente docente;
    private Solicitud solicitud;

    @BeforeEach
    void setUp() {
        usuarioEstudiante = Usuario.builder().id(1L).nombre("Ana").apellido("Torres").build();
        usuarioDocente = Usuario.builder().id(2L).nombre("Carlos").apellido("Ruiz").build();
        estudiante = Estudiante.builder().id(1L).usuario(usuarioEstudiante).build();
        docente = Docente.builder().id(1L).usuario(usuarioDocente).build();
        solicitud = Solicitud.builder().id(10L).tituloTema("Sistema X").estudiante(estudiante).build();
    }

    // ---- asignarTutor ----

    @Test
    void asignarTutorLanzaSiSolicitudNoExiste() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> tutorService.asignarTutor(10L, 1L));
        assertTrue(ex.getMessage().contains("Solicitud no encontrada"));
        verifyNoInteractions(docenteRepository);
    }

    @Test
    void asignarTutorLanzaSiDocenteNoExiste() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(1L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> tutorService.asignarTutor(10L, 1L));
        assertTrue(ex.getMessage().contains("Docente no encontrado"));
    }

    @Test
    void asignarTutorSinTutorPrevioNoEliminaNada() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.empty());
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estadoSolicitudRepository.findByCodigo("TUTORIA"))
                .thenReturn(Optional.of(EstadoSolicitud.builder().codigo("TUTORIA").nombre("Tutoria").build()));

        Tutor resultado = tutorService.asignarTutor(10L, 1L);

        assertEquals("ACTIVO", resultado.getEstado());
        verify(tutorRepository, never()).delete(any());
        verify(solicitudRepository).save(solicitud);
        assertEquals("TUTORIA", solicitud.getEstado().getCodigo());
    }

    @Test
    void asignarTutorReemplazaTutorPrevio() {
        Tutor tutorPrevio = Tutor.builder().id(5L).estado("ACTIVO").build();
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.of(tutorPrevio));
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estadoSolicitudRepository.findByCodigo("TUTORIA"))
                .thenReturn(Optional.of(EstadoSolicitud.builder().codigo("TUTORIA").build()));

        tutorService.asignarTutor(10L, 1L);

        verify(tutorRepository).delete(tutorPrevio);
    }

    @Test
    void asignarTutorCreaEstadoTutoriaSiNoExisteEnCatalogo() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.empty());
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estadoSolicitudRepository.findByCodigo("TUTORIA")).thenReturn(Optional.empty());
        when(estadoSolicitudRepository.save(any(EstadoSolicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        tutorService.asignarTutor(10L, 1L);

        verify(estadoSolicitudRepository).save(argThat(e -> "TUTORIA".equals(e.getCodigo())));
    }

    @Test
    void asignarTutorNoPropagaFalloDeNotificacion() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(docente));
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.empty());
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estadoSolicitudRepository.findByCodigo("TUTORIA"))
                .thenReturn(Optional.of(EstadoSolicitud.builder().codigo("TUTORIA").build()));
        doThrow(new RuntimeException("fallo notificacion")).when(notificacionService)
                .crearNotificacion(anyLong(), anyString());

        Tutor resultado = assertDoesNotThrow(() -> tutorService.asignarTutor(10L, 1L));

        assertNotNull(resultado);
        verify(notificacionService, times(2)).crearNotificacion(anyLong(), anyString());
    }

    // ---- delegados simples ----

    @Test
    void buscarPorSolicitudDelega() {
        Tutor tutor = Tutor.builder().id(1L).build();
        when(tutorRepository.findBySolicitudId(10L)).thenReturn(Optional.of(tutor));
        assertEquals(Optional.of(tutor), tutorService.buscarPorSolicitud(10L));
    }

    @Test
    void listarTodosDelega() {
        Pageable pageable = mock(Pageable.class);
        Page<Tutor> pagina = new PageImpl<>(List.of());
        when(tutorRepository.findAll(pageable)).thenReturn(pagina);
        assertSame(pagina, tutorService.listarTodos(pageable));
    }

    @Test
    void eliminarTutorDelega() {
        tutorService.eliminarTutor(5L);
        verify(tutorRepository).deleteById(5L);
    }

    // ---- misEstudiantes ----

    @Test
    void misEstudiantesMapeaConCarreraEntidadYEstadoAcademico() {
        Carrera carrera = Carrera.builder().id(1).nombre("Software").build();
        EstadoAcademico ea = EstadoAcademico.builder().codigo("ACTIVO").nombre("Activo").build();
        Estudiante est = Estudiante.builder().id(1L).usuario(usuarioEstudiante).telefono("099")
                .expedienteCodigo("EXP-1").carreraEntidad(carrera).semestreActual((short) 3)
                .estadoAcademico(ea).build();
        Solicitud sol = Solicitud.builder().id(10L).tituloTema("Tema").estudiante(est)
                .estado(EstadoSolicitud.builder().codigo("TUTORIA").nombre("Tutoria").build()).build();
        Tutor tutor = Tutor.builder().id(7L).solicitud(sol).docente(docente).estado("ACTIVO").build();
        when(tutorRepository.findByDocenteUsuarioId(2L)).thenReturn(List.of(tutor));

        List<MiEstudianteTutoradoDTO> resultado = tutorService.misEstudiantes(2L);

        assertEquals(1, resultado.size());
        MiEstudianteTutoradoDTO dto = resultado.get(0);
        assertEquals("Software", dto.getCarreraNombre());
        assertEquals("ACTIVO", dto.getEstadoAcademicoCodigo());
        assertEquals("TUTORIA", dto.getEstadoSolicitudCodigo());
    }

    @Test
    void misEstudiantesUsaFallbacksSinCarreraEntidadNiEstadoNiEstadoSolicitud() {
        Estudiante est = Estudiante.builder().id(1L).usuario(usuarioEstudiante).carrera("Carrera Legado").build();
        Solicitud sol = Solicitud.builder().id(10L).tituloTema("Tema").estudiante(est).estadoCodigo("BORRADOR").build();
        Tutor tutor = Tutor.builder().id(7L).solicitud(sol).docente(docente).estado("ACTIVO").build();
        when(tutorRepository.findByDocenteUsuarioId(2L)).thenReturn(List.of(tutor));

        MiEstudianteTutoradoDTO dto = tutorService.misEstudiantes(2L).get(0);

        assertEquals("Carrera Legado", dto.getCarreraNombre());
        assertNull(dto.getEstadoAcademicoCodigo());
        assertEquals("BORRADOR", dto.getEstadoSolicitudCodigo());
        assertNull(dto.getEstadoSolicitudNombre());
    }

    // ---- obtenerEstadisticasTutoresSP ----

    @Test
    void obtenerEstadisticasTutoresSPMapeaCadaFila() {
        Object[] fila = {1L, "Carlos Ruiz", 3, 5, 12};
        when(tutorRepository.obtenerEstadisticasTutoresSp()).thenReturn(List.<Object[]>of(fila));

        List<java.util.Map<String, Object>> resultado = tutorService.obtenerEstadisticasTutoresSP();

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).get("tutorDocenteId"));
        assertEquals("Carlos Ruiz", resultado.get(0).get("tutorNombre"));
        assertEquals(3, resultado.get(0).get("tutoriasActivas"));
        assertEquals(5, resultado.get(0).get("tutoriasCompletadas"));
        assertEquals(12, resultado.get(0).get("totalFasesAprobadas"));
    }

    @Test
    void obtenerEstadisticasTutoresSPDevuelveVacioSinFilas() {
        when(tutorRepository.obtenerEstadisticasTutoresSp()).thenReturn(List.of());
        assertTrue(tutorService.obtenerEstadisticasTutoresSP().isEmpty());
    }
}
