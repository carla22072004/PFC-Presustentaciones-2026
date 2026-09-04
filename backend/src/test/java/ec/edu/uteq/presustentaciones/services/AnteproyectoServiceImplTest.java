package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Anteproyecto;
import ec.edu.uteq.presustentaciones.entities.EstadoSolicitud;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.AnteproyectoRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.security.service.SolicitudAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnteproyectoServiceImplTest {

    @Mock
    private AnteproyectoRepository anteproyectoRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SolicitudAccessService solicitudAccessService;

    @InjectMocks
    private AnteproyectoServiceImpl anteproyectoService;

    @TempDir
    Path tempDir;

    private Solicitud solicitud;
    private Usuario estudianteUsuario;
    private Estudiante estudiante;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(anteproyectoService, "uploadDir", tempDir.resolve("anteproyectos").toString());

        estudianteUsuario = Usuario.builder().id(5L).nombre("Carlos").apellido("Mendoza").email("cmendoza@uteq.edu.ec").rol("ESTUDIANTE").build();
        estudiante = Estudiante.builder().id(1L).usuario(estudianteUsuario).build();
        solicitud = Solicitud.builder().id(10L).estudiante(estudiante).tituloTema("Sistema IA").build();

        // validarPuedeSubir() exige un SecurityContextHolder autenticado (mismo patron que
        // ActaServiceImplTest); se autentica como el propio estudiante dueno de la solicitud,
        // que es el caso real que enviarAnteproyecto() protege.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cmendoza@uteq.edu.ec", null,
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ESTUDIANTE")));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEnviarAnteproyectoExitoso() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));
        when(anteproyectoRepository.findBySolicitudId(10L)).thenReturn(Optional.empty());
        when(anteproyectoRepository.save(any(Anteproyecto.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile archivoPdf = new MockMultipartFile(
                "archivo", "anteproyecto.pdf", "application/pdf", "%PDF-1.4 contenido".getBytes());

        Anteproyecto ap = anteproyectoService.enviarAnteproyecto(10L, archivoPdf);

        assertNotNull(ap);
        assertEquals("ENVIADO", ap.getEstado());
        assertNotNull(ap.getSha256Hash());
        verify(anteproyectoRepository).save(any(Anteproyecto.class));
    }

    @Test
    void testEnviarAnteproyectoFallaSiSolicitudSuspendida() {
        EstadoSolicitud susp = EstadoSolicitud.builder().codigo("SUSPENDIDA").nombre("Suspendida").build();
        solicitud.setEstado(susp);
        solicitud.setMotivoSuspension("Incumplimiento de fechas");

        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));

        MockMultipartFile archivoPdf = new MockMultipartFile(
                "archivo", "anteproyecto.pdf", "application/pdf", "%PDF-1.4 contenido".getBytes());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                anteproyectoService.enviarAnteproyecto(10L, archivoPdf));
        assertTrue(ex.getMessage().contains("suspendido"));
    }

    @Test
    void testEnviarAnteproyectoFallaSiNoEsPdf() {
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));

        MockMultipartFile archivoTxt = new MockMultipartFile(
                "archivo", "anteproyecto.txt", "text/plain", "texto plano".getBytes());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                anteproyectoService.enviarAnteproyecto(10L, archivoTxt));
        assertTrue(ex.getMessage().contains("Solo se permiten archivos PDF"));
    }

    @Test
    void testEnviarAnteproyectoRechazaAUsuarioQueNoEsElDuenoDeLaSolicitud() {
        // Caso IDOR de escritura: un tercero (otro estudiante) intenta subir el PDF de una
        // solicitud que no le pertenece cambiando el solicitudId.
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(solicitud));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otro.estudiante@uteq.edu.ec", null,
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ESTUDIANTE")));

        MockMultipartFile archivoPdf = new MockMultipartFile(
                "archivo", "anteproyecto.pdf", "application/pdf", "%PDF-1.4 contenido".getBytes());

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> anteproyectoService.enviarAnteproyecto(10L, archivoPdf));
    }

    @Test
    void testBuscarPorSolicitudPropagaAccessDeniedSiSolicitudAccessServiceLoRechaza() {
        // Caso IDOR de lectura: el anteproyecto existe pero SolicitudAccessService decide que
        // este usuario no participa en la solicitud (estudiante ajeno, ni jurado ni tutor).
        Anteproyecto ap = Anteproyecto.builder().id(1L).solicitud(solicitud).estado("ENVIADO").build();
        when(anteproyectoRepository.findBySolicitudId(10L)).thenReturn(Optional.of(ap));
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException(
                        "No tienes permiso para acceder a la información de esta solicitud"))
                .when(solicitudAccessService).validarAcceso(solicitud, "ANTEPROYECTO_REVISAR");

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> anteproyectoService.buscarPorSolicitud(10L));
    }

    @Test
    void testAprobarAnteproyecto() {
        Anteproyecto ap = Anteproyecto.builder().id(1L).solicitud(solicitud).estado("PENDIENTE").build();
        when(anteproyectoRepository.findById(1L)).thenReturn(Optional.of(ap));
        when(anteproyectoRepository.save(any(Anteproyecto.class))).thenAnswer(inv -> inv.getArgument(0));

        Anteproyecto resultado = anteproyectoService.aprobarAnteproyecto(1L, "Cumple con todos los requisitos.");

        assertNotNull(resultado);
        assertEquals("APROBADO", resultado.getEstado());
        assertEquals("Cumple con todos los requisitos.", resultado.getObservaciones());
        verify(notificacionService).crearNotificacion(eq(5L), anyString());
    }

    @Test
    void testRechazarAnteproyecto() {
        Anteproyecto ap = Anteproyecto.builder().id(1L).solicitud(solicitud).estado("PENDIENTE").build();
        when(anteproyectoRepository.findById(1L)).thenReturn(Optional.of(ap));
        when(anteproyectoRepository.save(any(Anteproyecto.class))).thenAnswer(inv -> inv.getArgument(0));

        Anteproyecto resultado = anteproyectoService.rechazarAnteproyecto(1L, "Falta marco teórico.");

        assertNotNull(resultado);
        assertEquals("RECHAZADO", resultado.getEstado());
        assertEquals("Falta marco teórico.", resultado.getObservaciones());
        verify(notificacionService).crearNotificacion(eq(5L), anyString());
    }
}
