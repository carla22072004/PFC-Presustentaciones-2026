package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.EscalaCriterioDTO;
import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaRequest;
import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.entities.Jurado;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.*;
import ec.edu.uteq.presustentaciones.security.service.SolicitudAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Cubre calcularNotaTribunal(), el cálculo real de promedio de evaluación
 * (equivalente en Java a sp_calcular_promedio_evaluacion mencionado en OBSERVACIONES.md).
 */
@ExtendWith(MockitoExtension.class)
class RubricaEvaluacionServiceImplTest {

    @Mock private EvaluacionCriterioRepository evalCriterioRepo;
    @Mock private CriterioRubricaRepository criterioRepo;
    @Mock private JuradoRepository juradoRepo;
    @Mock private SolicitudRepository solicitudRepo;
    @Mock private RubricaRepository rubricaRepo;
    @Mock private TutorRepository tutorRepo;
    @Mock private EvaluacionFinalRepository evaluacionFinalRepo;
    @Mock private EvaluacionJuradoRepository javaEvaluacionJuradoRepo;
    @Mock private EvaluadorRepository evaluadorRepo;
    @Mock private TipoEvaluadorRepository tipoEvaluadorRepo;
    @Mock private SolicitudAccessService solicitudAccessService;
    @Mock private PermisoService permisoService;

    @InjectMocks
    private RubricaEvaluacionServiceImpl service;

    private static Object[] fila(long evaluadorId, double suma) {
        return new Object[]{evaluadorId, suma};
    }

    /** calcularNotaTribunal() ahora carga la Solicitud para delegar la autorización en
     * SolicitudAccessService (mockeado aquí -- validarAcceso() no-opea por defecto, así que
     * estos tests siguen centrados en el cálculo del promedio, no en la autorización). */
    private void stubSolicitud(Long solicitudId) {
        when(solicitudRepo.findById(solicitudId))
                .thenReturn(Optional.of(Solicitud.builder().id(solicitudId).build()));
    }

    @BeforeEach
    void setUp() {}

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrarEvaluacionRechazaAJuradoQueRegistraANombreDeOtro() {
        // Caso IDOR de escritura, mismo patrón que EvaluacionJuradoServiceTest: un docente que
        // NO es el jurado asignado no puede registrar la evaluación de rúbrica a su nombre. El
        // caso "permitido" para este mismo guardia (validarPuedeRegistrar) ya se prueba a fondo
        // en EvaluacionJuradoServiceTest -- es idéntico en ambos servicios -- así que aquí solo
        // se cubre la regresión específica de este archivo.
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Usuario usuarioDocente = Usuario.builder().id(50L).email("jurado.real@uteq.edu.ec").build();
        Docente docente = Docente.builder().id(1L).usuario(usuarioDocente).build();
        Jurado jurado = Jurado.builder().id(3L).solicitud(solicitud).docente(docente).build();

        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otro.docente@uteq.edu.ec", null,
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_DOCENTE")));
        when(permisoService.esPropioDocente(any(), eq(1L))).thenReturn(false);
        when(permisoService.tienePermiso(any(), eq("EVALUACION_CALIFICAR"))).thenReturn(false);

        EvaluacionRubricaRequest req = new EvaluacionRubricaRequest();
        req.setSolicitudId(7L);
        req.setJuradoId(3L);
        req.setRubricaId(1L);
        req.setCriterios(List.of(new EscalaCriterioDTO()));

        assertThrows(AccessDeniedException.class, () -> service.registrarEvaluacion(req));
    }

    @Test
    void obtenerEvaluacionJuradoPropagaAccessDeniedSiSolicitudAccessServiceLoRechaza() {
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Jurado jurado = Jurado.builder().id(3L).solicitud(solicitud).build();
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        org.mockito.Mockito.doThrow(new AccessDeniedException("No tienes permiso para acceder a la información de esta solicitud"))
                .when(solicitudAccessService).validarAcceso(solicitud, "EVALUACION_CALIFICAR");

        assertThrows(AccessDeniedException.class, () -> service.obtenerEvaluacionJurado(7L, 3L));
    }

    @Test
    void calcularNotaTribunalPromediaLasSumasDeCadaJurado() {
        stubSolicitud(10L);
        // 3 jurados, cada uno con su suma de notas ponderadas por criterio: (90 + 85 + 78) / 3 = 84.33
        List<Object[]> filas = Arrays.asList(fila(1L, 90.0), fila(2L, 85.0), fila(3L, 78.0));
        when(evalCriterioRepo.sumaPorEvaluador(10L)).thenReturn(filas);

        Double nota = service.calcularNotaTribunal(10L);

        assertEquals(84.33, nota, 0.001);
    }

    @Test
    void calcularNotaTribunalRedondeaADosDecimales() {
        stubSolicitud(11L);
        List<Object[]> filas = Arrays.asList(fila(1L, 100.0), fila(2L, 100.0), fila(3L, 66.0));
        when(evalCriterioRepo.sumaPorEvaluador(11L)).thenReturn(filas);

        Double nota = service.calcularNotaTribunal(11L);

        // (100 + 100 + 66) / 3 = 88.666... -> 88.67
        assertEquals(88.67, nota);
    }

    @Test
    void calcularNotaTribunalRetornaNullSiNoHayEvaluaciones() {
        stubSolicitud(12L);
        when(evalCriterioRepo.sumaPorEvaluador(12L)).thenReturn(Collections.emptyList());

        assertNull(service.calcularNotaTribunal(12L));
    }

    @Test
    void calcularNotaTribunalConUnSoloJuradoDevuelveSuPropiaNota() {
        stubSolicitud(13L);
        when(evalCriterioRepo.sumaPorEvaluador(13L)).thenReturn(Collections.singletonList(fila(1L, 95.5)));

        assertEquals(95.5, service.calcularNotaTribunal(13L));
    }
}
