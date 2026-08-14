package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.TutoriaFaseDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaMensajeDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaResumenDTO;
import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TutoriaServiceImplTest {

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private TutoriaFaseRepository tutoriaFaseRepository;

    @Mock
    private TutoriaMensajeRepository tutoriaMensajeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AnteproyectoRepository anteproyectoRepository;

    @InjectMocks
    private TutoriaServiceImpl tutoriaService;

    @TempDir
    Path tempDir;

    private Tutor tutor;
    private Solicitud solicitud;
    private Estudiante estudiante;
    private Docente docente;
    private Usuario usuarioDocente;
    private Usuario usuarioEstudiante;
    private TutoriaFase fase1;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tutoriaService, "uploadDir", tempDir.resolve("tutorias").toString());
        ReflectionTestUtils.setField(tutoriaService, "uploadDirAnteproyectos", tempDir.resolve("anteproyectos").toString());

        usuarioDocente = Usuario.builder().id(10L).nombre("Profesor").apellido("Docente").rol("DOCENTE").email("pdocente@uteq.edu.ec").build();
        docente = Docente.builder().id(1L).usuario(usuarioDocente).disponible(true).build();

        usuarioEstudiante = Usuario.builder().id(20L).nombre("Alumno").apellido("Estudiante").rol("ESTUDIANTE").email("aestudiante@uteq.edu.ec").build();
        estudiante = Estudiante.builder().id(2L).usuario(usuarioEstudiante).build();

        solicitud = Solicitud.builder().id(100L).estudiante(estudiante).tituloTema("Sistema Web").build();
        tutor = Tutor.builder().id(1L).solicitud(solicitud).docente(docente).estado("EN_PROCESO").build();

        fase1 = TutoriaFase.builder()
                .id(1L)
                .tutor(tutor)
                .numeroFase(1)
                .estado("PENDIENTE_ESTUDIANTE")
                .build();
    }

    @Test
    void testObtenerResumen() {
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(tutoriaFaseRepository.findByTutorIdOrderByNumeroFaseAsc(1L)).thenReturn(List.of(fase1));

        TutoriaResumenDTO resumen = tutoriaService.obtenerResumen(1L, 10L);

        assertNotNull(resumen);
        assertEquals(1L, resumen.getTutorId());
        assertEquals("EN_PROCESO", resumen.getEstadoTutoria());
        assertEquals("Sistema Web", resumen.getTituloTema());
    }

    @Test
    void testCrearFaseConObservacionExitoso() {
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(tutoriaFaseRepository.countByTutorId(1L)).thenReturn(0L);
        when(tutoriaFaseRepository.save(any(TutoriaFase.class))).thenAnswer(inv -> {
            TutoriaFase f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioDocente));

        TutoriaFaseDTO faseDTO = tutoriaService.crearFaseConObservacion(1L, 10L, "Favor corregir la introducción.");

        assertNotNull(faseDTO);
        assertEquals(1, faseDTO.getNumeroFase());
        verify(tutoriaMensajeRepository).save(any(TutoriaMensaje.class));
    }

    @Test
    void testCrearFaseConObservacionFallaSiUsuarioNoEsElTutor() {
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                tutoriaService.crearFaseConObservacion(1L, 999L, "Observación"));
        assertEquals("No autorizado", ex.getMessage());
    }

    @Test
    void testCrearFaseConObservacionFallaSiExcedeTresFases() {
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(tutoriaFaseRepository.countByTutorId(1L)).thenReturn(3L);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                tutoriaService.crearFaseConObservacion(1L, 10L, "Observación"));
        assertTrue(ex.getMessage().contains("No se pueden crear más de 3 fases"));
    }

    @Test
    void testSubirPdfCorregidoExitoso() {
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        when(tutoriaFaseRepository.save(any(TutoriaFase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioEstudiante));

        MockMultipartFile archivoPdf = new MockMultipartFile(
                "archivo", "documento.pdf", "application/pdf", "%PDF-1.4 demo content".getBytes());

        TutoriaFaseDTO resultado = tutoriaService.subirPdfCorregido(1L, archivoPdf, 20L);

        assertNotNull(resultado);
        assertEquals("PENDIENTE_TUTOR", resultado.getEstado());
        verify(tutoriaMensajeRepository).save(any(TutoriaMensaje.class));
    }

    @Test
    void testSubirPdfFallaSiSolicitudEstaSuspendida() {
        EstadoSolicitud estadoSusp = EstadoSolicitud.builder().codigo("SUSPENDIDA").nombre("Suspendida").build();
        solicitud.setEstado(estadoSusp);
        solicitud.setMotivoSuspension("Plagio detectado");

        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));

        MockMultipartFile archivoPdf = new MockMultipartFile(
                "archivo", "documento.pdf", "application/pdf", "%PDF-1.4 demo".getBytes());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                tutoriaService.subirPdfCorregido(1L, archivoPdf, 20L));
        assertTrue(ex.getMessage().contains("suspendido"));
    }

    @Test
    void testAprobarFaseExitoso() {
        fase1.setEstado("PENDIENTE_TUTOR");
        fase1.setArchivoPdfEstudiante("archivo_fase1.pdf");

        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        when(tutoriaFaseRepository.save(any(TutoriaFase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioDocente));
        when(tutoriaFaseRepository.countByTutorId(1L)).thenReturn(1L);
        when(tutoriaFaseRepository.countByTutorIdAndEstado(1L, "APROBADA")).thenReturn(1L);

        TutoriaFaseDTO resultado = tutoriaService.aprobarFase(1L, 10L, "Excelente trabajo");

        assertNotNull(resultado);
        assertEquals("APROBADA", resultado.getEstado());
        verify(tutoriaMensajeRepository).save(any(TutoriaMensaje.class));
    }

    @Test
    void testEnviarMensajeExitosoPorTutorYEstudiante() {
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioDocente));
        when(tutoriaMensajeRepository.save(any(TutoriaMensaje.class))).thenAnswer(inv -> inv.getArgument(0));

        TutoriaMensajeDTO dto = tutoriaService.enviarMensaje(1L, 10L, "Mensaje de prueba", "OBSERVACION");
        assertNotNull(dto);
        assertEquals("Mensaje de prueba", dto.getContenido());
    }

    @Test
    void testEnviarMensajeRechazaUsuarioNoAutorizado() {
        Usuario ajeno = Usuario.builder().id(999L).rol("ESTUDIANTE").build();
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        when(usuarioRepository.findById(999L)).thenReturn(Optional.of(ajeno));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                tutoriaService.enviarMensaje(1L, 999L, "Mensaje sospechoso", "OBSERVACION"));
        assertTrue(ex.getMessage().contains("No autorizado"));
    }
}
