package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import ec.edu.uteq.presustentaciones.dto.SeguimientoDTO;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.SolicitudService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SolicitudController es la puerta de entrada del flujo académico completo y expone
 * sp_generar_reporte_defensas; tenía 5 de 81 líneas cubiertas.
 *
 * El foco está en validarAccesoSolicitud(): un estudiante sólo puede abrir, enviar y
 * seguir SU propia solicitud, mientras que quien tiene SOLICITUDES_REVISAR (o es ADMIN)
 * puede abrir cualquiera. Es la misma clase de comprobación de propiedad que se corrigió
 * como IDOR en Evaluaciones y Anteproyectos, y aquí no tenía prueba que la fijara.
 */
@ExtendWith(MockitoExtension.class)
class SolicitudControllerTest {

    @Mock private SolicitudService solicitudService;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private SolicitudController controller;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private ResponseWrapper<Object> wrapperDe(ResponseEntity<?> response) {
        return (ResponseWrapper<Object>) response.getBody();
    }

    private void autenticar(String email, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null,
                        List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private Solicitud solicitudDe(String emailPropietario) {
        return Solicitud.builder()
                .id(1L)
                .estudiante(Estudiante.builder().id(7L)
                        .usuario(Usuario.builder().id(50L).email(emailPropietario).build())
                        .build())
                .build();
    }

    // ── Creación ──────────────────────────────────────────────────────────────

    @Test
    void crearDevuelveLaSolicitudCreada() {
        Solicitud datos = Solicitud.builder().tituloTema("Tema").build();
        Solicitud creada = Solicitud.builder().id(1L).build();
        when(solicitudService.crearSolicitud(7L, datos)).thenReturn(creada);

        ResponseEntity<?> response = controller.crear(7L, datos);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(creada, wrapperDe(response).getData());
        assertEquals("Solicitud creada exitosamente", wrapperDe(response).getMessage());
    }

    @Test
    void crearTraduceElErrorDelServicioA400() {
        Solicitud datos = Solicitud.builder().build();
        when(solicitudService.crearSolicitud(7L, datos))
                .thenThrow(new RuntimeException("El estudiante ya tiene una solicitud activa"));

        ResponseEntity<?> response = controller.crear(7L, datos);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("El estudiante ya tiene una solicitud activa", wrapperDe(response).getMessage());
    }

    @Test
    void crearPorUsuarioIgnoraElIdDelPathYUsaElDelToken() {
        autenticar("est@uteq.edu.ec");
        Solicitud datos = Solicitud.builder().build();
        Solicitud creada = Solicitud.builder().id(1L).build();
        when(usuarioRepository.findByEmail("est@uteq.edu.ec"))
                .thenReturn(Optional.of(Usuario.builder().id(50L).build()));
        when(solicitudService.crearSolicitudPorUsuario(50L, datos)).thenReturn(creada);

        // El cliente manda 999 en la URL; el backend debe resolver 50 desde el JWT
        ResponseEntity<?> response = controller.crearPorUsuario(999L, datos);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(solicitudService).crearSolicitudPorUsuario(50L, datos);
        verify(solicitudService, never()).crearSolicitudPorUsuario(eq(999L), any());
    }

    @Test
    void crearPorUsuarioConTokenDeUsuarioInexistenteDevuelve400() {
        autenticar("fantasma@uteq.edu.ec");
        when(usuarioRepository.findByEmail("fantasma@uteq.edu.ec")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.crearPorUsuario(1L, Solicitud.builder().build());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Usuario no encontrado en el sistema", wrapperDe(response).getMessage());
    }

    // ── Listados propios ──────────────────────────────────────────────────────

    @Test
    void misSolicitudesResuelveElUsuarioDesdeElToken() {
        autenticar("est@uteq.edu.ec");
        List<Solicitud> solicitudes = List.of(Solicitud.builder().id(1L).build());
        when(usuarioRepository.findByEmail("est@uteq.edu.ec"))
                .thenReturn(Optional.of(Usuario.builder().id(50L).build()));
        when(solicitudService.listarPorUsuario(50L)).thenReturn(solicitudes);

        ResponseEntity<?> response = controller.listarMisSolicitudes();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(solicitudes, wrapperDe(response).getData());
    }

    @Test
    void misSolicitudesDevuelveListaVaciaEnVezDeErrorSiFallaLaResolucion() {
        autenticar("fantasma@uteq.edu.ec");
        when(usuarioRepository.findByEmail("fantasma@uteq.edu.ec")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.listarMisSolicitudes();

        // Decisión de diseño del controlador: la pantalla del estudiante no debe romperse,
        // muestra una lista vacía en vez de propagar el error.
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), wrapperDe(response).getData());
    }

    @Test
    void listarPorUsuarioDevuelveListaVaciaSiElServicioFalla() {
        when(solicitudService.listarPorUsuario(50L)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.listarPorUsuario(50L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), wrapperDe(response).getData());
    }

    // ── Comprobación de propiedad (validarAccesoSolicitud) ────────────────────

    @Test
    void unEstudianteNoPuedeAbrirLaSolicitudDeOtro() {
        autenticar("otro@uteq.edu.ec");
        when(solicitudService.obtenerPorId(1L)).thenReturn(Optional.of(solicitudDe("dueno@uteq.edu.ec")));

        ResponseEntity<?> response = controller.obtener(1L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Acceso denegado: no eres propietario de esta solicitud",
                wrapperDe(response).getMessage());
    }

    @Test
    void elPropietarioSiPuedeAbrirSuSolicitud() {
        autenticar("dueno@uteq.edu.ec");
        Solicitud propia = solicitudDe("dueno@uteq.edu.ec");
        when(solicitudService.obtenerPorId(1L)).thenReturn(Optional.of(propia));

        ResponseEntity<?> response = controller.obtener(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(propia, wrapperDe(response).getData());
    }

    @Test
    void unRevisorPuedeAbrirCualquierSolicitudSinComprobarPropiedad() {
        autenticar("coord@uteq.edu.ec", "SOLICITUDES_REVISAR");
        Solicitud ajena = solicitudDe("dueno@uteq.edu.ec");
        when(solicitudService.obtenerPorId(1L)).thenReturn(Optional.of(ajena));

        ResponseEntity<?> response = controller.obtener(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(ajena, wrapperDe(response).getData());
    }

    @Test
    void unAdminPuedeAbrirCualquierSolicitud() {
        autenticar("admin@uteq.edu.ec", "ROLE_ADMIN");
        when(solicitudService.obtenerPorId(1L)).thenReturn(Optional.of(solicitudDe("dueno@uteq.edu.ec")));

        assertEquals(HttpStatus.OK, controller.obtener(1L).getStatusCode());
    }

    @Test
    void obtenerDevuelve404CuandoElRevisorPideUnaSolicitudInexistente() {
        autenticar("coord@uteq.edu.ec", "SOLICITUDES_REVISAR");
        when(solicitudService.obtenerPorId(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.obtener(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Solicitud no encontrada", wrapperDe(response).getMessage());
    }

    @Test
    void enviarExigeSerPropietarioAntesDeEnviarARevision() {
        autenticar("otro@uteq.edu.ec");
        when(solicitudService.obtenerPorId(1L)).thenReturn(Optional.of(solicitudDe("dueno@uteq.edu.ec")));

        ResponseEntity<?> response = controller.enviar(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(solicitudService, never()).enviarSolicitud(any());
    }

    @Test
    void enviarFuncionaParaElPropietario() {
        autenticar("dueno@uteq.edu.ec");
        Solicitud enviada = Solicitud.builder().id(1L).build();
        when(solicitudService.obtenerPorId(1L)).thenReturn(Optional.of(solicitudDe("dueno@uteq.edu.ec")));
        when(solicitudService.enviarSolicitud(1L)).thenReturn(enviada);

        ResponseEntity<?> response = controller.enviar(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Solicitud enviada a revisión", wrapperDe(response).getMessage());
    }

    @Test
    void obtenerSeguimientoExigeLaMismaComprobacionDePropiedad() {
        autenticar("otro@uteq.edu.ec");
        when(solicitudService.obtenerPorId(1L)).thenReturn(Optional.of(solicitudDe("dueno@uteq.edu.ec")));

        assertEquals(HttpStatus.FORBIDDEN, controller.obtenerSeguimiento(1L).getStatusCode());
        verify(solicitudService, never()).obtenerSeguimiento(any());
    }

    @Test
    void obtenerSeguimientoDevuelveElHistorialAlPropietario() {
        autenticar("dueno@uteq.edu.ec");
        SeguimientoDTO seguimiento = mock(SeguimientoDTO.class);
        when(solicitudService.obtenerPorId(1L)).thenReturn(Optional.of(solicitudDe("dueno@uteq.edu.ec")));
        when(solicitudService.obtenerSeguimiento(1L)).thenReturn(seguimiento);

        ResponseEntity<?> response = controller.obtenerSeguimiento(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(seguimiento, wrapperDe(response).getData());
    }

    // ── Transiciones de estado (revisor) ──────────────────────────────────────

    @Test
    void aprobarRechazarYRechazarConObservacionDeleganEnElServicio() {
        Solicitud resultado = Solicitud.builder().id(1L).build();
        when(solicitudService.aprobarSolicitud(1L)).thenReturn(resultado);
        when(solicitudService.rechazarSolicitud(2L)).thenReturn(resultado);
        when(solicitudService.rechazarConObservacion(3L, "Falta el anteproyecto")).thenReturn(resultado);

        assertEquals("Solicitud aprobada", wrapperDe(controller.aprobar(1L)).getMessage());
        assertEquals("Solicitud rechazada", wrapperDe(controller.rechazar(2L)).getMessage());
        assertEquals("Solicitud rechazada con observaciones",
                wrapperDe(controller.rechazarConObservacion(3L, Map.of("observacion", "Falta el anteproyecto"))).getMessage());
    }

    @Test
    void rechazarConObservacionSinObservacionUsaCadenaVacia() {
        when(solicitudService.rechazarConObservacion(3L, "")).thenReturn(Solicitud.builder().id(3L).build());

        assertEquals(HttpStatus.OK, controller.rechazarConObservacion(3L, Map.of()).getStatusCode());
        verify(solicitudService).rechazarConObservacion(3L, "");
    }

    @Test
    void aprobarTraduceElErrorDeTransicionInvalidaA400() {
        when(solicitudService.aprobarSolicitud(1L))
                .thenThrow(new RuntimeException("La solicitud no está en estado ENVIADA"));

        ResponseEntity<?> response = controller.aprobar(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("La solicitud no está en estado ENVIADA", wrapperDe(response).getMessage());
    }

    @Test
    void suspenderPasaElMotivoAlServicioYTraduceErrores() {
        Solicitud suspendida = Solicitud.builder().id(1L).build();
        when(solicitudService.suspenderSolicitud(1L, "Estudiante retirado")).thenReturn(suspendida);
        assertEquals("Solicitud suspendida",
                wrapperDe(controller.suspender(1L, Map.of("motivo", "Estudiante retirado"))).getMessage());

        when(solicitudService.suspenderSolicitud(2L, null))
                .thenThrow(new RuntimeException("El motivo es obligatorio"));
        ResponseEntity<?> error = controller.suspender(2L, Map.of());
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("El motivo es obligatorio", wrapperDe(error).getMessage());
    }

    // ── Listados administrativos ──────────────────────────────────────────────

    @Test
    void listarYContarPorEstadoDeleganEnElServicio() {
        List<Solicitud> todas = List.of(Solicitud.builder().id(1L).build());
        Map<String, Long> conteo = Map.of("ENVIADA", 3L);
        when(solicitudService.listarSolicitudes()).thenReturn(todas);
        doReturn(conteo).when(solicitudService).contarPorEstado();

        assertSame(todas, wrapperDe(controller.listar()).getData());
        assertSame(conteo, wrapperDe(controller.contarPorEstado()).getData());
    }

    @Test
    void listarPaginadoArmaLaRespuestaConLosMetadatosDePagina() {
        Page<Solicitud> pagina = new PageImpl<>(
                List.of(Solicitud.builder().id(1L).build()), PageRequest.of(2, 20), 45);
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        when(solicitudService.listarSolicitudesPaginado(2, 20, "ENVIADA", "tema", desde, hasta))
                .thenReturn(pagina);

        ResponseEntity<?> response = controller.listarPaginado(2, 20, "ENVIADA", "tema", desde, hasta);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) wrapperDe(response).getData();
        assertEquals(1, ((List<?>) data.get("content")).size());
        // PageImpl recorta el total al offset+contenido cuando la ultima pagina viene
        // incompleta (40 elementos saltados + 1 en esta pagina), asi que el metadato real
        // que ve el frontend es 41, no el 45 nominal.
        assertEquals(41L, data.get("totalElements"));
        assertEquals(3, data.get("totalPages"));
        assertEquals(2, data.get("page"));
        assertEquals(20, data.get("size"));
    }

    @Test
    void listarPorEstudianteDelegaEnElServicio() {
        List<Solicitud> solicitudes = List.of(Solicitud.builder().id(1L).build());
        when(solicitudService.listarPorEstudiante(7L)).thenReturn(solicitudes);

        assertSame(solicitudes, wrapperDe(controller.listarPorEstudiante(7L)).getData());
    }

    // ── sp_generar_reporte_defensas ───────────────────────────────────────────

    @Test
    void reporteDefensasDevuelveLasFilasDelProcedimientoAlmacenado() {
        List<Map<String, Object>> reporte = List.of(Map.of("estudianteNombre", "Ana Pérez"));
        when(solicitudService.generarReporteDefensasSP("Software")).thenReturn(reporte);

        ResponseEntity<?> response = controller.reporteDefensas("Software");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(reporte, wrapperDe(response).getData());
    }

    @Test
    void reporteDefensasTraduceElErrorDelProcedimientoA400() {
        when(solicitudService.generarReporteDefensasSP(""))
                .thenThrow(new RuntimeException("cursor \"reporte_defensas_cursor\" does not exist"));

        ResponseEntity<?> response = controller.reporteDefensas("");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(wrapperDe(response).getMessage().contains("reporte_defensas_cursor"));
    }
}
