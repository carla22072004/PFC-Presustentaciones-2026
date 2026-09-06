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
        // Hallazgo real (2026-09-01): validarAccesoATutoria() (control de acceso real agregado
        // por el equipo) busca al usuario por ID -- faltaba este stub, escrito antes del cambio.
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioDocente));
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

    // ── validarAccesoATutoria (via obtenerResumen/obtenerFases) ─────────────

    @Test
    void obtenerResumenLanzaSiUsuarioNoExiste() {
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> tutoriaService.obtenerResumen(1L, 999L));
    }

    @Test
    void obtenerResumenPermiteAccesoAAdminSinSerTutorNiEstudiante() {
        Usuario admin = Usuario.builder().id(500L).rol("ADMIN").build();
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(usuarioRepository.findById(500L)).thenReturn(Optional.of(admin));
        when(tutoriaFaseRepository.findByTutorIdOrderByNumeroFaseAsc(1L)).thenReturn(List.of(fase1));

        assertDoesNotThrow(() -> tutoriaService.obtenerResumen(1L, 500L));
    }

    @Test
    void obtenerResumenRechazaUsuarioAjeno() {
        Usuario ajeno = Usuario.builder().id(999L).rol("ESTUDIANTE").build();
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(usuarioRepository.findById(999L)).thenReturn(Optional.of(ajeno));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> tutoriaService.obtenerResumen(1L, 999L));
    }

    @Test
    void obtenerFasesPermiteAlPropioEstudiante() {
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioEstudiante));
        when(tutoriaFaseRepository.findByTutorIdOrderByNumeroFaseAsc(1L)).thenReturn(List.of(fase1));
        when(tutoriaMensajeRepository.findByFaseIdOrderByFechaEnvioAsc(1L)).thenReturn(List.of());

        List<TutoriaFaseDTO> fases = tutoriaService.obtenerFases(1L, 20L);
        assertEquals(1, fases.size());
    }

    // ── crearFaseConObservacion: rama restante ──────────────────────────────

    @Test
    void crearFaseConObservacionFallaSiLaFaseAnteriorNoEstaAprobada() {
        TutoriaFase faseAnteriorPendiente = TutoriaFase.builder().id(1L).tutor(tutor).numeroFase(1).estado("PENDIENTE_TUTOR").build();
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(tutoriaFaseRepository.countByTutorId(1L)).thenReturn(1L);
        when(tutoriaFaseRepository.findByTutorIdOrderByNumeroFaseAsc(1L)).thenReturn(List.of(faseAnteriorPendiente));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> tutoriaService.crearFaseConObservacion(1L, 10L, "obs"));
        assertTrue(ex.getMessage().contains("Debes aprobar la fase actual"));
    }

    // ── subirPdfCorregido: ramas de validacion ──────────────────────────────

    @Test
    void subirPdfRechazaUsuarioQueNoEsElEstudianteDeLaSolicitud() {
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        MockMultipartFile pdf = new MockMultipartFile("archivo", "d.pdf", "application/pdf", "x".getBytes());

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> tutoriaService.subirPdfCorregido(1L, pdf, 999L));
    }

    @Test
    void subirPdfRechazaEstadoDistintoDePendienteEstudiante() {
        fase1.setEstado("PENDIENTE_TUTOR");
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        MockMultipartFile pdf = new MockMultipartFile("archivo", "d.pdf", "application/pdf", "x".getBytes());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> tutoriaService.subirPdfCorregido(1L, pdf, 20L));
        assertTrue(ex.getMessage().contains("cuando el tutor ha enviado observaciones"));
    }

    @Test
    void subirPdfRechazaContentTypeDistintoDePdf() {
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        MockMultipartFile archivo = new MockMultipartFile("archivo", "d.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "x".getBytes());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> tutoriaService.subirPdfCorregido(1L, archivo, 20L));
        assertTrue(ex.getMessage().contains("Solo se permiten archivos PDF"));
    }

    @Test
    void subirPdfRechazaArchivoMayorA10MB() {
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        byte[] contenidoGrande = new byte[11 * 1024 * 1024];
        MockMultipartFile archivo = new MockMultipartFile("archivo", "grande.pdf", "application/pdf", contenidoGrande);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> tutoriaService.subirPdfCorregido(1L, archivo, 20L));
        assertTrue(ex.getMessage().contains("no puede superar los 10 MB"));
    }

    @Test
    void subirPdfEliminaArchivoAnteriorSiExiste() throws Exception {
        // sube un primer PDF, luego uno de reemplazo -- ejercita la rama de borrado del anterior.
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        when(tutoriaFaseRepository.save(any(TutoriaFase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioEstudiante));
        MockMultipartFile pdf1 = new MockMultipartFile("archivo", "d1.pdf", "application/pdf", "contenido 1".getBytes());
        tutoriaService.subirPdfCorregido(1L, pdf1, 20L);

        fase1.setEstado("PENDIENTE_ESTUDIANTE"); // el tutor volvio a pedir correccion
        MockMultipartFile pdf2 = new MockMultipartFile("archivo", "d2.pdf", "application/pdf", "contenido 2".getBytes());
        TutoriaFaseDTO resultado = tutoriaService.subirPdfCorregido(1L, pdf2, 20L);

        assertNotNull(resultado.getArchivoPdfEstudiante());
    }

    // ── aprobarFase: ramas de validacion y flujo de cierre (3 fases) ────────

    @Test
    void aprobarFaseRechazaUsuarioQueNoEsElTutor() {
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> tutoriaService.aprobarFase(1L, 999L, "ok"));
    }

    @Test
    void aprobarFaseRechazaSiNoEstaPendienteDeTutor() {
        fase1.setEstado("PENDIENTE_ESTUDIANTE");
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> tutoriaService.aprobarFase(1L, 10L, "ok"));
        assertTrue(ex.getMessage().contains("sin correcciones del estudiante"));
    }

    @Test
    void aprobarFaseRechazaSiNoHayPdfDelEstudiante() {
        fase1.setEstado("PENDIENTE_TUTOR");
        fase1.setArchivoPdfEstudiante(null);
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> tutoriaService.aprobarFase(1L, 10L, "ok"));
        assertTrue(ex.getMessage().contains("No existe un PDF"));
    }

    @Test
    void aprobarFaseUsaComentarioPorDefectoSiVieneVacio() {
        fase1.setEstado("PENDIENTE_TUTOR");
        fase1.setArchivoPdfEstudiante("a.pdf");
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        when(tutoriaFaseRepository.save(any(TutoriaFase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioDocente));
        when(tutoriaFaseRepository.countByTutorId(1L)).thenReturn(1L);
        when(tutoriaFaseRepository.countByTutorIdAndEstado(1L, "APROBADA")).thenReturn(1L);

        tutoriaService.aprobarFase(1L, 10L, "   ");

        verify(tutoriaMensajeRepository).save(argThat(m -> "Fase aprobada.".equals(m.getContenido())));
    }

    @Test
    void aprobarFaseCompletaLasTresFasesYActualizaElAnteproyecto() throws Exception {
        // Prepara fisicamente el PDF de la fase 3 en el tempDir, para que Files.copy() real
        // encuentre el origen (mismo mecanismo que usa el codigo de produccion).
        String uploadDir = (String) ReflectionTestUtils.getField(tutoriaService, "uploadDir");
        java.nio.file.Path dirFase3 = java.nio.file.Paths.get(uploadDir, "1", "fase_3");
        java.nio.file.Files.createDirectories(dirFase3);
        java.nio.file.Files.write(dirFase3.resolve("final_fase3.pdf"), "contenido final".getBytes());

        TutoriaFase fase3 = TutoriaFase.builder().id(3L).tutor(tutor).numeroFase(3).estado("PENDIENTE_TUTOR")
                .archivoPdfEstudiante("final_fase3.pdf").sha256Pdf("abc123").tamanoPdfBytes(15L).build();

        when(tutoriaFaseRepository.findById(3L)).thenReturn(Optional.of(fase3));
        when(tutoriaFaseRepository.save(any(TutoriaFase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioDocente));
        when(tutoriaFaseRepository.countByTutorId(1L)).thenReturn(3L);
        when(tutoriaFaseRepository.countByTutorIdAndEstado(1L, "APROBADA")).thenReturn(3L);
        when(tutoriaFaseRepository.findByTutorIdOrderByNumeroFaseAsc(1L)).thenReturn(List.of(fase3));
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));
        Anteproyecto anteproyecto = Anteproyecto.builder().id(1L).build();
        when(anteproyectoRepository.findBySolicitudId(100L)).thenReturn(Optional.of(anteproyecto));
        when(anteproyectoRepository.save(any(Anteproyecto.class))).thenAnswer(inv -> inv.getArgument(0));

        tutoriaService.aprobarFase(3L, 10L, "Fase final aprobada");

        assertEquals("COMPLETADA", tutor.getEstado());
        verify(anteproyectoRepository).save(argThat(a ->
                "final_fase3.pdf".equals(a.getArchivoPdf()) && "APROBADO".equals(a.getEstado())));
    }

    // ── enviarMensaje: ramas restantes ───────────────────────────────────────

    @Test
    void enviarMensajePermiteAlEstudiante() {
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioEstudiante));
        when(tutoriaMensajeRepository.save(any(TutoriaMensaje.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> tutoriaService.enviarMensaje(1L, 20L, "Ya subí el PDF", "RESPUESTA"));
    }

    @Test
    void enviarMensajePermiteAUsuarioPrivilegiadoAunNoSiendoParteDeLaTutoria() {
        Usuario coordinador = Usuario.builder().id(700L).rol("COORDINADOR").build();
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        when(usuarioRepository.findById(700L)).thenReturn(Optional.of(coordinador));
        when(tutoriaMensajeRepository.save(any(TutoriaMensaje.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> tutoriaService.enviarMensaje(1L, 700L, "Mensaje de coordinación", "INFO"));
    }

    // ── marcarMensajesLeidos / listados / registrarAvanceSP / obtenerPdfFase ─

    @Test
    void marcarMensajesLeidosMarcaTodosLosNoLeidos() {
        TutoriaMensaje m1 = TutoriaMensaje.builder().id(1L).leido(false).build();
        TutoriaMensaje m2 = TutoriaMensaje.builder().id(2L).leido(false).build();
        when(tutoriaMensajeRepository.findByFaseIdAndLeidoFalseAndRemitenteIdNot(1L, 20L))
                .thenReturn(new ArrayList<>(List.of(m1, m2)));

        tutoriaService.marcarMensajesLeidos(1L, 20L);

        assertTrue(m1.getLeido());
        assertTrue(m2.getLeido());
        verify(tutoriaMensajeRepository).saveAll(anyList());
    }

    @Test
    void obtenerTutoriasEstudianteDelega() {
        when(tutorRepository.findBySolicitudEstudianteUsuarioId(20L)).thenReturn(List.of(tutor));
        when(tutoriaFaseRepository.findByTutorIdOrderByNumeroFaseAsc(1L)).thenReturn(List.of());

        List<TutoriaResumenDTO> resultado = tutoriaService.obtenerTutoriasEstudiante(20L);
        assertEquals(1, resultado.size());
    }

    @Test
    void obtenerTutoriasDocenteDelega() {
        when(tutorRepository.findByDocenteUsuarioId(10L)).thenReturn(List.of(tutor));
        when(tutoriaFaseRepository.findByTutorIdOrderByNumeroFaseAsc(1L)).thenReturn(List.of());

        List<TutoriaResumenDTO> resultado = tutoriaService.obtenerTutoriasDocente(10L);
        assertEquals(1, resultado.size());
    }

    @Test
    void registrarAvanceSPValidaAccesoYDelegaAlProcedimiento() {
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioDocente));

        tutoriaService.registrarAvanceSP(1L, 2, "archivo.pdf", 1024L, "hash", 10L);

        verify(tutoriaFaseRepository).spRegistrarTutoriaAvance(1L, 2, "archivo.pdf", 1024L, "hash");
    }

    @Test
    void obtenerPdfFaseLanzaSiNoHayArchivo() {
        when(tutoriaFaseRepository.findById(1L)).thenReturn(Optional.of(fase1));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioDocente));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> tutoriaService.obtenerPdfFase(1L, 10L));
        assertTrue(ex.getMessage().contains("no tiene PDF"));
    }
}
