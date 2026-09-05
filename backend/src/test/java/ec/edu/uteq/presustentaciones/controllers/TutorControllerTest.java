package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.MiEstudianteTutoradoDTO;
import ec.edu.uteq.presustentaciones.entities.Tutor;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.TutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TutorController expone sp_obtener_estadisticas_tutores y tenía 4 de 21 líneas
 * cubiertas. Se cubre además el caso en que el usuario autenticado no existe en la
 * base (token válido de un usuario ya borrado), que hoy revienta con RuntimeException
 * y conviene dejar fijado como comportamiento conocido.
 */
@ExtendWith(MockitoExtension.class)
class TutorControllerTest {

    @Mock private TutorService tutorService;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private TutorController controller;

    @BeforeEach
    void autenticarDocente() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("docente@uteq.edu.ec", null, List.of()));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void misEstudiantesResuelveElDocenteAutenticadoAntesDeConsultar() {
        Usuario docente = Usuario.builder().id(50L).email("docente@uteq.edu.ec").build();
        List<MiEstudianteTutoradoDTO> roster = List.of();
        when(usuarioRepository.findByEmail("docente@uteq.edu.ec")).thenReturn(Optional.of(docente));
        when(tutorService.misEstudiantes(50L)).thenReturn(roster);

        ResponseEntity<List<MiEstudianteTutoradoDTO>> response = controller.misEstudiantes();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(roster, response.getBody());
    }

    @Test
    void misEstudiantesFallaSiElUsuarioDelTokenYaNoExiste() {
        when(usuarioRepository.findByEmail("docente@uteq.edu.ec")).thenReturn(Optional.empty());

        RuntimeException error = assertThrows(RuntimeException.class, () -> controller.misEstudiantes());

        assertEquals("Usuario autenticado no encontrado", error.getMessage());
        verify(tutorService, never()).misEstudiantes(any());
    }

    @Test
    void asignarDevuelveElTutorCreado() {
        Tutor tutor = Tutor.builder().id(1L).build();
        when(tutorService.asignarTutor(1L, 2L)).thenReturn(tutor);

        ResponseEntity<Tutor> response = controller.asignar(1L, 2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(tutor, response.getBody());
    }

    @Test
    void asignarDevuelve400SinCuerpoCuandoElServicioRechaza() {
        when(tutorService.asignarTutor(1L, 2L)).thenThrow(new RuntimeException("La solicitud ya tiene tutor"));

        ResponseEntity<Tutor> response = controller.asignar(1L, 2L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void porSolicitudDevuelve404CuandoNoHayTutorAsignado() {
        when(tutorService.buscarPorSolicitud(1L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, controller.porSolicitud(1L).getStatusCode());
    }

    @Test
    void porSolicitudDevuelveElTutorCuandoExiste() {
        Tutor tutor = Tutor.builder().id(1L).build();
        when(tutorService.buscarPorSolicitud(1L)).thenReturn(Optional.of(tutor));

        assertSame(tutor, controller.porSolicitud(1L).getBody());
    }

    @Test
    void listarPropagaLaPaginacionRecibida() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Tutor> pagina = new PageImpl<>(List.of(Tutor.builder().id(1L).build()));
        when(tutorService.listarTodos(pageable)).thenReturn(pagina);

        assertSame(pagina, controller.listar(pageable).getBody());
    }

    @Test
    void eliminarDevuelve204() {
        assertEquals(HttpStatus.NO_CONTENT, controller.eliminar(3L).getStatusCode());
        verify(tutorService).eliminarTutor(3L);
    }

    // ── sp_obtener_estadisticas_tutores ───────────────────────────────────────

    @Test
    void estadisticasDevuelveLasFilasDelProcedimientoAlmacenado() {
        List<Map<String, Object>> stats = List.of(Map.of(
                "tutorDocenteId", 1L, "tutorNombre", "Ana Pérez",
                "tutoriasActivas", 3L, "tutoriasCompletadas", 5L, "totalFasesAprobadas", 12L));
        when(tutorService.obtenerEstadisticasTutoresSP()).thenReturn(stats);

        ResponseEntity<?> response = controller.estadisticas();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(stats, response.getBody());
    }

    @Test
    void estadisticasTraduceElErrorDelProcedimientoA400() {
        when(tutorService.obtenerEstadisticasTutoresSP())
                .thenThrow(new RuntimeException("function presus.sp_obtener_estadisticas_tutores() does not exist"));

        ResponseEntity<?> response = controller.estadisticas();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) response.getBody();
        assertTrue(error.get("error").contains("sp_obtener_estadisticas_tutores"));
    }
}
