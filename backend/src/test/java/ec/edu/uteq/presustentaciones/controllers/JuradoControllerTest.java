package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.entities.Jurado;
import ec.edu.uteq.presustentaciones.entities.RolJurado;
import ec.edu.uteq.presustentaciones.entities.Tutor;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.services.JuradoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JuradoController expone sp_asignar_jurado_masivo, uno de los procedimientos
 * almacenados que se defienden en el examen, y tenía 3 de 53 líneas cubiertas.
 *
 * Además de la ruta feliz se cubre el manejo de error de cada endpoint: el
 * controlador traduce cualquier RuntimeException del servicio a un 400 con
 * ResponseWrapper.error, en vez de dejar que escale a un 500 -- comportamiento
 * que ninguna prueba verificaba hasta ahora.
 */
@ExtendWith(MockitoExtension.class)
class JuradoControllerTest {

    @Mock private JuradoService juradoService;

    @InjectMocks
    private JuradoController controller;

    @SuppressWarnings("unchecked")
    private ResponseWrapper<Object> wrapperDe(ResponseEntity<?> response) {
        return (ResponseWrapper<Object>) response.getBody();
    }

    private Jurado juradoConDocente(String nombre, String apellido, String rolCodigo) {
        return Jurado.builder()
                .id(1L)
                .confirmado(true)
                .rolJurado(RolJurado.builder().codigo(rolCodigo).build())
                .docente(Docente.builder().id(1L)
                        .usuario(Usuario.builder().id(1L).nombre(nombre).apellido(apellido).build())
                        .build())
                .build();
    }

    // ── Asignación individual ─────────────────────────────────────────────────

    @Test
    void asignarJuradoDevuelveElJuradoAsignado() {
        Jurado jurado = Jurado.builder().id(1L).build();
        when(juradoService.asignarJurado(1L, 2L, "PRESIDENTE")).thenReturn(jurado);

        ResponseEntity<?> response = controller.asignarJurado(1L, 2L, "PRESIDENTE");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(wrapperDe(response).isSuccess());
        assertSame(jurado, wrapperDe(response).getData());
        assertEquals("Jurado asignado exitosamente", wrapperDe(response).getMessage());
    }

    @Test
    void asignarJuradoTraduceElErrorDelServicioA400() {
        when(juradoService.asignarJurado(1L, 2L, "PRESIDENTE"))
                .thenThrow(new RuntimeException("El docente ya es jurado de esta solicitud"));

        ResponseEntity<?> response = controller.asignarJurado(1L, 2L, "PRESIDENTE");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(wrapperDe(response).isSuccess());
        assertEquals("El docente ya es jurado de esta solicitud", wrapperDe(response).getMessage());
    }

    @Test
    void asignarAutomaticamenteDevuelveLosJuradosResultantes() {
        List<Jurado> jurados = List.of(Jurado.builder().id(1L).build(), Jurado.builder().id(2L).build());
        when(juradoService.listarPorSolicitud(1L)).thenReturn(jurados);

        ResponseEntity<?> response = controller.asignarAutomaticamente(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(jurados, wrapperDe(response).getData());
        verify(juradoService).asignarJuradosAutomaticamente(1L);
    }

    @Test
    void asignarAutomaticamenteTraduceElErrorDelServicioA400() {
        doThrow(new RuntimeException("No hay suficientes docentes disponibles"))
                .when(juradoService).asignarJuradosAutomaticamente(1L);

        ResponseEntity<?> response = controller.asignarAutomaticamente(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No hay suficientes docentes disponibles", wrapperDe(response).getMessage());
        verify(juradoService, never()).listarPorSolicitud(any());
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Test
    void listarPorSolicitudEnvuelveLaListaDelServicio() {
        List<Jurado> jurados = List.of(Jurado.builder().id(1L).build());
        when(juradoService.listarPorSolicitud(1L)).thenReturn(jurados);

        assertSame(jurados, wrapperDe(controller.listarPorSolicitud(1L)).getData());
    }

    @Test
    void listarTodosPropagaLaPaginacionRecibida() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Jurado> pagina = new PageImpl<>(List.of(Jurado.builder().id(1L).build()));
        when(juradoService.listarTodos(pageable)).thenReturn(pagina);

        assertSame(pagina, wrapperDe(controller.listarTodos(pageable)).getData());
    }

    @Test
    void eliminarJuradoDevuelve204() {
        assertEquals(HttpStatus.NO_CONTENT, controller.eliminarJurado(9L).getStatusCode());
        verify(juradoService).eliminarJurado(9L);
    }

    @Test
    void sugerirDocentesUsaLaCantidadSolicitada() {
        List<Docente> docentes = List.of(Docente.builder().id(1L).build());
        when(juradoService.sugerirDocentes(1L, 3)).thenReturn(docentes);

        assertSame(docentes, wrapperDe(controller.sugerirDocentes(1L, 3)).getData());
    }

    @Test
    void listarPorDocenteYTutoriasPorDocenteDeleganEnElServicio() {
        when(juradoService.listarPorDocente(4L)).thenReturn(List.of(Jurado.builder().id(1L).build()));
        when(juradoService.listarTutoriasPorDocente(4L)).thenReturn(List.of(Tutor.builder().id(1L).build()));

        assertEquals(HttpStatus.OK, controller.listarPorDocente(4L).getStatusCode());
        assertEquals(HttpStatus.OK, controller.listarTutoriasPorDocente(4L).getStatusCode());
    }

    // ── Tutor ─────────────────────────────────────────────────────────────────

    @Test
    void asignarTutorDevuelveElTutorAsignado() {
        Tutor tutor = Tutor.builder().id(1L).build();
        when(juradoService.asignarTutor(1L, 2L)).thenReturn(tutor);

        ResponseEntity<?> response = controller.asignarTutor(1L, 2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(tutor, wrapperDe(response).getData());
    }

    @Test
    void asignarTutorTraduceElErrorDelServicioA400() {
        when(juradoService.asignarTutor(1L, 2L)).thenThrow(new RuntimeException("La solicitud ya tiene tutor"));

        ResponseEntity<?> response = controller.asignarTutor(1L, 2L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("La solicitud ya tiene tutor", wrapperDe(response).getMessage());
    }

    @Test
    void obtenerTutorDevuelve404CuandoLaSolicitudNoTieneTutor() {
        when(juradoService.obtenerTutorDeSolicitud(1L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, controller.obtenerTutor(1L).getStatusCode());
    }

    @Test
    void obtenerTutorDevuelveElTutorCuandoExiste() {
        Tutor tutor = Tutor.builder().id(1L).build();
        when(juradoService.obtenerTutorDeSolicitud(1L)).thenReturn(Optional.of(tutor));

        ResponseEntity<?> response = controller.obtenerTutor(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(tutor, wrapperDe(response).getData());
    }

    @Test
    void eliminarTutorDevuelve204() {
        assertEquals(HttpStatus.NO_CONTENT, controller.eliminarTutor(7L).getStatusCode());
        verify(juradoService).eliminarTutor(7L);
    }

    // ── Info de jurado (armado manual del Map de respuesta) ───────────────────

    @Test
    void obtenerInfoJuradoArmaElNombreDelDocenteCuandoLaCadenaEstaCompleta() {
        when(juradoService.obtenerInfoJurado(1L, 2L))
                .thenReturn(Optional.of(juradoConDocente("Ana", "Pérez", "PRESIDENTE")));

        ResponseEntity<?> response = controller.obtenerInfoJurado(1L, 2L);

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) wrapperDe(response).getData();
        assertEquals(1L, info.get("id"));
        assertEquals("PRESIDENTE", info.get("rol"));
        assertEquals(true, info.get("confirmado"));
        assertEquals("Ana Pérez", info.get("nombreDocente"));
    }

    @Test
    void obtenerInfoJuradoSinDocenteNiRolNoRompeYDevuelveCadenasVacias() {
        // Jurado sin docente y sin rolJurado: getRol() devuelve null y el nombre queda vacío.
        // Map.of no admite valores nulos, así que si el controlador no hiciera el fallback
        // este endpoint reventaría con NullPointerException en produccion.
        when(juradoService.obtenerInfoJurado(1L, 2L))
                .thenReturn(Optional.of(Jurado.builder().id(5L).confirmado(false).build()));

        ResponseEntity<?> response = controller.obtenerInfoJurado(1L, 2L);

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) wrapperDe(response).getData();
        assertEquals(5L, info.get("id"));
        assertEquals("", info.get("rol"));
        assertEquals(false, info.get("confirmado"));
        assertEquals("", info.get("nombreDocente"));
    }

    @Test
    void obtenerInfoJuradoDevuelveDataNulaCuandoNoHayAsignacion() {
        when(juradoService.obtenerInfoJurado(1L, 2L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.obtenerInfoJurado(1L, 2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(wrapperDe(response).getData());
    }

    // ── sp_asignar_jurado_masivo ──────────────────────────────────────────────

    @Test
    void asignarMasivoConvierteLosIdsJsonAArreglosLongYLlamaAlProcedimiento() {
        ResponseEntity<?> response = controller.asignarMasivo(Map.of(
                "solicitudIds", List.of(1, 2, 3),
                "docenteIds", List.of(4, 5, 6),
                "rol", "PRESIDENTE"));

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Jackson deserializa los enteros del JSON como Integer; el procedimiento espera Long[].
        ArgumentCaptor<Long[]> solicitudes = ArgumentCaptor.forClass(Long[].class);
        ArgumentCaptor<Long[]> docentes = ArgumentCaptor.forClass(Long[].class);
        verify(juradoService).asignarJuradoMasivoSP(solicitudes.capture(), docentes.capture(), eq("PRESIDENTE"));
        assertArrayEquals(new Long[]{1L, 2L, 3L}, solicitudes.getValue());
        assertArrayEquals(new Long[]{4L, 5L, 6L}, docentes.getValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) wrapperDe(response).getData();
        assertEquals(3, data.get("asignados"));
        assertEquals("PRESIDENTE", data.get("rol"));
    }

    @Test
    void asignarMasivoRechazaElCuerpoIncompletoSinLlamarAlProcedimiento() {
        ResponseEntity<?> sinRol = controller.asignarMasivo(Map.of(
                "solicitudIds", List.of(1), "docenteIds", List.of(2)));

        assertEquals(HttpStatus.BAD_REQUEST, sinRol.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> error = (Map<String, String>) sinRol.getBody();
        assertEquals("Se requieren 'solicitudIds', 'docenteIds' y 'rol'", error.get("error"));
        verify(juradoService, never()).asignarJuradoMasivoSP(any(), any(), any());
    }

    @Test
    void asignarMasivoTraduceElErrorDelProcedimientoA400() {
        doThrow(new RuntimeException("rol_jurado inexistente"))
                .when(juradoService).asignarJuradoMasivoSP(any(), any(), eq("INVENTADO"));

        ResponseEntity<?> response = controller.asignarMasivo(Map.of(
                "solicitudIds", List.of(1),
                "docenteIds", List.of(2),
                "rol", "INVENTADO"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("rol_jurado inexistente", wrapperDe(response).getMessage());
    }
}
