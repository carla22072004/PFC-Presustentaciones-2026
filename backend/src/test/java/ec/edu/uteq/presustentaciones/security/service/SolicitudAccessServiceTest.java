package ec.edu.uteq.presustentaciones.security.service;

import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.JuradoRepository;
import ec.edu.uteq.presustentaciones.repositories.TutorRepository;
import ec.edu.uteq.presustentaciones.services.PermisoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Corrección de IDOR (auditoría 2026-09-04): Evaluación, EvaluaciónJurado, RúbricaEvaluación y
 * Anteproyecto delegan aquí la comprobación de "¿este usuario tiene relación real con esta
 * solicitud?" antes de exponer datos académicos. Cubre exactamente los casos mínimos pedidos:
 * permitido (estudiante/jurado/tutor propios), IDOR (usuario sin relación), administrativo
 * (ADMIN y el permiso de bypass indicado).
 */
@ExtendWith(MockitoExtension.class)
class SolicitudAccessServiceTest {

    @Mock private JuradoRepository juradoRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private PermisoService permisoService;

    @InjectMocks
    private SolicitudAccessService solicitudAccessService;

    private Solicitud solicitud;

    @BeforeEach
    void setUp() {
        Usuario usuarioEstudiante = Usuario.builder().id(10L).email("estudiante.dueno@uteq.edu.ec").build();
        Estudiante estudiante = Estudiante.builder().id(3L).usuario(usuarioEstudiante).build();
        solicitud = Solicitud.builder().id(7L).estudiante(estudiante).tituloTema("Sistema X").build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String email, String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null,
                        AuthorityUtils.createAuthorityList(roles)));
    }

    @Test
    void sinAutenticarLanzaAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> solicitudAccessService.validarAcceso(solicitud, "EVALUACION_CALIFICAR"));
    }

    @Test
    void adminSiempreTieneAcceso() {
        autenticarComo("admin@uteq.edu.ec", "ROLE_ADMIN");
        assertDoesNotThrow(() -> solicitudAccessService.validarAcceso(solicitud, "EVALUACION_CALIFICAR"));
    }

    @Test
    void titularDelPermisoDeBypassTieneAcceso() {
        autenticarComo("coordinador@uteq.edu.ec", "ROLE_COORDINADOR");
        when(permisoService.tienePermiso(any(), eq("EVALUACION_CALIFICAR"))).thenReturn(true);
        assertDoesNotThrow(() -> solicitudAccessService.validarAcceso(solicitud, "EVALUACION_CALIFICAR"));
    }

    @Test
    void estudiantePropietarioTieneAcceso() {
        // El chequeo de "estudiante dueño" resuelve el acceso antes de consultar jurados/tutor,
        // así que aquí solo hace falta stubear el permiso de bypass (no se cumple).
        autenticarComo("estudiante.dueno@uteq.edu.ec", "ROLE_ESTUDIANTE");
        when(permisoService.tienePermiso(any(), any())).thenReturn(false);

        assertDoesNotThrow(() -> solicitudAccessService.validarAcceso(solicitud, "EVALUACION_CALIFICAR"));
    }

    @Test
    void otroEstudianteSinRelacionRecibeAccessDenied() {
        // Caso IDOR: un usuario autenticado que no es el estudiante dueño, ni jurado, ni tutor.
        autenticarComo("otro.estudiante@uteq.edu.ec", "ROLE_ESTUDIANTE");
        when(permisoService.tienePermiso(any(), any())).thenReturn(false);
        when(juradoRepository.findBySolicitudId(7L)).thenReturn(List.of());
        when(tutorRepository.findBySolicitudId(7L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> solicitudAccessService.validarAcceso(solicitud, "EVALUACION_CALIFICAR"));
    }

    @Test
    void juradoAsignadoTieneAcceso() {
        Usuario usuarioDocente = Usuario.builder().id(50L).email("jurado@uteq.edu.ec").build();
        Docente docente = Docente.builder().id(5L).usuario(usuarioDocente).build();
        Jurado jurado = Jurado.builder().id(1L).solicitud(solicitud).docente(docente).build();

        autenticarComo("jurado@uteq.edu.ec", "ROLE_DOCENTE");
        when(permisoService.tienePermiso(any(), any())).thenReturn(false);
        when(juradoRepository.findBySolicitudId(7L)).thenReturn(List.of(jurado));

        assertDoesNotThrow(() -> solicitudAccessService.validarAcceso(solicitud, "EVALUACION_CALIFICAR"));
    }

    @Test
    void docenteQueNoEsJuradoNiTutorRecibeAccessDenied() {
        // Caso IDOR: un DOCENTE autenticado que no participa en esta solicitud como jurado/tutor.
        autenticarComo("docente.ajeno@uteq.edu.ec", "ROLE_DOCENTE");
        when(permisoService.tienePermiso(any(), any())).thenReturn(false);
        when(juradoRepository.findBySolicitudId(7L)).thenReturn(List.of());
        when(tutorRepository.findBySolicitudId(7L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> solicitudAccessService.validarAcceso(solicitud, "EVALUACION_CALIFICAR"));
    }

    @Test
    void tutorAsignadoTieneAcceso() {
        Usuario usuarioDocente = Usuario.builder().id(60L).email("tutor@uteq.edu.ec").build();
        Docente docente = Docente.builder().id(6L).usuario(usuarioDocente).build();
        Tutor tutor = Tutor.builder().id(2L).solicitud(solicitud).docente(docente).build();

        autenticarComo("tutor@uteq.edu.ec", "ROLE_DOCENTE");
        when(permisoService.tienePermiso(any(), any())).thenReturn(false);
        when(juradoRepository.findBySolicitudId(7L)).thenReturn(List.of());
        when(tutorRepository.findBySolicitudId(7L)).thenReturn(Optional.of(tutor));

        assertDoesNotThrow(() -> solicitudAccessService.validarAcceso(solicitud, "ANTEPROYECTO_REVISAR"));
    }
}
