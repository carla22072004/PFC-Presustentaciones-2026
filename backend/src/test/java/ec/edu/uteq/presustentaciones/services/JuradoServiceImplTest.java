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
}
