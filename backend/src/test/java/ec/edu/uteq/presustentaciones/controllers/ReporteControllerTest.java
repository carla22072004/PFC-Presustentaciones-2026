package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.ReporteConteoDTO;
import ec.edu.uteq.presustentaciones.dto.ReporteDefensaResult;
import ec.edu.uteq.presustentaciones.dto.ReporteResumenDTO;
import ec.edu.uteq.presustentaciones.entities.Cronograma;
import ec.edu.uteq.presustentaciones.entities.EstadoCronograma;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.EvaluacionFinal;
import ec.edu.uteq.presustentaciones.entities.ResultadoEvaluacion;
import ec.edu.uteq.presustentaciones.entities.Sala;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.CronogramaRepository;
import ec.edu.uteq.presustentaciones.repositories.EvaluacionFinalRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import ec.edu.uteq.presustentaciones.services.ReporteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ReporteController era el hueco de cobertura más grande del paquete de controladores
 * (2 de 148 líneas, 1.4 %, con 66 ramas sin ejercitar) pese a ser uno de los que exponen
 * procedimientos almacenados -- justamente los que la guía pide priorizar.
 *
 * Los reportes PDF se generan de verdad con iText contra un ByteArrayOutputStream en
 * memoria: se verifica que la salida sea un PDF real (cabecera %PDF-) y no solo que el
 * método no lance excepción. Cada caso incluye filas con relaciones nulas (solicitud,
 * estudiante, sala, estado, resultado, notas), que es donde vive la mayoría de las ramas
 * y donde un NullPointerException real rompería la descarga del reporte en producción.
 */
@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock private CronogramaRepository cronogramaRepo;
    @Mock private EvaluacionFinalRepository evaluacionFinalRepo;
    @Mock private SolicitudRepository solicitudRepo;
    @Mock private ReporteService reporteService;

    @InjectMocks
    private ReporteController controller;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Usuario usuario(String nombre, String apellido) {
        return Usuario.builder().id(1L).nombre(nombre).apellido(apellido).build();
    }

    private Solicitud solicitudCompleta() {
        return Solicitud.builder()
                .id(1L)
                .tituloTema("Sistema de gestión de pre-sustentaciones")
                .estudiante(Estudiante.builder().id(1L).usuario(usuario("Ana", "Pérez")).build())
                .build();
    }

    private void assertEsPdfDescargable(ResponseEntity<byte[]> response, String filename) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains(filename));
        byte[] body = response.getBody();
        assertNotNull(body);
        assertTrue(body.length > 0, "el PDF no puede venir vacío");
        assertTrue(new String(body, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF-"),
                "la respuesta debe ser un PDF real generado por iText, no bytes arbitrarios");
    }

    // ── PDF de cronograma ─────────────────────────────────────────────────────

    @Test
    void reporteCronogramaGeneraPdfRealConFilasCompletasYConRelacionesNulas() throws Exception {
        Cronograma completo = Cronograma.builder()
                .id(1L)
                .solicitud(solicitudCompleta())
                .sala(Sala.builder().id(1L).nombre("Aula 101").build())
                .estado(EstadoCronograma.builder().codigo("PROGRAMADO").nombre("Programado").build())
                .fechaInicio(LocalDateTime.of(2026, 9, 10, 9, 0))
                .build();
        // Fila sin solicitud, sin sala y sin estado: ejercita las tres ramas de fallback "—"
        Cronograma minimo = Cronograma.builder()
                .id(2L)
                .fechaInicio(LocalDateTime.of(2026, 9, 11, 11, 30))
                .build();
        // Tercera fila para ejercitar también el alternado de color de fondo (i % 2)
        Cronograma sinEstudiante = Cronograma.builder()
                .id(3L)
                .solicitud(Solicitud.builder().id(2L).build())
                .fechaInicio(LocalDateTime.of(2026, 9, 12, 15, 0))
                .build();
        when(cronogramaRepo.findReporteCronograma()).thenReturn(List.of(completo, minimo, sinEstudiante));

        assertEsPdfDescargable(controller.reporteCronograma(), "cronograma_presustentaciones.pdf");
        verify(cronogramaRepo).findReporteCronograma();
    }

    @Test
    void reporteCronogramaSinDatosGeneraPdfConTablaVacia() throws Exception {
        when(cronogramaRepo.findReporteCronograma()).thenReturn(List.of());

        assertEsPdfDescargable(controller.reporteCronograma(), "cronograma_presustentaciones.pdf");
    }

    // ── PDF de estadísticas ───────────────────────────────────────────────────

    @Test
    void reporteEstadisticasGeneraPdfRealConAprobadosReprobadosYSinResultado() throws Exception {
        EvaluacionFinal aprobado = EvaluacionFinal.builder()
                .id(1L)
                .solicitud(solicitudCompleta())
                .notaInstructor(9.0).notaJuradoPromedio(8.5).notaFinal(8.8)
                .resultado(ResultadoEvaluacion.builder().codigo("APROBADO").nombre("Aprobado").build())
                .build();
        EvaluacionFinal reprobado = EvaluacionFinal.builder()
                .id(2L)
                .solicitud(Solicitud.builder().id(3L).build())
                .notaInstructor(4.0).notaJuradoPromedio(3.5).notaFinal(3.8)
                .resultado(ResultadoEvaluacion.builder().codigo("REPROBADO").nombre("Reprobado").build())
                .build();
        // Sin resultado y sin notas: ejercita el fallback "—" de fmt() y la rama de color rojo
        EvaluacionFinal sinDatos = EvaluacionFinal.builder().id(3L).build();
        when(evaluacionFinalRepo.findAllWithRelationships())
                .thenReturn(List.of(aprobado, reprobado, sinDatos));

        assertEsPdfDescargable(controller.reporteEstadisticas(), "estadisticas_evaluaciones.pdf");
        verify(evaluacionFinalRepo).findAllWithRelationships();
    }

    @Test
    void reporteEstadisticasSinEvaluacionesUsaPromedioCeroYNoFalla() throws Exception {
        when(evaluacionFinalRepo.findAllWithRelationships()).thenReturn(List.of());

        assertEsPdfDescargable(controller.reporteEstadisticas(), "estadisticas_evaluaciones.pdf");
    }

    // ── Procedimiento almacenado sp_generar_reporte_defensas ──────────────────

    @Test
    void reporteDefensasDelegaEnElProcedimientoAlmacenado() {
        List<ReporteDefensaResult> esperado = List.of(new ReporteDefensaResult());
        when(solicitudRepo.generarReporteDefensas("Software")).thenReturn(esperado);

        ResponseEntity<List<ReporteDefensaResult>> response = controller.reporteDefensas("Software");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(esperado, response.getBody());
        verify(solicitudRepo).generarReporteDefensas("Software");
    }

    // ── Estadísticas JSON ─────────────────────────────────────────────────────

    @Test
    void estadisticasJsonCalculaTotalesPromedioYTasaDeAprobacion() {
        EvaluacionFinal aprobado1 = EvaluacionFinal.builder().notaFinal(8.0)
                .resultado(ResultadoEvaluacion.builder().codigo("APROBADO").build()).build();
        EvaluacionFinal aprobado2 = EvaluacionFinal.builder().notaFinal(9.0)
                .resultado(ResultadoEvaluacion.builder().codigo("APROBADO").build()).build();
        EvaluacionFinal reprobado = EvaluacionFinal.builder().notaFinal(4.0)
                .resultado(ResultadoEvaluacion.builder().codigo("REPROBADO").build()).build();
        // notaFinal null y resultado null: no debe contar en el promedio ni en los conteos
        EvaluacionFinal incompleto = EvaluacionFinal.builder().build();
        when(evaluacionFinalRepo.findAllWithRelationships())
                .thenReturn(List.of(aprobado1, aprobado2, reprobado, incompleto));
        when(solicitudRepo.countByEstadoCodigo("APROBADA")).thenReturn(5L);

        ResponseEntity<Map<String, Object>> response = controller.estadisticasJson();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(4L, body.get("totalEvaluados"));
        assertEquals(2L, body.get("aprobados"));
        assertEquals(1L, body.get("reprobados"));
        // (8.0 + 9.0 + 4.0) / 3 = 7.0 -- el null queda fuera por el filtro n > 0
        assertEquals(7.0, body.get("notaPromedio"));
        assertEquals(50L, body.get("tasaAprobacion")); // 2 de 4
        assertEquals(5L, body.get("solicitudesPendientes"));
    }

    @Test
    void estadisticasJsonSinEvaluacionesDevuelveTasaCeroSinDividirPorCero() {
        when(evaluacionFinalRepo.findAllWithRelationships()).thenReturn(List.of());
        when(solicitudRepo.countByEstadoCodigo("APROBADA")).thenReturn(0L);

        Map<String, Object> body = controller.estadisticasJson().getBody();

        assertNotNull(body);
        assertEquals(0L, body.get("totalEvaluados"));
        assertEquals(0.0, body.get("notaPromedio"));
        // El operador ternario del controlador promueve ambas ramas a long, asi que
        // la tasa viaja como Long incluso en el caso 0 -- el frontend recibe siempre
        // el mismo tipo JSON, sin importar si hay evaluaciones o no.
        assertEquals(0L, body.get("tasaAprobacion"));
    }

    // ── Módulo de reportes JSON (delegación en ReporteService) ────────────────

    @Test
    void resumenDelegaEnElServicioConFiltrosDeFechaYCarrera() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        ReporteResumenDTO esperado = ReporteResumenDTO.builder().build();
        when(reporteService.resumen(desde, hasta, "Software")).thenReturn(esperado);

        ResponseEntity<?> response = controller.resumen(desde, hasta, "Software");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(esperado, response.getBody());
    }

    @Test
    void resumenSinFiltrosPasaNullsAlServicio() {
        when(reporteService.resumen(null, null, null)).thenReturn(ReporteResumenDTO.builder().build());

        assertEquals(HttpStatus.OK, controller.resumen(null, null, null).getStatusCode());
        verify(reporteService).resumen(null, null, null);
    }

    @Test
    void solicitudesPorEstadoDelegaEnElServicio() {
        List<ReporteConteoDTO> esperado = List.of(new ReporteConteoDTO());
        when(reporteService.solicitudesPorEstado(null, null, null)).thenReturn(esperado);

        ResponseEntity<?> response = controller.solicitudesPorEstado(null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(esperado, response.getBody());
    }

    @Test
    void sustentacionesPorPeriodoDelegaEnElServicio() {
        List<ReporteConteoDTO> esperado = List.of(new ReporteConteoDTO());
        when(reporteService.sustentacionesPorPeriodo(null, null)).thenReturn(esperado);

        ResponseEntity<?> response = controller.sustentacionesPorPeriodo(null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(esperado, response.getBody());
    }

    @Test
    void resumenActasDelegaEnElServicio() {
        Map<String, Long> esperado = Map.of("generadas", 3L);
        when(reporteService.resumenActas(null, null)).thenReturn(esperado);

        ResponseEntity<?> response = controller.resumenActas(null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(esperado, response.getBody());
    }

    @Test
    void actividadDocenteDelegaEnElServicio() {
        when(reporteService.actividadPorDocente()).thenReturn(List.of());

        assertEquals(HttpStatus.OK, controller.actividadDocente().getStatusCode());
        verify(reporteService).actividadPorDocente();
    }

    @Test
    void porCarreraDelegaEnElServicio() {
        List<Map<String, Object>> esperado = List.of(Map.of("carrera", "Software"));
        when(reporteService.estadisticasPorCarrera()).thenReturn(esperado);

        ResponseEntity<?> response = controller.porCarrera();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(esperado, response.getBody());
    }
}
