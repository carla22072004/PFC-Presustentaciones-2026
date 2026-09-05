package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.NuevoMensajeRequest;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import ec.edu.uteq.presustentaciones.dto.TutoriaFaseDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaMensajeDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaResumenDTO;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.TutoriaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TutoriaController expone sp_registrar_tutoria_avance y tenía 4 de 83 líneas cubiertas.
 *
 * Lo más relevante de esta clase no son los delegados sino resolverUsuarioId(): un
 * estudiante o docente sólo puede consultar sus propias tutorías aunque mande otro
 * usuarioId en la URL, y sólo ADMIN/COORDINADOR pueden consultar las de un tercero.
 * Esa es una comprobación de propiedad del recurso equivalente a las que se corrigieron
 * como IDOR en otros controladores, y no tenía ninguna prueba que la fijara.
 */
@ExtendWith(MockitoExtension.class)
class TutoriaControllerTest {

    @Mock private TutoriaService tutoriaService;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private TutoriaController controller;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private ResponseWrapper<Object> wrapperDe(ResponseEntity<?> response) {
        return (ResponseWrapper<Object>) response.getBody();
    }

    /** Autentica un usuario con el rol dado y lo deja resoluble por email. */
    private Usuario autenticar(Long id, String rol) {
        String email = "usuario" + id + "@uteq.edu.ec";
        Usuario usuario = Usuario.builder().id(id).email(email).rol(rol).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
        lenient().when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        return usuario;
    }

    // ── resolverUsuarioId: propiedad del recurso ──────────────────────────────

    @Test
    void unEstudianteNoPuedeConsultarLasTutoriasDeOtroUsuarioAunqueLoPidaEnLaUrl() {
        autenticar(50L, "ESTUDIANTE");
        when(tutoriaService.obtenerTutoriasEstudiante(50L)).thenReturn(List.of());

        // Pide explícitamente el usuario 99, pero el controlador ignora ese id
        controller.obtenerTutoriasEstudiante(99L);

        verify(tutoriaService).obtenerTutoriasEstudiante(50L);
        verify(tutoriaService, never()).obtenerTutoriasEstudiante(99L);
    }

    @Test
    void unCoordinadorSiPuedeConsultarLasTutoriasDeOtroUsuario() {
        autenticar(1L, "COORDINADOR");
        when(tutoriaService.obtenerTutoriasDocente(99L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.obtenerTutoriasDocente(99L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tutoriaService).obtenerTutoriasDocente(99L);
    }

    @Test
    void unAdminSinUsuarioIdExplicitoConsultaLasSuyas() {
        autenticar(1L, "ADMIN");
        when(tutoriaService.obtenerResumen(5L, 1L)).thenReturn(mock(TutoriaResumenDTO.class));

        assertEquals(HttpStatus.OK, controller.obtenerResumen(5L, null).getStatusCode());
        verify(tutoriaService).obtenerResumen(5L, 1L);
    }

    @Test
    void sinAutenticacionElEndpointDevuelve400EnVezDeReventar() {
        ResponseEntity<?> response = controller.obtenerTutoriasEstudiante(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Usuario no autenticado", wrapperDe(response).getMessage());
        verifyNoInteractions(tutoriaService);
    }

    @Test
    void conUsuarioAnonimoElEndpointDevuelve400() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        ResponseEntity<?> response = controller.obtenerTutoriasDocente(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Usuario no autenticado", wrapperDe(response).getMessage());
    }

    @Test
    void conTokenDeUsuarioYaBorradoElEndpointDevuelve400() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("fantasma@uteq.edu.ec", null, List.of()));
        when(usuarioRepository.findByEmail("fantasma@uteq.edu.ec")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.obtenerFases(1L, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Usuario autenticado no encontrado en el sistema", wrapperDe(response).getMessage());
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Test
    void obtenerFasesDevuelveLasFasesDelServicio() {
        autenticar(50L, "ESTUDIANTE");
        List<TutoriaFaseDTO> fases = List.of(mock(TutoriaFaseDTO.class));
        when(tutoriaService.obtenerFases(5L, 50L)).thenReturn(fases);

        ResponseEntity<?> response = controller.obtenerFases(5L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(fases, wrapperDe(response).getData());
    }

    @Test
    void obtenerResumenTraduceElErrorDelServicioA400() {
        autenticar(50L, "ESTUDIANTE");
        when(tutoriaService.obtenerResumen(5L, 50L))
                .thenThrow(new RuntimeException("No tienes acceso a esta tutoría"));

        ResponseEntity<?> response = controller.obtenerResumen(5L, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No tienes acceso a esta tutoría", wrapperDe(response).getMessage());
    }

    // ── Operaciones sobre fases ───────────────────────────────────────────────

    @Test
    void crearFaseUsaSiempreElUsuarioAutenticadoNoElDelParametro() {
        autenticar(60L, "DOCENTE");
        TutoriaFaseDTO fase = mock(TutoriaFaseDTO.class);
        when(tutoriaService.crearFaseConObservacion(5L, 60L, "Revisar capítulo 2")).thenReturn(fase);

        ResponseEntity<?> response = controller.crearFaseConObservacion(5L, "Revisar capítulo 2", 999L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(fase, wrapperDe(response).getData());
        verify(tutoriaService).crearFaseConObservacion(5L, 60L, "Revisar capítulo 2");
    }

    @Test
    void subirPdfCorregidoDelegaConElEstudianteAutenticado() {
        autenticar(50L, "ESTUDIANTE");
        MultipartFile archivo = new MockMultipartFile("archivo", "cap2.pdf",
                MediaType.APPLICATION_PDF_VALUE, "contenido".getBytes());
        TutoriaFaseDTO fase = mock(TutoriaFaseDTO.class);
        when(tutoriaService.subirPdfCorregido(7L, archivo, 50L)).thenReturn(fase);

        ResponseEntity<?> response = controller.subirPdfCorregido(7L, archivo, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(fase, wrapperDe(response).getData());
    }

    @Test
    void subirPdfCorregidoTraduceElErrorDelServicioA400() {
        autenticar(50L, "ESTUDIANTE");
        MultipartFile archivo = new MockMultipartFile("archivo", "malo.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{1});
        when(tutoriaService.subirPdfCorregido(7L, archivo, 50L))
                .thenThrow(new RuntimeException("Solo se admiten archivos PDF"));

        ResponseEntity<?> response = controller.subirPdfCorregido(7L, archivo, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Solo se admiten archivos PDF", wrapperDe(response).getMessage());
    }

    @Test
    void aprobarFaseDelegaConElTutorAutenticado() {
        autenticar(60L, "DOCENTE");
        TutoriaFaseDTO fase = mock(TutoriaFaseDTO.class);
        when(tutoriaService.aprobarFase(7L, 60L, "Buen avance")).thenReturn(fase);

        ResponseEntity<?> response = controller.aprobarFase(7L, null, "Buen avance");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(fase, wrapperDe(response).getData());
    }

    @Test
    void enviarMensajeUsaElRemitenteAutenticadoYElCuerpoDelRequest() {
        autenticar(50L, "ESTUDIANTE");
        NuevoMensajeRequest request = new NuevoMensajeRequest();
        request.setContenido("¿Puede revisar el capítulo 3?");
        request.setTipo("CONSULTA");
        TutoriaMensajeDTO mensaje = mock(TutoriaMensajeDTO.class);
        when(tutoriaService.enviarMensaje(7L, 50L, "¿Puede revisar el capítulo 3?", "CONSULTA"))
                .thenReturn(mensaje);

        ResponseEntity<?> response = controller.enviarMensaje(7L, 999L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(mensaje, wrapperDe(response).getData());
        verify(tutoriaService).enviarMensaje(7L, 50L, "¿Puede revisar el capítulo 3?", "CONSULTA");
    }

    @Test
    void marcarMensajesLeidosDevuelveOkSinCuerpoDeDatos() {
        autenticar(50L, "ESTUDIANTE");

        ResponseEntity<?> response = controller.marcarMensajesLeidos(7L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(wrapperDe(response).getData());
        verify(tutoriaService).marcarMensajesLeidos(7L, 50L);
    }

    @Test
    void marcarMensajesLeidosTraduceElErrorDelServicioA400() {
        autenticar(50L, "ESTUDIANTE");
        doThrow(new RuntimeException("Fase inexistente"))
                .when(tutoriaService).marcarMensajesLeidos(7L, 50L);

        ResponseEntity<?> response = controller.marcarMensajesLeidos(7L, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Fase inexistente", wrapperDe(response).getMessage());
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    @Test
    void obtenerPdfFaseDevuelveElRecursoConCabeceraInline() {
        autenticar(50L, "ESTUDIANTE");
        Resource recurso = new ByteArrayResource("%PDF-1.4".getBytes()) {
            @Override public String getFilename() { return "fase-1.pdf"; }
        };
        when(tutoriaService.obtenerPdfFase(7L, 50L)).thenReturn(recurso);

        ResponseEntity<?> response = controller.obtenerPdfFase(7L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("fase-1.pdf"));
        assertSame(recurso, response.getBody());
    }

    @Test
    void obtenerPdfFaseTraduceElErrorDelServicioA400() {
        autenticar(50L, "ESTUDIANTE");
        when(tutoriaService.obtenerPdfFase(7L, 50L))
                .thenThrow(new RuntimeException("La fase no tiene PDF cargado"));

        ResponseEntity<?> response = controller.obtenerPdfFase(7L, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("La fase no tiene PDF cargado", wrapperDe(response).getMessage());
    }

    // ── sp_registrar_tutoria_avance ───────────────────────────────────────────

    @Test
    void registrarAvanceConvierteElTamanoNumericoYLlamaAlProcedimiento() {
        autenticar(50L, "ESTUDIANTE");

        ResponseEntity<?> response = controller.registrarAvanceSP(5L, Map.of(
                "numeroFase", 2,
                "archivoPdf", "capitulo2.pdf",
                "tamanoBytes", 12345,   // Jackson lo entrega como Integer, el SP espera Long
                "sha256", "abc123"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tutoriaService).registrarAvanceSP(5L, 2, "capitulo2.pdf", 12345L, "abc123", 50L);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) wrapperDe(response).getData();
        assertEquals(5L, data.get("tutorId"));
        assertEquals(2, data.get("numeroFase"));
    }

    @Test
    void registrarAvanceSinTamanoNiSha256PasaNullsAlProcedimiento() {
        autenticar(50L, "ESTUDIANTE");

        ResponseEntity<?> response = controller.registrarAvanceSP(5L, Map.of(
                "numeroFase", 1, "archivoPdf", "capitulo1.pdf"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tutoriaService).registrarAvanceSP(5L, 1, "capitulo1.pdf", null, null, 50L);
    }

    @Test
    void registrarAvanceRechazaElCuerpoIncompletoSinLlamarAlProcedimiento() {
        ResponseEntity<?> response = controller.registrarAvanceSP(5L, Map.of("numeroFase", 1));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Se requieren 'numeroFase' y 'archivoPdf'", wrapperDe(response).getMessage());
        verify(tutoriaService, never()).registrarAvanceSP(any(), any(), any(), any(), any(), any());
    }

    @Test
    void registrarAvanceTraduceElErrorDelProcedimientoA400() {
        autenticar(50L, "ESTUDIANTE");
        doThrow(new RuntimeException("No se puede registrar la fase 3, la fase 2 debe estar APROBADA"))
                .when(tutoriaService).registrarAvanceSP(5L, 3, "capitulo3.pdf", null, null, 50L);

        ResponseEntity<?> response = controller.registrarAvanceSP(5L, Map.of(
                "numeroFase", 3, "archivoPdf", "capitulo3.pdf"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(wrapperDe(response).getMessage().contains("la fase 2 debe estar APROBADA"));
    }
}
