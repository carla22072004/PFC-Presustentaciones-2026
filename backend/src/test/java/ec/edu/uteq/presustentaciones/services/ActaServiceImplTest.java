package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ActaServiceImpl.firmarActa() implementa las reglas reales de negocio (validacion de rol,
 * invocacion de sp_firmar_acta_digital -- Fase 3 / Criterio P1 --, transicion de la solicitud
 * a COMPLETADA solo cuando las 4 firmas estan puestas). Sin test dedicado pese a ser el unico
 * punto del codigo que invoca ese procedimiento almacenado.
 */
@ExtendWith(MockitoExtension.class)
class ActaServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock private ActaRepository actaRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private EvaluacionFinalRepository evaluacionRepository;
    @Mock private JuradoRepository juradoRepository;
    @Mock private EstadoSolicitudRepository estadoSolicitudRepository;
    @Mock private EntityManager entityManager;
    @Mock private NotificacionService notificacionService;
    @Mock private AuditoriaService auditoriaService;
    @Mock private TutorRepository tutorRepository;

    @InjectMocks
    private ActaServiceImpl actaService;

    private Usuario usuarioEstudiante;
    private Estudiante estudiante;
    private Solicitud solicitud;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(actaService, "actasDir", tempDir.toString());

        usuarioEstudiante = Usuario.builder().id(10L).nombre("Ana").apellido("Torres")
                .email("atorres@uteq.edu.ec").rol("ESTUDIANTE").build();
        estudiante = Estudiante.builder().id(3L).usuario(usuarioEstudiante).build();
        solicitud = Solicitud.builder().id(7L).estudiante(estudiante).tituloTema("Sistema X").build();

        // Hallazgo real (2026-09-01): ActaServiceImpl.validarAcceso()/firmarActa() ahora exigen un
        // SecurityContextHolder autenticado (control de acceso real agregado por el equipo). Sin
        // limpiar el contexto entre tests, SecurityContextHolder (ThreadLocal) queda "sucio" entre
        // metodos -- algunos tests pasaban solo por herencia accidental del contexto ADMIN dejado
        // por eliminarActaExitosoSiEsAdmin() al correr antes en el mismo hilo, y fallaban si corrian
        // en otro orden. Se autentica como ADMIN por defecto aqui (bypassa las reglas de propiedad,
        // igual que ya hacia eliminarActaExitosoSiEsAdmin) para que cada test sea determinista sin
        // importar el orden; los tests que SI prueban las reglas de autorizacion (eliminarActa*)
        // sobreescriben este contexto explicitamente como ya lo hacian.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@uteq.edu.ec", null,
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Acta actaConFirmas(boolean presidente, boolean vocal1, boolean vocal2, boolean tutor) {
        Acta acta = Acta.builder()
                .id(1L)
                .solicitud(solicitud)
                .firmadaPresidente(presidente)
                .firmadaVocal1(vocal1)
                .firmadaVocal2(vocal2)
                .firmadaTutor(tutor)
                .build();
        acta.actualizarEstadoFirma();
        return acta;
    }

    @Test
    void firmarActaConRolInvalidoLanzaExcepcionYNoLlamaAlProcedimiento() {
        Acta acta = actaConFirmas(false, false, false, false);
        when(actaRepository.findById(1L)).thenReturn(Optional.of(acta));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> actaService.firmarActa(1L, "SECRETARIO", "obs"));

        assertTrue(ex.getMessage().contains("Rol inválido"));
        verify(actaRepository, never()).firmarActaDigital(any(), any(), any());
    }

    @Test
    void firmarActaParcialNoCompletaLaSolicitudNiRegeneraElPdf() {
        // Ya firmado por Presidente y Vocal 1; esta llamada firma Vocal 2 -- Tutor sigue pendiente.
        Acta acta = actaConFirmas(true, true, false, false);
        when(actaRepository.findById(1L)).thenReturn(Optional.of(acta));
        when(actaRepository.save(any(Acta.class))).thenAnswer(inv -> inv.getArgument(0));

        Acta resultado = actaService.firmarActa(1L, "vocal_2", "Todo correcto");

        verify(actaRepository).firmarActaDigital(1L, "VOCAL_2", "Todo correcto");
        verify(entityManager).refresh(acta);
        assertFalse(resultado.isFirmada());
        verify(solicitudRepository, never()).save(any());
        verify(estadoSolicitudRepository, never()).findByCodigo(any());
    }

    @Test
    void firmarActaCuandoQuedanLas4FirmasCompletaLaSolicitudYNotifica() {
        // Las 4 ya estaban en true al recargar (entityManager.refresh esta mockeado como no-op,
        // asi que el objeto que devuelve findById ya simula el estado post-SP/post-refresh).
        Acta acta = actaConFirmas(true, true, true, true);
        acta.setArchivoPdf("acta_7.pdf");
        when(actaRepository.findById(1L)).thenReturn(Optional.of(acta));
        when(actaRepository.save(any(Acta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estadoSolicitudRepository.findByCodigo("COMPLETADA"))
                .thenReturn(Optional.of(EstadoSolicitud.builder().codigo("COMPLETADA").nombre("Completada").build()));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));
        when(evaluacionRepository.findBySolicitudId(7L)).thenReturn(Optional.empty());
        when(juradoRepository.findBySolicitudId(7L)).thenReturn(List.of());

        Acta resultado = actaService.firmarActa(1L, "TUTOR", null);

        assertTrue(resultado.isFirmada());
        assertEquals("COMPLETADA", solicitud.getEstado().getCodigo());
        verify(solicitudRepository).save(solicitud);
        verify(notificacionService).crearNotificacion(eq(10L), contains("firmó"));
        verify(notificacionService).crearNotificacion(eq(10L), contains("finalizado"));
        // El PDF se regenera de verdad (iText real) en tempDir -- confirma que generarPdf()
        // corre sin lanzar excepcion con datos minimos (evaluacion/jurados vacios).
        assertTrue(tempDir.resolve("acta_7.pdf").toFile().exists());
    }

    @Test
    void firmarActaSiFallaLaNotificacionNoPropagaLaExcepcion() {
        Acta acta = actaConFirmas(true, true, false, false);
        when(actaRepository.findById(1L)).thenReturn(Optional.of(acta));
        when(actaRepository.save(any(Acta.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("servicio de notificaciones caido"))
                .when(notificacionService).crearNotificacion(any(), any());

        assertDoesNotThrow(() -> actaService.firmarActa(1L, "VOCAL_2", null));
    }

    @Test
    void obtenerPdfBytesLanzaExcepcionSiElActaNoTienePdfGenerado() {
        Acta acta = actaConFirmas(false, false, false, false);
        when(actaRepository.findById(1L)).thenReturn(Optional.of(acta));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> actaService.obtenerPdfBytes(1L));
        assertTrue(ex.getMessage().contains("PDF"));
    }

    @Test
    void obtenerPdfBytesLanzaExcepcionSiElActaNoExiste() {
        when(actaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> actaService.obtenerPdfBytes(99L));
    }

    // generarActa() -- RF-06 (Must), endpoint POST /api/v1/actas/generar. Sin test dedicado
    // pese a que matriz.csv lo cita explicitamente como "ninguna (ActaServiceImplTest.java
    // no existe)" -- ahora existe.
    @Test
    void generarActaLanzaExcepcionSiLaSolicitudNoExiste() {
        when(solicitudRepository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> actaService.generarActa(7L));
        verify(actaRepository, never()).save(any());
    }

    @Test
    void generarActaRetornaLaExistenteSinRegenerarElPdf() {
        Acta existente = actaConFirmas(false, false, false, false);
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(solicitud));
        when(evaluacionRepository.findBySolicitudId(7L)).thenReturn(Optional.of(new ec.edu.uteq.presustentaciones.entities.EvaluacionFinal()));
        when(juradoRepository.findBySolicitudId(7L)).thenReturn(List.of());
        when(actaRepository.findBySolicitudId(7L)).thenReturn(Optional.of(existente));

        Acta resultado = actaService.generarActa(7L);

        assertSame(existente, resultado);
        verify(actaRepository, never()).save(any());
    }

    @Test
    void generarActaCreaUnaNuevaConPdfRealYLaGuarda() {
        when(solicitudRepository.findById(7L)).thenReturn(Optional.of(solicitud));
        when(evaluacionRepository.findBySolicitudId(7L)).thenReturn(Optional.of(new ec.edu.uteq.presustentaciones.entities.EvaluacionFinal()));
        when(juradoRepository.findBySolicitudId(7L)).thenReturn(List.of());
        when(actaRepository.findBySolicitudId(7L)).thenReturn(Optional.empty());
        when(actaRepository.save(any(Acta.class))).thenAnswer(inv -> inv.getArgument(0));

        Acta resultado = actaService.generarActa(7L);

        assertNotNull(resultado);
        assertEquals(solicitud, resultado.getSolicitud());
        assertNotNull(resultado.getArchivoPdf());
        assertTrue(tempDir.resolve(resultado.getArchivoPdf()).toFile().exists());
        verify(actaRepository).save(any(Acta.class));
    }

    @Test
    void buscarPorSolicitudDelegaAlRepositorio() {
        Acta acta = actaConFirmas(false, false, false, false);
        when(actaRepository.findBySolicitudId(7L)).thenReturn(Optional.of(acta));

        Optional<Acta> resultado = actaService.buscarPorSolicitud(7L);

        assertTrue(resultado.isPresent());
        verify(actaRepository).findBySolicitudId(7L);
    }

    @Test
    void eliminarActaExitosoSiEsAdmin() {
        Acta acta = actaConFirmas(false, false, false, false);
        acta.setArchivoPdf("test.pdf");
        when(actaRepository.findById(1L)).thenReturn(Optional.of(acta));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@uteq.edu.ec", null,
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

        assertDoesNotThrow(() -> actaService.eliminarActa(1L));
        verify(actaRepository).delete(acta);
    }

    @Test
    void eliminarActaLanzaExcepcionSiNoTieneAcceso() {
        Acta acta = actaConFirmas(false, false, false, false);
        when(actaRepository.findById(1L)).thenReturn(Optional.of(acta));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otro@uteq.edu.ec", null,
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ESTUDIANTE")));

        when(juradoRepository.findBySolicitudId(7L)).thenReturn(List.of());
        when(tutorRepository.findBySolicitudId(7L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> actaService.eliminarActa(1L));
        assertTrue(ex.getMessage().contains("No tienes permiso"));
        verify(actaRepository, never()).delete(any());
    }
}
