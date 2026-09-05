package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.EvaluacionFinal;
import ec.edu.uteq.presustentaciones.services.EvaluacionService;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * EvaluacionController expone sp_calcular_promedio_evaluacion (categoría "cálculos
 * agregados" del catálogo de procedimientos) y tenía 3 de 18 líneas cubiertas.
 * Se cubre tanto la ruta feliz como la traducción de errores del servicio a 400,
 * incluida la del endpoint del procedimiento almacenado.
 */
@ExtendWith(MockitoExtension.class)
class EvaluacionControllerTest {

    @Mock private EvaluacionService evaluacionService;

    @InjectMocks
    private EvaluacionController controller;

    @SuppressWarnings("unchecked")
    private String errorDe(ResponseEntity<?> response) {
        return ((Map<String, String>) response.getBody()).get("error");
    }

    @Test
    void evaluarPonderadoDevuelveLaEvaluacionCalculada() {
        EvaluacionFinal evaluacion = EvaluacionFinal.builder().id(1L).notaFinal(8.6).build();
        when(evaluacionService.evaluarSolicitud(1L, 2L, 9.0, 8.0, "Buen trabajo", 60.0, 40.0))
                .thenReturn(evaluacion);

        ResponseEntity<?> response = controller.evaluarPonderado(1L, 2L, 9.0, 8.0, "Buen trabajo", 60.0, 40.0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(evaluacion, response.getBody());
    }

    @Test
    void evaluarPonderadoConPesosInvalidosDevuelve400ConElMensajeDelServicio() {
        when(evaluacionService.evaluarSolicitud(1L, 2L, 9.0, 8.0, "obs", 70.0, 40.0))
                .thenThrow(new RuntimeException("Los pesos deben sumar 100"));

        ResponseEntity<?> response = controller.evaluarPonderado(1L, 2L, 9.0, 8.0, "obs", 70.0, 40.0);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Los pesos deben sumar 100", errorDe(response));
    }

    @Test
    void evaluarLegadoDelegaEnElServicioConLaNotaFinalDirecta() {
        EvaluacionFinal evaluacion = EvaluacionFinal.builder().id(1L).build();
        when(evaluacionService.evaluarSolicitud(1L, 2L, 7.5, "obs")).thenReturn(evaluacion);

        assertSame(evaluacion, controller.evaluar(1L, 2L, 7.5, "obs"));
    }

    @Test
    void listarPropagaLaPaginacionRecibida() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<EvaluacionFinal> pagina = new PageImpl<>(List.of(EvaluacionFinal.builder().id(1L).build()));
        when(evaluacionService.listarEvaluaciones(pageable)).thenReturn(pagina);

        assertSame(pagina, controller.listar(pageable).getBody());
    }

    @Test
    void listarPorEstudianteYPorUsuarioDeleganEnElServicio() {
        List<EvaluacionFinal> porEstudiante = List.of(EvaluacionFinal.builder().id(1L).build());
        List<EvaluacionFinal> porUsuario = List.of(EvaluacionFinal.builder().id(2L).build());
        when(evaluacionService.listarPorEstudiante(7L)).thenReturn(porEstudiante);
        when(evaluacionService.listarPorUsuario(50L)).thenReturn(porUsuario);

        assertSame(porEstudiante, controller.listarPorEstudiante(7L));
        assertSame(porUsuario, controller.listarPorUsuario(50L));
    }

    @Test
    void porSolicitudDevuelve404CuandoLaSolicitudNoTieneEvaluacion() {
        when(evaluacionService.buscarPorSolicitud(1L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, controller.porSolicitud(1L).getStatusCode());
    }

    @Test
    void porSolicitudDevuelveLaEvaluacionCuandoExiste() {
        EvaluacionFinal evaluacion = EvaluacionFinal.builder().id(1L).build();
        when(evaluacionService.buscarPorSolicitud(1L)).thenReturn(Optional.of(evaluacion));

        assertSame(evaluacion, controller.porSolicitud(1L).getBody());
    }

    // ── sp_calcular_promedio_evaluacion ───────────────────────────────────────

    @Test
    void calcularPromedioDevuelveElResultadoDelProcedimientoAlmacenado() {
        Map<String, Object> resultado = Map.of(
                "solicitudId", 1L, "notaFinal", 8.6, "estadoResultado", "APROBADO");
        when(evaluacionService.calcularPromedioSP(1L)).thenReturn(resultado);

        ResponseEntity<?> response = controller.calcularPromedio(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(resultado, response.getBody());
    }

    @Test
    void calcularPromedioTraduceElErrorDelProcedimientoA400() {
        when(evaluacionService.calcularPromedioSP(99L))
                .thenThrow(new RuntimeException("La solicitud 99 no tiene evaluaciones por criterio"));

        ResponseEntity<?> response = controller.calcularPromedio(99L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("La solicitud 99 no tiene evaluaciones por criterio", errorDe(response));
    }
}
