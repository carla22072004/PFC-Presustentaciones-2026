package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.EscalaCriterioDTO;
import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaRequest;
import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaResponse;
import ec.edu.uteq.presustentaciones.dto.ObservacionesSolicitudDTO;
import ec.edu.uteq.presustentaciones.entities.*;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    // ── registrarEvaluacion: validaciones y flujo completo ──────────────────

    private void autenticarComoAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@uteq.edu.ec", null,
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    private EvaluacionRubricaRequest requestBase(Long solicitudId, Long juradoId, Long rubricaId) {
        EvaluacionRubricaRequest req = new EvaluacionRubricaRequest();
        req.setSolicitudId(solicitudId);
        req.setJuradoId(juradoId);
        req.setRubricaId(rubricaId);
        return req;
    }

    @Test
    void registrarEvaluacionLanzaSiSolicitudNoExiste() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.registrarEvaluacion(requestBase(7L, 3L, 1L)));
        assertTrue(ex.getMessage().contains("Solicitud no encontrada"));
    }

    @Test
    void registrarEvaluacionLanzaSiJuradoNoExiste() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(Solicitud.builder().id(7L).build()));
        when(juradoRepo.findById(3L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.registrarEvaluacion(requestBase(7L, 3L, 1L)));
        assertTrue(ex.getMessage().contains("Jurado no encontrado"));
    }

    @Test
    void registrarEvaluacionLanzaSiJuradoNoPerteneceALaSolicitud() {
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Solicitud otraSolicitud = Solicitud.builder().id(999L).build();
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(Jurado.builder().id(3L).solicitud(otraSolicitud).build()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.registrarEvaluacion(requestBase(7L, 3L, 1L)));
        assertTrue(ex.getMessage().contains("no pertenece a esta solicitud"));
    }

    private Jurado juradoDeSolicitud(Solicitud solicitud, Docente docente) {
        return Jurado.builder().id(3L).solicitud(solicitud).docente(docente)
                .rolJurado(RolJurado.builder().codigo("PRESIDENTE").nombre("Presidente").build()).build();
    }

    @Test
    void registrarEvaluacionLanzaSiRubricaNoExiste() {
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Docente docente = Docente.builder().id(1L).usuario(Usuario.builder().id(50L).build()).build();
        Jurado jurado = juradoDeSolicitud(solicitud, docente);
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        autenticarComoAdmin();
        when(rubricaRepo.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.registrarEvaluacion(requestBase(7L, 3L, 1L)));
        assertTrue(ex.getMessage().contains("Rúbrica no encontrada"));
    }

    @Test
    void registrarEvaluacionLanzaSiRubricaSinCriterios() {
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Docente docente = Docente.builder().id(1L).usuario(Usuario.builder().id(50L).build()).build();
        Jurado jurado = juradoDeSolicitud(solicitud, docente);
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        autenticarComoAdmin();
        when(rubricaRepo.findById(1L)).thenReturn(Optional.of(Rubrica.builder().id(1L).build()));
        when(criterioRepo.findByRubricaIdOrderByOrdenAsc(1L)).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.registrarEvaluacion(requestBase(7L, 3L, 1L)));
        assertTrue(ex.getMessage().contains("no tiene criterios"));
    }

    @Test
    void registrarEvaluacionLanzaSiNoEvaluaTodosLosCriterios() {
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Docente docente = Docente.builder().id(1L).usuario(Usuario.builder().id(50L).build()).build();
        Jurado jurado = juradoDeSolicitud(solicitud, docente);
        CriterioRubrica c1 = CriterioRubrica.builder().id(1L).nombre("Claridad").ponderacion(50.0).build();
        CriterioRubrica c2 = CriterioRubrica.builder().id(2L).nombre("Rigor").ponderacion(50.0).build();
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        autenticarComoAdmin();
        when(rubricaRepo.findById(1L)).thenReturn(Optional.of(Rubrica.builder().id(1L).build()));
        when(criterioRepo.findByRubricaIdOrderByOrdenAsc(1L)).thenReturn(List.of(c1, c2));

        EvaluacionRubricaRequest req = requestBase(7L, 3L, 1L);
        EscalaCriterioDTO dto1 = new EscalaCriterioDTO();
        dto1.setCriterioId(1L);
        dto1.setEscala(80);
        req.setCriterios(List.of(dto1)); // solo 1 de 2

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.registrarEvaluacion(req));
        assertTrue(ex.getMessage().contains("Debe evaluar todos los 2 criterios"));
    }

    @Test
    void registrarEvaluacionLanzaSiEscalaFueraDeRango() {
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Docente docente = Docente.builder().id(1L).usuario(Usuario.builder().id(50L).build()).build();
        Jurado jurado = juradoDeSolicitud(solicitud, docente);
        CriterioRubrica c1 = CriterioRubrica.builder().id(1L).nombre("Claridad").ponderacion(100.0).build();
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        autenticarComoAdmin();
        when(rubricaRepo.findById(1L)).thenReturn(Optional.of(Rubrica.builder().id(1L).build()));
        when(criterioRepo.findByRubricaIdOrderByOrdenAsc(1L)).thenReturn(List.of(c1));

        EvaluacionRubricaRequest req = requestBase(7L, 3L, 1L);
        EscalaCriterioDTO dto1 = new EscalaCriterioDTO();
        dto1.setCriterioId(1L);
        dto1.setEscala(150);
        req.setCriterios(List.of(dto1));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.registrarEvaluacion(req));
        assertTrue(ex.getMessage().contains("Escala inválida"));
    }

    @Test
    void registrarEvaluacionLanzaSiCriterioDelDtoNoExisteEnLaRubrica() {
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Docente docente = Docente.builder().id(1L).usuario(Usuario.builder().id(50L).build()).build();
        Jurado jurado = juradoDeSolicitud(solicitud, docente);
        CriterioRubrica c1 = CriterioRubrica.builder().id(1L).nombre("Claridad").ponderacion(100.0).build();
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        autenticarComoAdmin();
        when(rubricaRepo.findById(1L)).thenReturn(Optional.of(Rubrica.builder().id(1L).build()));
        when(criterioRepo.findByRubricaIdOrderByOrdenAsc(1L)).thenReturn(List.of(c1));
        when(evaluadorRepo.findBySolicitudIdAndDocenteIdAndTipoEvaluadorCodigo(7L, 1L, "JURADO"))
                .thenReturn(Optional.of(Evaluador.builder().id(20L).build()));

        EvaluacionRubricaRequest req = requestBase(7L, 3L, 1L);
        EscalaCriterioDTO dto1 = new EscalaCriterioDTO();
        dto1.setCriterioId(999L); // no coincide con ningún criterio de la rúbrica
        dto1.setEscala(80);
        req.setCriterios(List.of(dto1));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.registrarEvaluacion(req));
        assertTrue(ex.getMessage().contains("Criterio no encontrado"));
    }

    @Test
    void registrarEvaluacionLanzaSiTipoEvaluadorJuradoNoConfigurado() {
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Docente docente = Docente.builder().id(1L).usuario(Usuario.builder().id(50L).build()).build();
        Jurado jurado = juradoDeSolicitud(solicitud, docente);
        CriterioRubrica c1 = CriterioRubrica.builder().id(1L).nombre("Claridad").ponderacion(100.0).build();
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        autenticarComoAdmin();
        when(rubricaRepo.findById(1L)).thenReturn(Optional.of(Rubrica.builder().id(1L).build()));
        when(criterioRepo.findByRubricaIdOrderByOrdenAsc(1L)).thenReturn(List.of(c1));
        when(evaluadorRepo.findBySolicitudIdAndDocenteIdAndTipoEvaluadorCodigo(7L, 1L, "JURADO"))
                .thenReturn(Optional.empty());
        when(tipoEvaluadorRepo.findByCodigo("JURADO")).thenReturn(Optional.empty());

        EvaluacionRubricaRequest req = requestBase(7L, 3L, 1L);
        EscalaCriterioDTO dto1 = new EscalaCriterioDTO();
        dto1.setCriterioId(1L);
        dto1.setEscala(80);
        req.setCriterios(List.of(dto1));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.registrarEvaluacion(req));
        assertTrue(ex.getMessage().contains("Tipo evaluador JURADO no configurado"));
    }

    @Test
    void registrarEvaluacionExitosaCreaEvaluadorNuevoYCalculaNota() {
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Docente docente = Docente.builder().id(1L).usuario(Usuario.builder().id(50L).nombre("Ana").apellido("Ruiz").build()).build();
        Jurado jurado = juradoDeSolicitud(solicitud, docente);
        CriterioRubrica c1 = CriterioRubrica.builder().id(1L).nombre("Claridad").ponderacion(60.0).build();
        CriterioRubrica c2 = CriterioRubrica.builder().id(2L).nombre("Rigor").ponderacion(40.0).build();
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        autenticarComoAdmin();
        when(rubricaRepo.findById(1L)).thenReturn(Optional.of(Rubrica.builder().id(1L).build()));
        when(criterioRepo.findByRubricaIdOrderByOrdenAsc(1L)).thenReturn(List.of(c1, c2));
        when(evaluadorRepo.findBySolicitudIdAndDocenteIdAndTipoEvaluadorCodigo(7L, 1L, "JURADO"))
                .thenReturn(Optional.empty());
        when(tipoEvaluadorRepo.findByCodigo("JURADO"))
                .thenReturn(Optional.of(TipoEvaluador.builder().id((short) 1).codigo("JURADO").build()));
        when(evaluadorRepo.save(any(Evaluador.class))).thenAnswer(inv -> {
            Evaluador e = inv.getArgument(0);
            e.setId(20L);
            return e;
        });
        when(evalCriterioRepo.save(any(EvaluacionCriterio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(juradoRepo.findBySolicitudId(7L)).thenReturn(List.of(jurado));
        when(evalCriterioRepo.sumaPorEvaluador(7L)).thenReturn(List.<Object[]>of(fila(20L, 92.0)));

        EvaluacionRubricaRequest req = requestBase(7L, 3L, 1L);
        EscalaCriterioDTO dto1 = new EscalaCriterioDTO();
        dto1.setCriterioId(1L);
        dto1.setEscala(100); // nota = 60 * 100 / 100 = 60
        EscalaCriterioDTO dto2 = new EscalaCriterioDTO();
        dto2.setCriterioId(2L);
        dto2.setEscala(80); // nota = 40 * 80 / 100 = 32
        req.setCriterios(List.of(dto1, dto2));

        EvaluacionRubricaResponse resp = service.registrarEvaluacion(req);

        assertEquals(92.0, resp.getNotaTotalJurado()); // 60 + 32
        assertEquals("Ana Ruiz", resp.getNombreJurado());
        // tribunalCompleto no se afirma aqui: el evaluador recien creado no se re-stubea para
        // la segunda consulta que hace buildResponse -- ese caso ya se cubre en
        // obtenerEvaluacionesSolicitudMarcaTribunalIncompletoSiFaltaUnJurado.
        assertEquals(2, resp.getDetalles().size());
        verify(evalCriterioRepo).deleteBySolicitudIdAndEvaluadorId(7L, 20L);
        verify(evaluadorRepo).save(any(Evaluador.class));
    }

    @Test
    void registrarEvaluacionReusaEvaluadorExistenteYPermiteReevaluacion() {
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Docente docente = Docente.builder().id(1L).usuario(Usuario.builder().id(50L).nombre("Ana").apellido("Ruiz").build()).build();
        Jurado jurado = juradoDeSolicitud(solicitud, docente);
        CriterioRubrica c1 = CriterioRubrica.builder().id(1L).nombre("Claridad").ponderacion(100.0).build();
        Evaluador evaluadorExistente = Evaluador.builder().id(20L).build();
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        autenticarComoAdmin();
        when(rubricaRepo.findById(1L)).thenReturn(Optional.of(Rubrica.builder().id(1L).build()));
        when(criterioRepo.findByRubricaIdOrderByOrdenAsc(1L)).thenReturn(List.of(c1));
        when(evaluadorRepo.findBySolicitudIdAndDocenteIdAndTipoEvaluadorCodigo(7L, 1L, "JURADO"))
                .thenReturn(Optional.of(evaluadorExistente));
        when(evalCriterioRepo.save(any(EvaluacionCriterio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(juradoRepo.findBySolicitudId(7L)).thenReturn(List.of(jurado));
        when(evalCriterioRepo.existsBySolicitudIdAndEvaluadorId(7L, 20L)).thenReturn(true);
        when(evalCriterioRepo.sumaPorEvaluador(7L)).thenReturn(List.<Object[]>of(fila(20L, 50.0)));

        EvaluacionRubricaRequest req = requestBase(7L, 3L, 1L);
        EscalaCriterioDTO dto1 = new EscalaCriterioDTO();
        dto1.setCriterioId(1L);
        dto1.setEscala(50);
        req.setCriterios(List.of(dto1));

        service.registrarEvaluacion(req);

        verify(evaluadorRepo, never()).save(any());
        verify(evalCriterioRepo).deleteBySolicitudIdAndEvaluadorId(7L, 20L);
    }

    // ── obtenerEvaluacionesSolicitud ─────────────────────────────────────────

    @Test
    void obtenerEvaluacionesSolicitudLanzaSiSolicitudNoExiste() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.obtenerEvaluacionesSolicitud(7L));
    }

    @Test
    void obtenerEvaluacionesSolicitudMarcaTribunalIncompletoSiFaltaUnJurado() {
        Solicitud solicitud = Solicitud.builder().id(7L).build();
        Docente docente1 = Docente.builder().id(1L).usuario(Usuario.builder().id(50L).nombre("A").apellido("B").build()).build();
        Docente docente2 = Docente.builder().id(2L).usuario(Usuario.builder().id(51L).nombre("C").apellido("D").build()).build();
        Jurado j1 = juradoDeSolicitud(solicitud, docente1);
        Jurado j2 = Jurado.builder().id(4L).solicitud(solicitud).docente(docente2)
                .rolJurado(RolJurado.builder().codigo("VOCAL_1").build()).build();
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findBySolicitudId(7L)).thenReturn(List.of(j1, j2));
        when(evaluadorRepo.findBySolicitudIdAndDocenteIdAndTipoEvaluadorCodigo(7L, 1L, "JURADO"))
                .thenReturn(Optional.of(Evaluador.builder().id(20L).build()));
        when(evalCriterioRepo.findBySolicitudIdAndEvaluadorId(7L, 20L)).thenReturn(List.of());
        // docente2 (jurado j2) aun no tiene evaluador -> tribunal incompleto
        when(evaluadorRepo.findBySolicitudIdAndDocenteIdAndTipoEvaluadorCodigo(7L, 2L, "JURADO"))
                .thenReturn(Optional.empty());

        List<EvaluacionRubricaResponse> resultado = service.obtenerEvaluacionesSolicitud(7L);

        assertEquals(2, resultado.size());
        assertFalse(resultado.get(0).isTribunalCompleto());
    }

    // ── obtenerObservacionesSolicitud ────────────────────────────────────────

    @Test
    void obtenerObservacionesSolicitudLanzaSiSolicitudNoExiste() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.obtenerObservacionesSolicitud(7L));
    }

    @Test
    void obtenerObservacionesSolicitudDevuelveTodoVacioSinTutorJuradosNiEvaluacionFinal() {
        Solicitud solicitud = Solicitud.builder().id(7L).tituloTema("Tema").build();
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(tutorRepo.findBySolicitudId(7L)).thenReturn(Optional.empty());
        when(juradoRepo.findBySolicitudId(7L)).thenReturn(List.of());
        when(javaEvaluacionJuradoRepo.findBySolicitudId(7L)).thenReturn(List.of());
        when(evaluacionFinalRepo.findBySolicitudId(7L)).thenReturn(Optional.empty());

        ObservacionesSolicitudDTO dto = service.obtenerObservacionesSolicitud(7L);

        assertEquals("", dto.getNombreEstudiante());
        assertNull(dto.getTutor());
        assertTrue(dto.getJurados().isEmpty());
        assertNull(dto.getCoordinador());
    }

    @Test
    void obtenerObservacionesSolicitudArmaElReporteCompleto() {
        Usuario usuarioEstudiante = Usuario.builder().id(1L).nombre("Ana").apellido("Torres").build();
        Estudiante estudiante = Estudiante.builder().id(1L).usuario(usuarioEstudiante).build();
        Solicitud solicitud = Solicitud.builder().id(7L).tituloTema("Tema X").estudiante(estudiante).build();

        Usuario usuarioTutor = Usuario.builder().id(60L).nombre("Luis").apellido("Mora").build();
        Docente docenteTutor = Docente.builder().id(5L).usuario(usuarioTutor).build();
        Tutor tutor = Tutor.builder().id(9L).docente(docenteTutor).observaciones("Buen avance").build();

        Usuario usuarioJurado = Usuario.builder().id(50L).nombre("Carla").apellido("Zamora").build();
        Docente docenteJurado = Docente.builder().id(1L).usuario(usuarioJurado).build();
        Jurado jurado = juradoDeSolicitud(solicitud, docenteJurado);
        EvaluacionJurado evalJurado = EvaluacionJurado.builder().id(3L).jurado(jurado)
                .notaJurado(85.0).observaciones("Bien").resultado("APROBADO")
                .comentarioPreestablecido("Cumple").build();

        CriterioRubrica criterio = CriterioRubrica.builder().id(1L).nombre("Claridad").ponderacion(100.0).build();
        Evaluador evaluador = Evaluador.builder().id(20L).build();
        EvaluacionCriterio ec = EvaluacionCriterio.builder().id(1L).criterio(criterio).escala(90)
                .notaObtenida(90.0).observacionAuto("Excelente").build();

        EvaluacionFinal evaluacionFinal = EvaluacionFinal.builder().id(1L)
                .observaciones("Todo bien").notaInstructor(88.0).notaFinal(89.0)
                .resultado(ResultadoEvaluacion.builder().codigo("APROBADO").nombre("Aprobado").build())
                .build();

        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(tutorRepo.findBySolicitudId(7L)).thenReturn(Optional.of(tutor));
        when(juradoRepo.findBySolicitudId(7L)).thenReturn(List.of(jurado));
        when(javaEvaluacionJuradoRepo.findBySolicitudId(7L)).thenReturn(List.of(evalJurado));
        when(evaluadorRepo.findBySolicitudIdAndDocenteIdAndTipoEvaluadorCodigo(7L, 1L, "JURADO"))
                .thenReturn(Optional.of(evaluador));
        when(evalCriterioRepo.findBySolicitudIdAndEvaluadorId(7L, 20L)).thenReturn(List.of(ec));
        when(evaluacionFinalRepo.findBySolicitudId(7L)).thenReturn(Optional.of(evaluacionFinal));

        ObservacionesSolicitudDTO dto = service.obtenerObservacionesSolicitud(7L);

        assertEquals("Ana Torres", dto.getNombreEstudiante());
        assertEquals("Luis Mora", dto.getTutor().getNombreTutor());
        assertEquals("Buen avance", dto.getTutor().getObservaciones());
        assertEquals(1, dto.getJurados().size());
        assertEquals("Carla Zamora", dto.getJurados().get(0).getNombreJurado());
        assertEquals(85.0, dto.getJurados().get(0).getNotaJurado());
        assertEquals(1, dto.getJurados().get(0).getCriterios().size());
        assertEquals("Aprobado", dto.getCoordinador().getResultado());
        assertEquals(89.0, dto.getCoordinador().getNotaFinal());
    }
}
