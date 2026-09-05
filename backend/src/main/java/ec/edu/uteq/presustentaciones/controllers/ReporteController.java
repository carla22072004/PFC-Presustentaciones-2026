package ec.edu.uteq.presustentaciones.controllers;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import ec.edu.uteq.presustentaciones.entities.Cronograma;
import ec.edu.uteq.presustentaciones.entities.EvaluacionFinal;
import ec.edu.uteq.presustentaciones.repositories.CronogramaRepository;
import ec.edu.uteq.presustentaciones.repositories.EvaluacionFinalRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import ec.edu.uteq.presustentaciones.services.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("@permisoService.tienePermiso(authentication, 'REPORTES_VER')")
public class ReporteController {

    private final CronogramaRepository cronogramaRepo;
    private final EvaluacionFinalRepository evaluacionFinalRepo;
    private final SolicitudRepository solicitudRepo;
    private final ReporteService reporteService;

    // ── Colores — siempre new DeviceRgb para evitar conflicto con Color.WHITE ──
    private static DeviceRgb BLUE()       { return new DeviceRgb(0,   56,  101); }
    private static DeviceRgb GOLD()       { return new DeviceRgb(204, 153, 0);   }
    private static DeviceRgb WHITE()      { return new DeviceRgb(255, 255, 255); }
    private static DeviceRgb LIGHT_BG()   { return new DeviceRgb(240, 243, 248); }
    private static DeviceRgb GRAY_TEXT()  { return new DeviceRgb(120, 120, 120); }
    private static DeviceRgb DARK_TEXT()  { return new DeviceRgb(80,  80,  80);  }
    private static DeviceRgb GREEN()      { return new DeviceRgb(21,  128, 61);  }
    private static DeviceRgb RED()        { return new DeviceRgb(185, 28,  28);  }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * RF-11: Genera el PDF con el cronograma completo de pre-sustentaciones.
     *
     * @return 200 con el PDF como adjunto descargable
     * @throws Exception si iText falla al construir el documento o las fuentes
     */
    @GetMapping("/cronograma/pdf")
    public ResponseEntity<byte[]> reporteCronograma() throws Exception {
        List<Cronograma> lista = cronogramaRepo.findReporteCronograma();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = abrirDoc(baos);
        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        encabezado(doc, bold, regular,
                "Cronograma de Pre-Sustentaciones",
                "Trabajo de Integración Curricular — Décimo Semestre");

        Table table = new Table(UnitValue.createPercentArray(new float[]{4, 18, 16, 12, 8}))
                .useAllAvailableWidth();
        for (String h : new String[]{"#", "Estudiante / Tema", "Fecha y Hora", "Sala", "Estado"}) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(9).setFontColor(WHITE()))
                    .setBackgroundColor(BLUE()).setTextAlignment(TextAlignment.CENTER));
        }

        int i = 1;
        for (Cronograma c : lista) {
            DeviceRgb bg = (i % 2 == 0) ? LIGHT_BG() : WHITE();
            String est = "—", tema = "—";
            if (c.getSolicitud() != null) {
                var u = c.getSolicitud().getEstudiante() != null
                        ? c.getSolicitud().getEstudiante().getUsuario() : null;
                if (u != null) est = u.getNombre() + " " + u.getApellido();
                if (c.getSolicitud().getTituloTema() != null) tema = c.getSolicitud().getTituloTema();
            }
            table.addCell(celda(String.valueOf(i++), regular, bg, TextAlignment.CENTER));
            table.addCell(new Cell()
                    .add(new Paragraph(est).setFont(bold).setFontSize(8))
                    .add(new Paragraph(tema).setFont(regular).setFontSize(7).setFontColor(DARK_TEXT()))
                    .setBackgroundColor(bg));
            table.addCell(celda(c.getFechaInicio().format(FMT), regular, bg, TextAlignment.CENTER));
            table.addCell(celda(c.getSala() != null ? c.getSala().getNombre() : "—", regular, bg, TextAlignment.CENTER));
            table.addCell(celda(c.getEstado() != null ? c.getEstado().getNombre() : "—", regular, bg, TextAlignment.CENTER));
        }
        doc.add(table);
        doc.add(new Paragraph("Total: " + lista.size() + " pre-sustentación(es) programadas.")
                 .setFont(bold).setFontSize(9).setMarginTop(10));
        doc.close();

        return pdfResponse(baos, "cronograma_presustentaciones.pdf");
    }

    /**
     * RF-11: Genera el PDF de estadísticas de evaluaciones (totales, aprobados, reprobados,
     * nota promedio y detalle por estudiante).
     *
     * @return 200 con el PDF como adjunto descargable
     * @throws Exception si iText falla al construir el documento o las fuentes
     */
    @GetMapping("/estadisticas/pdf")
    public ResponseEntity<byte[]> reporteEstadisticas() throws Exception {
        List<EvaluacionFinal> evals = evaluacionFinalRepo.findAllWithRelationships();

        long total      = evals.size();
        long aprobados  = evals.stream().filter(e -> e.getResultado() != null && "APROBADO".equals(e.getResultado().getCodigo())).count();
        long reprobados = evals.stream().filter(e -> e.getResultado() != null && "REPROBADO".equals(e.getResultado().getCodigo())).count();
        double promedio = evals.stream()
                .mapToDouble(e -> e.getNotaFinal() != null ? e.getNotaFinal() : 0)
                .filter(n -> n > 0).average().orElse(0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = abrirDoc(baos);
        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        encabezado(doc, bold, regular,
                "Estadísticas de Evaluaciones",
                "Pre-Sustentaciones TIC II — Carrera Software");

        // Resumen en 4 celdas
        Table resumen = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
        celdaStat(resumen, "Total evaluados", String.valueOf(total),      bold, regular, BLUE());
        celdaStat(resumen, "Aprobados",       String.valueOf(aprobados),  bold, regular, GREEN());
        celdaStat(resumen, "Reprobados",      String.valueOf(reprobados), bold, regular, RED());
        celdaStat(resumen, "Nota promedio",   String.format("%.2f", promedio), bold, regular, GOLD());
        doc.add(resumen);
        doc.add(new Paragraph(" ").setMarginBottom(16));

        // Tabla detalle
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{3, 14, 5, 5, 5, 5})).useAllAvailableWidth();
        for (String h : new String[]{"#", "Estudiante / Tema", "Nota Inst.", "Nota Trib.", "Nota Final", "Resultado"}) {
            tabla.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(WHITE()))
                    .setBackgroundColor(BLUE()).setTextAlignment(TextAlignment.CENTER));
        }

        int idx = 1;
        for (EvaluacionFinal e : evals) {
            DeviceRgb bg = (idx % 2 == 0) ? LIGHT_BG() : WHITE();
            String est = "—", tema = "—";
            if (e.getSolicitud() != null) {
                var u = e.getSolicitud().getEstudiante() != null
                        ? e.getSolicitud().getEstudiante().getUsuario() : null;
                if (u != null) est = u.getNombre() + " " + u.getApellido();
                if (e.getSolicitud().getTituloTema() != null) tema = e.getSolicitud().getTituloTema();
            }
            tabla.addCell(celda(String.valueOf(idx++), regular, bg, TextAlignment.CENTER));
            tabla.addCell(new Cell()
                    .add(new Paragraph(est).setFont(bold).setFontSize(8))
                    .add(new Paragraph(tema).setFont(regular).setFontSize(7).setFontColor(DARK_TEXT()))
                    .setBackgroundColor(bg));
            tabla.addCell(celda(fmt(e.getNotaInstructor()), regular, bg, TextAlignment.CENTER));
            tabla.addCell(celda(fmt(e.getNotaJuradoPromedio()), regular, bg, TextAlignment.CENTER));
            tabla.addCell(celda(fmt(e.getNotaFinal()),      bold,    bg, TextAlignment.CENTER));
            
            String resCod = e.getResultado() != null ? e.getResultado().getCodigo() : "";
            DeviceRgb rc = "APROBADO".equals(resCod) ? GREEN() : RED();
            tabla.addCell(new Cell()
                    .add(new Paragraph(e.getResultado() != null ? e.getResultado().getNombre() : "—")
                            .setFont(bold).setFontSize(8).setFontColor(rc))
                    .setBackgroundColor(bg).setTextAlignment(TextAlignment.CENTER));
        }
        doc.add(tabla);
        doc.close();

        return pdfResponse(baos, "estadisticas_evaluaciones.pdf");
    }

    /**
     * Reporte consolidado de defensas por carrera vía sp_generar_reporte_defensas
     * (Fase 3 / Criterio P1, categoría "consultas multi-tabla"). @Transactional es
     * necesario aquí: el procedimiento devuelve un REFCURSOR y Postgres solo lo mantiene
     * abierto dentro de la misma transacción que lo abrió -- sin esto, Hibernate hace el
     * fetch del cursor en una transacción/conexión ya cerrada ("cursor ... does not exist").
     *
     * @param carrera nombre o parte del nombre de la carrera por la que se filtra
     * @return 200 con las filas del reporte que devuelve el procedimiento
     */
    @GetMapping("/defensas")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<ec.edu.uteq.presustentaciones.dto.ReporteDefensaResult>> reporteDefensas(
            @RequestParam String carrera) {
        return ResponseEntity.ok(solicitudRepo.generarReporteDefensas(carrera));
    }

    /**
     * RF-11: Mismas estadísticas que el PDF pero en JSON, para las gráficas del dashboard.
     *
     * @return 200 con totales, aprobados, reprobados, nota promedio, tasa de aprobación y
     *         solicitudes pendientes; la tasa es 0 cuando todavía no hay evaluaciones
     */
    @GetMapping("/estadisticas/json")
    public ResponseEntity<Map<String, Object>> estadisticasJson() {
        List<EvaluacionFinal> evals = evaluacionFinalRepo.findAllWithRelationships();
        long total      = evals.size();
        long aprobados  = evals.stream().filter(e -> e.getResultado() != null && "APROBADO".equals(e.getResultado().getCodigo())).count();
        long reprobados = evals.stream().filter(e -> e.getResultado() != null && "REPROBADO".equals(e.getResultado().getCodigo())).count();
        double promedio = evals.stream()
                .mapToDouble(e -> e.getNotaFinal() != null ? e.getNotaFinal() : 0)
                .filter(n -> n > 0).average().orElse(0);
        long pendientes = solicitudRepo.countByEstadoCodigo("APROBADA");

        return ResponseEntity.ok(Map.of(
                "totalEvaluados",       total,
                "aprobados",            aprobados,
                "reprobados",           reprobados,
                "notaPromedio",         Math.round(promedio * 100.0) / 100.0,
                "tasaAprobacion",       total > 0 ? Math.round((double) aprobados / total * 100) : 0,
                "solicitudesPendientes", pendientes
        ));
    }

    // ── Módulo de reportes JSON (COORDINADOR / ADMINISTRADOR) ─────────────────
    // El @PreAuthorize('REPORTES_VER') de la clase protege también estos endpoints.
    // Todo se agrega con COUNT/GROUP BY en la base (ver ReporteServiceImpl).

    /**
     * Resumen general del proceso de pre-sustentaciones para el dashboard.
     *
     * @param desde   inicio del rango de fechas, opcional
     * @param hasta   fin del rango de fechas, opcional
     * @param carrera filtro por carrera, opcional
     * @return 200 con el resumen agregado
     */
    @GetMapping("/resumen")
    public ResponseEntity<?> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String carrera) {
        return ResponseEntity.ok(reporteService.resumen(desde, hasta, carrera));
    }

    /**
     * Cantidad de solicitudes/pre-sustentaciones agrupadas por estado.
     *
     * @param desde   inicio del rango de fechas, opcional
     * @param hasta   fin del rango de fechas, opcional
     * @param carrera filtro por carrera, opcional
     * @return 200 con el conteo por estado
     */
    @GetMapping("/solicitudes-por-estado")
    public ResponseEntity<?> solicitudesPorEstado(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String carrera) {
        return ResponseEntity.ok(reporteService.solicitudesPorEstado(desde, hasta, carrera));
    }

    /**
     * Cantidad de pre-sustentaciones agrupadas por período académico.
     *
     * @param desde inicio del rango de fechas, opcional
     * @param hasta fin del rango de fechas, opcional
     * @return 200 con el conteo por período
     */
    @GetMapping("/sustentaciones-por-periodo")
    public ResponseEntity<?> sustentacionesPorPeriodo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.sustentacionesPorPeriodo(desde, hasta));
    }

    /**
     * Estado de las actas: generadas, revisadas, observadas, finalizadas, anuladas y
     * pendientes de firma.
     *
     * @param desde inicio del rango de fechas, opcional
     * @param hasta fin del rango de fechas, opcional
     * @return 200 con el conteo por estado de acta
     */
    @GetMapping("/actas")
    public ResponseEntity<?> resumenActas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.resumenActas(desde, hasta));
    }

    /**
     * Actividad por docente: participaciones como jurado, como tutor y actas firmadas.
     *
     * @return 200 con una fila por docente
     */
    @GetMapping("/actividad-docente")
    public ResponseEntity<?> actividadDocente() {
        return ResponseEntity.ok(reporteService.actividadPorDocente());
    }

    /**
     * Estadísticas por carrera/programa: total, completadas y rechazadas.
     *
     * @return 200 con una fila por carrera
     */
    @GetMapping("/por-carrera")
    public ResponseEntity<?> porCarrera() {
        return ResponseEntity.ok(reporteService.estadisticasPorCarrera());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Document abrirDoc(ByteArrayOutputStream baos) throws Exception {
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
        return new Document(pdf);
    }

    private void encabezado(Document doc, PdfFont bold, PdfFont regular,
                            String titulo, String subtitulo) throws Exception {
        doc.add(new Paragraph("UNIVERSIDAD TÉCNICA ESTATAL DE QUEVEDO")
                .setFont(bold).setFontSize(13).setFontColor(BLUE())
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph(titulo)
                .setFont(bold).setFontSize(11).setFontColor(GOLD())
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph(subtitulo)
                .setFont(regular).setFontSize(9).setFontColor(GRAY_TEXT())
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Generado: " + LocalDateTime.now().format(FMT))
                .setFont(regular).setFontSize(8).setFontColor(GRAY_TEXT())
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(16));
    }

    private Cell celda(String txt, PdfFont font, DeviceRgb bg, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(txt).setFont(font).setFontSize(8))
                .setBackgroundColor(bg).setTextAlignment(align);
    }

    private void celdaStat(Table t, String label, String val,
                           PdfFont bold, PdfFont regular, DeviceRgb color) {
        t.addCell(new Cell()
                .add(new Paragraph(val).setFont(bold).setFontSize(20)
                        .setFontColor(color).setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph(label).setFont(regular).setFontSize(8)
                        .setFontColor(DARK_TEXT()).setTextAlignment(TextAlignment.CENTER))
                .setPadding(10));
    }

    private ResponseEntity<byte[]> pdfResponse(ByteArrayOutputStream baos, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(baos.toByteArray());
    }

    private String fmt(Double v) { return v != null ? String.format("%.2f", v) : "—"; }
}
