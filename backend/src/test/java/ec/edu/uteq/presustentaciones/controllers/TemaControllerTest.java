package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.GenerarTemaRequest;
import ec.edu.uteq.presustentaciones.dto.TemaPropuestoDTO;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.TemaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TemaControllerTest {

    @Mock private TemaService temaService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;

    @InjectMocks private TemaController temaController;

    private TemaPropuestoDTO temaMock;

    @BeforeEach
    void setUp() {
        temaMock = TemaPropuestoDTO.builder().id(1).titulo("Tema Prueba").build();

        Usuario usuario = new Usuario();
        usuario.setId(50L);
        usuario.setEmail("est@uteq.edu.ec");
        Estudiante estudiante = new Estudiante();
        estudiante.setId(7L);

        when(usuarioRepository.findByEmail("est@uteq.edu.ec")).thenReturn(Optional.of(usuario));
        when(estudianteRepository.findByUsuarioId(50L)).thenReturn(Optional.of(estudiante));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("est@uteq.edu.ec", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void explorarPasaElIdDelEstudianteAutenticado() {
        when(temaService.explorar(eq(1), eq(2), eq(3), eq("BASICO"), eq(7L)))
                .thenReturn(Collections.singletonList(temaMock));

        ResponseEntity<List<TemaPropuestoDTO>> response =
                temaController.explorar(1, 2, 3, "BASICO");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(temaService).explorar(1, 2, 3, "BASICO", 7L);
    }

    @Test
    void detalleDelegaEnElServicio() {
        when(temaService.obtenerDetalle(1)).thenReturn(temaMock);

        ResponseEntity<TemaPropuestoDTO> response = temaController.detalle(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Tema Prueba", response.getBody().getTitulo());
    }

    @Test
    void generarIdeasDevuelveLaLista() {
        GenerarTemaRequest request = new GenerarTemaRequest();
        request.setCarreraId(1);
        when(temaService.generarIdeas(any(GenerarTemaRequest.class)))
                .thenReturn(Collections.singletonList(temaMock));

        ResponseEntity<List<TemaPropuestoDTO>> response = temaController.generarIdeas(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void misTemasGuardadosUsaElEstudianteAutenticado() {
        when(temaService.obtenerTemasGuardados(7L)).thenReturn(Collections.singletonList(temaMock));

        ResponseEntity<List<TemaPropuestoDTO>> response = temaController.misTemasGuardados();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(temaService).obtenerTemasGuardados(7L);
    }

    @Test
    void guardarDevuelve201YResuelveElEstudianteDelToken() {
        ResponseEntity<Void> response = temaController.guardar(9);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(temaService).guardarTemaEstudiante(7L, 9);
    }

    @Test
    void quitarGuardadoDevuelve204() {
        ResponseEntity<Void> response = temaController.quitarGuardado(9);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(temaService).quitarTemaGuardado(7L, 9);
    }

    @Test
    void guardarFallaSiElUsuarioNoTienePerfilDeEstudiante() {
        when(estudianteRepository.findByUsuarioId(50L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> temaController.guardar(9));
        verify(temaService, never()).guardarTemaEstudiante(anyLong(), anyInt());
    }

    @Test
    void crearDelegaYDevuelve201() {
        var req = new ec.edu.uteq.presustentaciones.dto.GuardarTemaPropuestoRequest();
        req.setTitulo("Tema");
        when(temaService.crear(req)).thenReturn(temaMock);

        ResponseEntity<TemaPropuestoDTO> r = temaController.crear(req);

        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        verify(temaService).crear(req);
    }

    @Test
    void actualizarDelega() {
        var req = new ec.edu.uteq.presustentaciones.dto.GuardarTemaPropuestoRequest();
        req.setTitulo("Tema");
        when(temaService.actualizar(3, req)).thenReturn(temaMock);

        ResponseEntity<TemaPropuestoDTO> r = temaController.actualizar(3, req);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        verify(temaService).actualizar(3, req);
    }

    @Test
    void eliminarDelegaYDevuelve204() {
        ResponseEntity<Void> r = temaController.eliminar(3);

        assertEquals(HttpStatus.NO_CONTENT, r.getStatusCode());
        verify(temaService).eliminar(3);
    }
}
