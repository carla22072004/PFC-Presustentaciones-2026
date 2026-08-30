package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Cobertura priorizada en docs/mediciones/jacoco/COVERAGE.md: SolicitudServiceImpl por sus
 * reglas de transición de estado (CREADA -> ENVIADA -> APROBADA/RECHAZADA, y SUSPENDIDA
 * desde cualquier estado que no sea CREADA/RECHAZADA/SUSPENDIDA). Fase 4/6.
 */
@ExtendWith(MockitoExtension.class)
class SolicitudServiceImplTest {

    @Mock private SolicitudRepository solicitudRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private AnteproyectoRepository anteproyectoRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstadoSolicitudRepository estadoSolicitudRepository;
    @Mock private ModalidadTitulacionRepository modalidadTitulacionRepository;
    @Mock private ConvocatoriaTitulacionRepository convocatoriaTitulacionRepository;
    @Mock private CarreraRepository carreraRepository;
    @Mock private PeriodoAcademicoRepository periodoAcademicoRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private EstadoAcademicoRepository estadoAcademicoRepository;

    @InjectMocks
    private SolicitudServiceImpl solicitudService;

    private Estudiante estudiante;
    private Usuario usuarioEstudiante;
    private ModalidadTitulacion modalidad;
    private ConvocatoriaTitulacion convocatoria;

    @BeforeEach
    void setUp() {
        usuarioEstudiante = Usuario.builder().id(201L).nombre("Mario").apellido("Alvarado")
                .email("malvarado@uteq.edu.ec").rol("ESTUDIANTE").build();
        estudiante = Estudiante.builder().id(5L).usuario(usuarioEstudiante).build();
        modalidad = ModalidadTitulacion.builder().id((short) 1).codigo("PROYECTO").nombre("Proyecto Tecnológico").build();
        convocatoria = ConvocatoriaTitulacion.builder().id(1).codigo("CONV-2026-01").activa(true).build();
    }

    private EstadoSolicitud estado(String codigo) {
        return EstadoSolicitud.builder().codigo(codigo).nombre(codigo).build();
    }

    @Test
    void testCrearSolicitudExitosa() {
        Solicitud datos = Solicitud.builder().tituloTema("Sistema X").modalidadTitulacion(modalidad).build();

        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(estudiante));
        when(estadoSolicitudRepository.findByCodigo("CREADA")).thenReturn(Optional.of(estado("CREADA")));
        when(modalidadTitulacionRepository.findById((short) 1)).thenReturn(Optional.of(modalidad));
        when(convocatoriaTitulacionRepository.findFirstByActivaTrue()).thenReturn(Optional.of(convocatoria));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        Solicitud creada = solicitudService.crearSolicitud(5L, datos);

        assertNotNull(creada);
        assertEquals("CREADA", creada.getEstado().getCodigo());
        assertEquals(estudiante, creada.getEstudiante());
        verify(solicitudRepository).save(any(Solicitud.class));
    }

    @Test
    void testCrearSolicitudFallaSinModalidad() {
        Solicitud datos = Solicitud.builder().tituloTema("Sistema X").build();
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(estudiante));
        when(estadoSolicitudRepository.findByCodigo("CREADA")).thenReturn(Optional.of(estado("CREADA")));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                solicitudService.crearSolicitud(5L, datos));
        assertTrue(ex.getMessage().contains("modalidad"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void testCrearSolicitudFallaSiEstudianteNoExiste() {
        Solicitud datos = Solicitud.builder().tituloTema("Sistema X").modalidadTitulacion(modalidad).build();
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> solicitudService.crearSolicitud(99L, datos));
    }

    @Test
    void testCrearSolicitudPorUsuarioCreaPerfilEstudianteAutomaticamente() {
        Carrera carrera = Carrera.builder().id(1).nombre("Ingeniería en Software").build();
        Solicitud datos = Solicitud.builder().tituloTema("Sistema Y").modalidadTitulacion(modalidad).build();

        when(estudianteRepository.findByUsuarioId(201L)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(201L)).thenReturn(Optional.of(usuarioEstudiante));
        when(carreraRepository.findAll()).thenReturn(List.of(carrera));
        // sp_generar_codigo_expediente (Fase 3): verifica que el service SI llama al SP
        // al crear el perfil, no que calcule el codigo el mismo en Java.
        when(estudianteRepository.generarCodigoExpediente(null, null)).thenReturn("EXP-2026-00007");
        when(estadoAcademicoRepository.findByCodigo("ACTIVO"))
                .thenReturn(Optional.of(EstadoAcademico.builder().codigo("ACTIVO").nombre("Activo").build()));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(inv -> {
            Estudiante e = inv.getArgument(0);
            e.setId(5L);
            return e;
        });
        when(estudianteRepository.findById(5L)).thenReturn(Optional.of(estudiante));
        when(estadoSolicitudRepository.findByCodigo("CREADA")).thenReturn(Optional.of(estado("CREADA")));
        when(modalidadTitulacionRepository.findById((short) 1)).thenReturn(Optional.of(modalidad));
        when(convocatoriaTitulacionRepository.findFirstByActivaTrue()).thenReturn(Optional.of(convocatoria));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        solicitudService.crearSolicitudPorUsuario(201L, datos);

        verify(estudianteRepository).generarCodigoExpediente(null, null);
        verify(estudianteRepository).save(argThat(e -> "EXP-2026-00007".equals(e.getExpedienteCodigo())));
    }

    @Test
    void testEnviarSolicitudFallaSinPdfAnteproyecto() {
        Solicitud s = Solicitud.builder().id(10L).estudiante(estudiante).tituloTema("X").build();
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(s));
        when(anteproyectoRepository.findBySolicitudId(10L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> solicitudService.enviarSolicitud(10L));
        assertTrue(ex.getMessage().contains("anteproyecto"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void testEnviarSolicitudExitosaConPdf() {
        Solicitud s = Solicitud.builder().id(10L).estudiante(estudiante).tituloTema("X").build();
        Anteproyecto ap = Anteproyecto.builder().archivoPdf("anteproyecto.pdf").build();

        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(s));
        when(anteproyectoRepository.findBySolicitudId(10L)).thenReturn(Optional.of(ap));
        when(estadoSolicitudRepository.findByCodigo("ENVIADA")).thenReturn(Optional.of(estado("ENVIADA")));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        Solicitud enviada = solicitudService.enviarSolicitud(10L);

        assertEquals("ENVIADA", enviada.getEstado().getCodigo());
    }

    @Test
    void testAprobarSolicitudTransicionaAAprobada() {
        Solicitud s = Solicitud.builder().id(10L).estudiante(estudiante).tituloTema("X").estado(estado("ENVIADA")).build();
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(s));
        when(estadoSolicitudRepository.findByCodigo("APROBADA")).thenReturn(Optional.of(estado("APROBADA")));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        Solicitud aprobada = solicitudService.aprobarSolicitud(10L);

        assertEquals("APROBADA", aprobada.getEstado().getCodigo());
    }

    @Test
    void testRechazarConObservacionGuardaElMotivo() {
        Solicitud s = Solicitud.builder().id(10L).estudiante(estudiante).tituloTema("X").estado(estado("ENVIADA")).build();
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(s));
        when(estadoSolicitudRepository.findByCodigo("RECHAZADA")).thenReturn(Optional.of(estado("RECHAZADA")));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        Solicitud rechazada = solicitudService.rechazarConObservacion(10L, "Tema ya registrado por otro estudiante");

        assertEquals("RECHAZADA", rechazada.getEstado().getCodigo());
        assertEquals("Tema ya registrado por otro estudiante", rechazada.getObservaciones());
    }

    @Test
    void testSuspenderSolicitudFallaEnEstadoCreada() {
        Solicitud s = Solicitud.builder().id(10L).estudiante(estudiante).tituloTema("X").estado(estado("CREADA")).build();
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(s));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                solicitudService.suspenderSolicitud(10L, "motivo cualquiera"));
        assertTrue(ex.getMessage().contains("no puede ser suspendida"));
    }

    @Test
    void testSuspenderSolicitudFallaSinMotivo() {
        Solicitud s = Solicitud.builder().id(10L).estudiante(estudiante).tituloTema("X").estado(estado("EVALUACION")).build();
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(s));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                solicitudService.suspenderSolicitud(10L, "  "));
        assertTrue(ex.getMessage().contains("motivo"));
    }

    @Test
    void testSuspenderSolicitudExitosaDesdeEstadoSuspendible() {
        Solicitud s = Solicitud.builder().id(10L).estudiante(estudiante).tituloTema("X").estado(estado("EVALUACION")).build();
        when(solicitudRepository.findById(10L)).thenReturn(Optional.of(s));
        when(estadoSolicitudRepository.findByCodigo("SUSPENDIDA")).thenReturn(Optional.of(estado("SUSPENDIDA")));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        Solicitud suspendida = solicitudService.suspenderSolicitud(10L, "Estudiante retirado del período");

        assertEquals("SUSPENDIDA", suspendida.getEstado().getCodigo());
        assertEquals("Estudiante retirado del período", suspendida.getMotivoSuspension());
        assertNotNull(suspendida.getSuspendidoEn());
    }
}
