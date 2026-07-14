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
import ec.edu.uteq.presustentaciones.entities.Evaluacion;
import ec.edu.uteq.presustentaciones.repositories.CronogramaRepository;
import ec.edu.uteq.presustentaciones.repositories.EvaluacionRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final CronogramaRepository cronogramaRepo;
    private final EvaluacionRepository evaluacionRepo;
    private final SolicitudRepository solicitudRepo;

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

    /** RF-11: PDF del cronograma de pre-sustentaciones */
    @GetMapping("/cronograma/pdf")
    public ResponseEntity<byte[]> reporteCronograma() throws Exception {
        List<Cronograma> lista = cronogramaRepo.findAll().stream()
                .filter(c -> "ACTIVO".equals(c.getEstado()))
                .sorted((a, b) -> a.getFechaInicio().compareTo(b.getFechaInicio()))
                .toList();

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
            table.addCell(celda(c.getEstado(), regular, bg, TextAlignment.CENTER));
        }
        doc.add(table);
        doc.add(new Paragraph("Total: " + lista.size() + " pre-sustentación(es) programadas.")
                .setFont(bold).setFontSize(9).setMarginTop(10));
        doc.close();

        return pdfResponse(baos, "cronograma_presustentaciones.pdf");
    }

    /** RF-11: PDF de estadísticas de evaluaciones */
    @GetMapping("/estadisticas/pdf")
    public ResponseEntity<byte[]> reporteEstadisticas() throws Exception {
        List<Evaluacion> evals = evaluacionRepo.findAll();

        long total      = evals.size();
        long aprobados  = evals.stream().filter(e -> "APROBADO".equals(e.getResultado())).count();
        long reprobados = evals.stream().filter(e -> "REPROBADO".equals(e.getResultado())).count();
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
        for (Evaluacion e : evals) {
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
            tabla.addCell(celda(fmt(e.getNotaJurado()),     regular, bg, TextAlignment.CENTER));
            tabla.addCell(celda(fmt(e.getNotaFinal()),      bold,    bg, TextAlignment.CENTER));
            // Color resultado: verde=aprobado, rojo=reprobado
            DeviceRgb rc = "APROBADO".equals(e.getResultado()) ? GREEN() : RED();
            tabla.addCell(new Cell()
                    .add(new Paragraph(e.getResultado() != null ? e.getResultado() : "—")
                            .setFont(bold).setFontSize(8).setFontColor(rc))
                    .setBackgroundColor(bg).setTextAlignment(TextAlignment.CENTER));
        }
        doc.add(tabla);
        doc.close();

        return pdfResponse(baos, "estadisticas_evaluaciones.pdf");
    }

    /** RF-11: JSON de estadísticas para gráficas */
    @GetMapping("/estadisticas/json")
    public ResponseEntity<Map<String, Object>> estadisticasJson() {
        List<Evaluacion> evals = evaluacionRepo.findAll();
        long total      = evals.size();
        long aprobados  = evals.stream().filter(e -> "APROBADO".equals(e.getResultado())).count();
        long reprobados = evals.stream().filter(e -> "REPROBADO".equals(e.getResultado())).count();
        double promedio = evals.stream()
                .mapToDouble(e -> e.getNotaFinal() != null ? e.getNotaFinal() : 0)
                .filter(n -> n > 0).average().orElse(0);
        long pendientes = solicitudRepo.countByEstado("APROBADA");

        return ResponseEntity.ok(Map.of(
                "totalEvaluados",       total,
                "aprobados",            aprobados,
                "reprobados",           reprobados,
                "notaPromedio",         Math.round(promedio * 100.0) / 100.0,
                "tasaAprobacion",       total > 0 ? Math.round((double) aprobados / total * 100) : 0,
                "solicitudesPendientes", pendientes
        ));
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
