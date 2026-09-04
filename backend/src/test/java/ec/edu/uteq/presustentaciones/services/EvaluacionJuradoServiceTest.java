package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.EvaluacionJuradoDTO;
import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.EvaluacionJuradoRepository;
import ec.edu.uteq.presustentaciones.repositories.JuradoRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EvaluacionJuradoService.guardarEvaluacion() decide APROBADO/REPROBADO (umbral 7) y el
 * comentario preestablecido por rango de nota -- logica real sin ningun test dedicado.
 */
@ExtendWith(MockitoExtension.class)
class EvaluacionJuradoServiceTest {

    @Mock private EvaluacionJuradoRepository evaluacionJuradoRepo;
    @Mock private SolicitudRepository solicitudRepo;
    @Mock private JuradoRepository juradoRepo;
    @Mock private SolicitudAccessService solicitudAccessService;
    @Mock private PermisoService permisoService;

    @InjectMocks
    private EvaluacionJuradoService evaluacionJuradoService;

    private Solicitud solicitud;
    private Jurado jurado;

    @BeforeEach
    void setUp() {
        solicitud = Solicitud.builder().id(7L).tituloTema("Sistema X").build();
        Usuario usuarioDocente = Usuario.builder().id(50L).nombre("Ana").apellido("Torres").build();
        Docente docente = Docente.builder().id(1L).usuario(usuarioDocente).build();
        jurado = Jurado.builder().id(3L).solicitud(solicitud).docente(docente)
                .rolJurado(RolJurado.builder().codigo("VOCAL_1").build()).build();

        // validarPuedeRegistrar() exige un SecurityContextHolder autenticado (mismo patron que
        // ActaServiceImplTest); se autentica como ADMIN por defecto para bypasear la regla de
        // "debe ser el propio jurado" y mantener estos tests centrados en la logica de negocio
        // (umbral 7, comentario por rango), no en la autorizacion -- que ya se prueba aparte.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@uteq.edu.ec", null,
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void guardarEvaluacionLanzaExcepcionSiLaSolicitudNoExiste() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> evaluacionJuradoService.guardarEvaluacion(7L, 3L, 8.0, "bien"));
    }

    @Test
    void guardarEvaluacionLanzaExcepcionSiElJuradoNoExiste() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> evaluacionJuradoService.guardarEvaluacion(7L, 3L, 8.0, "bien"));
    }

    @Test
    void guardarEvaluacionRechazaJuradoQueNoPerteneceALaSolicitud() {
        Solicitud otraSolicitud = Solicitud.builder().id(999L).build();
        Jurado juradoDeOtraSolicitud = Jurado.builder().id(3L).solicitud(otraSolicitud).build();
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(juradoDeOtraSolicitud));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> evaluacionJuradoService.guardarEvaluacion(7L, 3L, 8.0, "bien"));
        assertTrue(ex.getMessage().contains("no pertenece"));
    }

    @Test
    void guardarEvaluacionRechazaNotaFueraDeRango() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));

        assertThrows(RuntimeException.class,
                () -> evaluacionJuradoService.guardarEvaluacion(7L, 3L, 0.5, "bien"));
        assertThrows(RuntimeException.class,
                () -> evaluacionJuradoService.guardarEvaluacion(7L, 3L, 10.5, "bien"));
    }

    @Test
    void guardarEvaluacionConNotaMayorOIgualA7ResultaAprobado() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        when(evaluacionJuradoRepo.findBySolicitudIdAndJuradoId(7L, 3L)).thenReturn(Optional.empty());
        when(evaluacionJuradoRepo.save(any(EvaluacionJurado.class))).thenAnswer(inv -> inv.getArgument(0));

        EvaluacionJuradoDTO dto = evaluacionJuradoService.guardarEvaluacion(7L, 3L, 7.0, "Buen trabajo");

        assertEquals("APROBADO", dto.getResultado());
        assertTrue(dto.getComentarioPreestablecido().contains("cumple satisfactoriamente"));
        assertEquals("Ana Torres", dto.getNombreJurado());
        assertEquals("VOCAL_1", dto.getRolJurado());
    }

    @Test
    void guardarEvaluacionConNotaMenorA7ResultaReprobado() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        when(evaluacionJuradoRepo.findBySolicitudIdAndJuradoId(7L, 3L)).thenReturn(Optional.empty());
        when(evaluacionJuradoRepo.save(any(EvaluacionJurado.class))).thenAnswer(inv -> inv.getArgument(0));

        EvaluacionJuradoDTO dto = evaluacionJuradoService.guardarEvaluacion(7L, 3L, 6.0, "Falta profundidad");

        assertEquals("REPROBADO", dto.getResultado());
        assertTrue(dto.getComentarioPreestablecido().contains("aspectos que requieren mejoras"));
    }

    @Test
    void guardarEvaluacionConNotaMuyBajaUsaElComentarioMasSevero() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        when(evaluacionJuradoRepo.findBySolicitudIdAndJuradoId(7L, 3L)).thenReturn(Optional.empty());
        when(evaluacionJuradoRepo.save(any(EvaluacionJurado.class))).thenAnswer(inv -> inv.getArgument(0));

        EvaluacionJuradoDTO dto = evaluacionJuradoService.guardarEvaluacion(7L, 3L, 2.0, "Insuficiente");

        assertEquals("REPROBADO", dto.getResultado());
        assertTrue(dto.getComentarioPreestablecido().contains("falencias significativas"));
    }

    @Test
    void guardarEvaluacionActualizaLaEvaluacionExistenteEnVezDeCrearOtra() {
        EvaluacionJurado existente = EvaluacionJurado.builder().id(1L).solicitud(solicitud).jurado(jurado)
                .notaJurado(5.0).resultado("REPROBADO").build();
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        when(evaluacionJuradoRepo.findBySolicitudIdAndJuradoId(7L, 3L)).thenReturn(Optional.of(existente));
        when(evaluacionJuradoRepo.save(any(EvaluacionJurado.class))).thenAnswer(inv -> inv.getArgument(0));

        EvaluacionJuradoDTO dto = evaluacionJuradoService.guardarEvaluacion(7L, 3L, 9.0, "Corregido, ahora excelente");

        assertEquals(1L, dto.getId());
        assertEquals("APROBADO", dto.getResultado());
        assertEquals("Corregido, ahora excelente", dto.getObservaciones());
        verify(evaluacionJuradoRepo).save(existente);
    }

    @Test
    void guardarEvaluacionPermiteAlPropioJuradoRegistrarSuNota() {
        // Caso permitido: el docente autenticado ES el jurado asignado a esta solicitud.
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));
        when(evaluacionJuradoRepo.findBySolicitudIdAndJuradoId(7L, 3L)).thenReturn(Optional.empty());
        when(evaluacionJuradoRepo.save(any(EvaluacionJurado.class))).thenAnswer(inv -> inv.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ana.torres@uteq.edu.ec", null,
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_DOCENTE")));
        when(permisoService.esPropioDocente(any(), eq(1L))).thenReturn(true);

        assertDoesNotThrow(() -> evaluacionJuradoService.guardarEvaluacion(7L, 3L, 8.0, "bien"));
    }

    @Test
    void guardarEvaluacionRechazaAJuradoQueRegistraANombreDeOtro() {
        // Caso IDOR de escritura: un docente que NO es el jurado asignado intenta registrar
        // la nota a nombre de otro cambiando el juradoId.
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(juradoRepo.findById(3L)).thenReturn(Optional.of(jurado));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otro.docente@uteq.edu.ec", null,
                        org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_DOCENTE")));
        when(permisoService.esPropioDocente(any(), eq(1L))).thenReturn(false);
        when(permisoService.tienePermiso(any(), eq("EVALUACION_CALIFICAR"))).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> evaluacionJuradoService.guardarEvaluacion(7L, 3L, 8.0, "bien"));
    }

    @Test
    void obtenerEvaluacionRetornaNullSiNoExiste() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        when(evaluacionJuradoRepo.findBySolicitudIdAndJuradoId(7L, 3L)).thenReturn(Optional.empty());
        assertNull(evaluacionJuradoService.obtenerEvaluacion(7L, 3L));
    }

    @Test
    void obtenerEvaluacionPropagaAccessDeniedSiSolicitudAccessServiceLoRechaza() {
        // Caso IDOR: SolicitudAccessService es quien decide; aquí solo verificamos que
        // EvaluacionJuradoService no atrapa/oculta ese rechazo (debe seguir siendo 403).
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        org.mockito.Mockito.doThrow(new AccessDeniedException("No tienes permiso para acceder a la información de esta solicitud"))
                .when(solicitudAccessService).validarAcceso(solicitud, "EVALUACION_CALIFICAR");

        assertThrows(AccessDeniedException.class, () -> evaluacionJuradoService.obtenerEvaluacion(7L, 3L));
    }

    @Test
    void obtenerTribunalMapeaTodasLasEvaluacionesDeLaSolicitud() {
        when(solicitudRepo.findById(7L)).thenReturn(Optional.of(solicitud));
        EvaluacionJurado eval1 = EvaluacionJurado.builder().id(1L).solicitud(solicitud).jurado(jurado)
                .notaJurado(8.0).resultado("APROBADO").build();
        when(evaluacionJuradoRepo.findBySolicitudId(7L)).thenReturn(List.of(eval1));

        List<EvaluacionJuradoDTO> resultado = evaluacionJuradoService.obtenerTribunal(7L);

        assertEquals(1, resultado.size());
        assertEquals(8.0, resultado.get(0).getNotaJurado());
    }
}
