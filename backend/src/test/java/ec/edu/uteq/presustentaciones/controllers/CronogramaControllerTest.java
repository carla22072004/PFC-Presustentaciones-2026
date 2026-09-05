package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Cronograma;
import ec.edu.uteq.presustentaciones.services.CronogramaService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CronogramaController es el punto de entrada de sp_validar_conflicto_jurado (la
 * validación cruzada que impide programar una defensa con un jurado ya ocupado) y
 * tenía 1 de 17 líneas cubiertas.
 *
 * El caso más importante que se cubre aquí es el de conflicto: el servicio lanza una
 * RuntimeException con el mensaje del procedimiento y el controlador debe devolver un
 * 400 legible, no un 500.
 */
@ExtendWith(MockitoExtension.class)
class CronogramaControllerTest {

    @Mock private CronogramaService cronogramaService;

    @InjectMocks
    private CronogramaController controller;

    @SuppressWarnings("unchecked")
    private String errorDe(ResponseEntity<?> response) {
        return ((Map<String, String>) response.getBody()).get("error");
    }

    @Test
    void crearDevuelveElCronogramaProgramado() {
        LocalDate fecha = LocalDate.of(2026, 9, 10);
        LocalTime hora = LocalTime.of(9, 0);
        Cronograma cronograma = Cronograma.builder().id(1L).build();
        when(cronogramaService.crearCronograma(1L, 2L, fecha, hora)).thenReturn(cronograma);

        ResponseEntity<?> response = controller.crear(1L, 2L, fecha, hora);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(cronograma, response.getBody());
    }

    @Test
    void crearConJuradoEnConflictoDevuelve400ConElMensajeDelProcedimiento() {
        LocalDate fecha = LocalDate.of(2026, 9, 10);
        LocalTime hora = LocalTime.of(9, 0);
        when(cronogramaService.crearCronograma(1L, 2L, fecha, hora))
                .thenThrow(new RuntimeException("El docente Ana Pérez ya tiene una defensa en ese horario"));

        ResponseEntity<?> response = controller.crear(1L, 2L, fecha, hora);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("El docente Ana Pérez ya tiene una defensa en ese horario", errorDe(response));
    }

    @Test
    void asignarAutomaticoDevuelveElCronogramaYTraduceErroresA400() {
        Cronograma cronograma = Cronograma.builder().id(1L).build();
        when(cronogramaService.asignarAutomatico(1L)).thenReturn(cronograma);
        assertSame(cronograma, controller.asignarAutomatico(1L).getBody());

        when(cronogramaService.asignarAutomatico(2L))
                .thenThrow(new RuntimeException("No hay franjas disponibles esta semana"));
        ResponseEntity<?> error = controller.asignarAutomatico(2L);
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("No hay franjas disponibles esta semana", errorDe(error));
    }

    @Test
    void disponibilidadDevuelveLasFranjasConLaFechaYDuracionConsultadas() {
        LocalDate fecha = LocalDate.of(2026, 9, 10);
        List<LocalDateTime> franjas = List.of(fecha.atTime(9, 0), fecha.atTime(10, 0));
        when(cronogramaService.franjasDisponibles(fecha, 45)).thenReturn(franjas);

        Map<String, Object> body = controller.disponibilidad(fecha, 45).getBody();

        assertNotNull(body);
        assertEquals(fecha, body.get("fecha"));
        assertEquals(45, body.get("duracionMin"));
        assertSame(franjas, body.get("franjas"));
    }

    @Test
    void verificarDisponibilidadDevuelveMensajeDistintoSegunElResultado() {
        LocalDateTime inicio = LocalDateTime.of(2026, 9, 10, 9, 0);
        when(cronogramaService.estaDisponible(1L, inicio, 45)).thenReturn(true);
        when(cronogramaService.estaDisponible(2L, inicio, 45)).thenReturn(false);

        Map<String, Object> libre = controller.verificarDisponibilidad(1L, inicio, 45).getBody();
        Map<String, Object> ocupada = controller.verificarDisponibilidad(2L, inicio, 45).getBody();

        assertNotNull(libre);
        assertNotNull(ocupada);
        assertEquals(true, libre.get("disponible"));
        assertTrue(((String) libre.get("mensaje")).contains("disponible"));
        assertEquals(false, ocupada.get("disponible"));
        assertTrue(((String) ocupada.get("mensaje")).contains("ocupada"));
    }

    @Test
    void listarPorEstudianteYPorUsuarioDeleganEnElServicio() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Cronograma> pagina = new PageImpl<>(List.of(Cronograma.builder().id(1L).build()));
        List<Cronograma> porEstudiante = List.of(Cronograma.builder().id(2L).build());
        List<Cronograma> porUsuario = List.of(Cronograma.builder().id(3L).build());
        when(cronogramaService.listarCronogramas(pageable)).thenReturn(pagina);
        when(cronogramaService.listarPorEstudiante(7L)).thenReturn(porEstudiante);
        when(cronogramaService.listarPorUsuario(50L)).thenReturn(porUsuario);

        assertSame(pagina, controller.listar(pageable).getBody());
        assertSame(porEstudiante, controller.porEstudiante(7L));
        assertSame(porUsuario, controller.porUsuario(50L));
    }

    @Test
    void porSolicitudDevuelve404CuandoNoHayCronogramaProgramado() {
        when(cronogramaService.buscarPorSolicitud(1L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, controller.porSolicitud(1L).getStatusCode());
    }

    @Test
    void porSolicitudDevuelveElCronogramaCuandoExiste() {
        Cronograma cronograma = Cronograma.builder().id(1L).build();
        when(cronogramaService.buscarPorSolicitud(1L)).thenReturn(Optional.of(cronograma));

        assertSame(cronograma, controller.porSolicitud(1L).getBody());
    }

    @Test
    void eliminarDevuelve204() {
        assertEquals(HttpStatus.NO_CONTENT, controller.eliminar(3L).getStatusCode());
        verify(cronogramaService).eliminar(3L);
    }
}
