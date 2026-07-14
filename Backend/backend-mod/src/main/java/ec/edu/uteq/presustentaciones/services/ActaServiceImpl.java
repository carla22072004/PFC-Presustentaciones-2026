package ec.edu.uteq.presustentaciones.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.constants.StandardFonts;
import ec.edu.uteq.presustentaciones.entities.Acta;
import ec.edu.uteq.presustentaciones.entities.Evaluacion;
import ec.edu.uteq.presustentaciones.entities.Jurado;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.enums.EstadoSolicitud;
import ec.edu.uteq.presustentaciones.repositories.ActaRepository;
import ec.edu.uteq.presustentaciones.repositories.EvaluacionRepository;
import ec.edu.uteq.presustentaciones.repositories.JuradoRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActaServiceImpl implements ActaService {

    private final ActaRepository actaRepository;
    private final SolicitudRepository solicitudRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final JuradoRepository juradoRepository;

    @Value("${app.actas.dir:uploads/actas}")
    private String actasDir;

    // ── Colores institucionales UTEQ ─────────────────────────────────────────
    private static final DeviceRgb UTEQ_BLUE    = new DeviceRgb(0, 56, 101);
    private static final DeviceRgb UTEQ_GOLD    = new DeviceRgb(204, 153, 0);
    private static final DeviceRgb LIGHT_GRAY   = new DeviceRgb(245, 245, 245);
    private static final DeviceRgb MEDIUM_GRAY  = new DeviceRgb(200, 200, 200);

    @Override
    public Acta generarActa(Long solicitudId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + solicitudId));

        // Buscar evaluación y jurados
        Optional<Evaluacion> evalOpt = evaluacionRepository.findBySolicitudId(solicitudId);
        List<Jurado> jurados = juradoRepository.findBySolicitudId(solicitudId);

        // Crear directorio si no existe
        try {
            Path dir = Paths.get(actasDir);
            if (!Files.exists(dir)) Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear directorio de actas: " + e.getMessage());
        }

        String nombreArchivo = "acta_" + solicitudId + "_" + System.currentTimeMillis() + ".pdf";
        String rutaCompleta = actasDir + "/" + nombreArchivo;

        // Generar PDF con iText
        generarPdf(rutaCompleta, solicitud, evalOpt.orElse(null), jurados);

        Acta acta = actaRepository.findBySolicitudId(solicitudId)
                .orElse(Acta.builder()
                        .solicitud(solicitud)
                        .fechaGeneracion(LocalDate.now())
                        .build());
        acta.setArchivoPdf(nombreArchivo);
        acta.setFechaGeneracion(LocalDate.now());
        return actaRepository.save(acta);
    }

    @Override
    public Acta firmarActa(Long actaId, String rol) {
        Acta acta = actaRepository.findById(actaId)
                .orElseThrow(() -> new RuntimeException("Acta no encontrada: " + actaId));

        LocalDateTime ahora = LocalDateTime.now();
        switch (rol.toUpperCase()) {
            case "PRESIDENTE" -> { acta.setFirmadaPresidente(true); acta.setFechaFirmaPresidente(ahora); }
            case "VOCAL_1"    -> { acta.setFirmadaVocal1(true);     acta.setFechaFirmaVocal1(ahora); }
            case "VOCAL_2"    -> { acta.setFirmadaVocal2(true);     acta.setFechaFirmaVocal2(ahora); }
            case "TUTOR"      -> { acta.setFirmadaTutor(true);      acta.setFechaFirmaTutor(ahora); }
            default -> throw new RuntimeException("Rol inválido: " + rol + ". Use: PRESIDENTE, VOCAL_1, VOCAL_2, TUTOR");
        }

        acta.actualizarEstadoFirma();

        // Si el acta quedó completamente firmada, cambiar estado a COMPLETADA y regenerar PDF
        if (acta.isFirmada()) {
            Solicitud solicitud = acta.getSolicitud();
            solicitud.setEstado(EstadoSolicitud.COMPLETADA);
            solicitudRepository.save(solicitud);
            log.info("Solicitud {} completada - todas las firmas del acta han sido aplicadas", solicitud.getId());

            if (acta.getArchivoPdf() != null) {
                Optional<Evaluacion> evalOpt = evaluacionRepository.findBySolicitudId(solicitud.getId());
                List<Jurado> jurados = juradoRepository.findBySolicitudId(solicitud.getId());
                String rutaCompleta = actasDir + "/" + acta.getArchivoPdf();
                generarPdf(rutaCompleta, solicitud, evalOpt.orElse(null), jurados, acta);
            }
        }

        return actaRepository.save(acta);
    }

    @Override
    public byte[] obtenerPdfBytes(Long actaId) {
        Acta acta = actaRepository.findById(actaId)
                .orElseThrow(() -> new RuntimeException("Acta no encontrada"));
        if (acta.getArchivoPdf() == null) {
            throw new RuntimeException("El acta no tiene PDF generado aún.");
        }
        Path path = Paths.get(actasDir, acta.getArchivoPdf());
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el PDF: " + e.getMessage());
        }
    }

    @Override
    public List<Acta> listarActas() {
        return actaRepository.findAll();
    }

    @Override
    public Optional<Acta> buscarPorSolicitud(Long solicitudId) {
        return actaRepository.findBySolicitudId(solicitudId);
    }

    // ── Generación PDF ────────────────────────────────────────────────────────

    private void generarPdf(String ruta, Solicitud solicitud, Evaluacion evaluacion, List<Jurado> jurados) {
        generarPdf(ruta, solicitud, evaluacion, jurados, null);
    }

    private void generarPdf(String ruta, Solicitud solicitud, Evaluacion evaluacion,
                             List<Jurado> jurados, Acta acta) {
        try {
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            PdfWriter writer     = new PdfWriter(ruta);
            PdfDocument pdfDoc   = new PdfDocument(writer);
            Document document    = new Document(pdfDoc);
            document.setMargins(40, 50, 40, 50);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter fmtDt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // ── Encabezado ────────────────────────────────────────────────────
            Table header = new Table(UnitValue.createPercentArray(new float[]{20f, 60f, 20f}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Logo placeholder (azul UTEQ)
            Cell logoCell = new Cell()
                    .add(new Paragraph("UTEQ").setFont(fontBold).setFontSize(18)
                            .setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(UTEQ_BLUE)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(15);
            header.addCell(logoCell);

            // Título central
            Cell titleCell = new Cell()
                    .add(new Paragraph("ACTA DE PRE-SUSTENTACIÓN")
                            .setFont(fontBold).setFontSize(14).setFontColor(UTEQ_BLUE)
                            .setTextAlignment(TextAlignment.CENTER))
                    .add(new Paragraph("Universidad Técnica Estatal de Quevedo")
                            .setFont(fontRegular).setFontSize(9).setFontColor(ColorConstants.DARK_GRAY)
                            .setTextAlignment(TextAlignment.CENTER))
                    .add(new Paragraph("Facultad de Ciencias de la Computación y Diseño Digital")
                            .setFont(fontRegular).setFontSize(8).setFontColor(ColorConstants.DARK_GRAY)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBorder(Border.NO_BORDER).setPadding(10);
            header.addCell(titleCell);

            // Número de acta
            Cell numCell = new Cell()
                    .add(new Paragraph("No. " + solicitud.getId())
                            .setFont(fontBold).setFontSize(12).setFontColor(UTEQ_GOLD)
                            .setTextAlignment(TextAlignment.CENTER))
                    .add(new Paragraph(LocalDate.now().format(fmt))
                            .setFont(fontRegular).setFontSize(9)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(LIGHT_GRAY).setBorder(Border.NO_BORDER).setPadding(10);
            header.addCell(numCell);
            document.add(header);

            // Línea separadora dorada
            document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(3f))
                    .setStrokeColor(UTEQ_GOLD));
            document.add(new Paragraph("\n").setMargin(2));

            // ── Datos del estudiante ──────────────────────────────────────────
            document.add(sectionTitle("1. DATOS DEL ESTUDIANTE", fontBold));
            Table datosEstudiante = new Table(UnitValue.createPercentArray(new float[]{30f, 70f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(10);
            String nombreEst = solicitud.getEstudiante() != null && solicitud.getEstudiante().getUsuario() != null
                    ? solicitud.getEstudiante().getUsuario().getNombre() + " "
                      + solicitud.getEstudiante().getUsuario().getApellido()
                    : "—";
            addRow(datosEstudiante, "Estudiante:", nombreEst, fontBold, fontRegular);
            addRow(datosEstudiante, "Carrera:", solicitud.getEstudiante() != null
                    ? nvl(solicitud.getEstudiante().getCarrera()) : "—", fontBold, fontRegular);
            addRow(datosEstudiante, "Título del tema:", nvl(solicitud.getTituloTema()), fontBold, fontRegular);
            addRow(datosEstudiante, "Modalidad:", nvl(solicitud.getModalidad()), fontBold, fontRegular);
            addRow(datosEstudiante, "Fecha de solicitud:",
                    solicitud.getFechaRegistro() != null ? solicitud.getFechaRegistro().format(fmtDt) : "—",
                    fontBold, fontRegular);
            document.add(datosEstudiante);

            // ── Tribunal ───────────────────────────────────────────────────────
            document.add(sectionTitle("2. TRIBUNAL EVALUADOR", fontBold));
            Table tribunal = new Table(UnitValue.createPercentArray(new float[]{40f, 40f, 20f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(10);
            addHeaderRow(tribunal, new String[]{"Docente", "Rol", "Confirmado"}, fontBold);
            if (jurados.isEmpty()) {
                Cell noJurados = new Cell(1, 3)
                        .add(new Paragraph("No hay jurados asignados").setFont(fontRegular).setFontSize(9))
                        .setTextAlignment(TextAlignment.CENTER).setPadding(8).setBackgroundColor(LIGHT_GRAY);
                tribunal.addCell(noJurados);
            } else {
                for (Jurado j : jurados) {
                    String docNombre = j.getDocente() != null && j.getDocente().getUsuario() != null
                            ? j.getDocente().getUsuario().getNombre() + " " + j.getDocente().getUsuario().getApellido()
                            : "—";
                    tribunal.addCell(dataCell(docNombre, fontRegular));
                    tribunal.addCell(dataCell(j.getRol(), fontRegular));
                    tribunal.addCell(dataCell(j.isConfirmado() ? "✓" : "Pendiente", fontRegular));
                }
            }
            document.add(tribunal);

            // ── Evaluación y calificación ────────────────────────────────────
            document.add(sectionTitle("3. EVALUACIÓN Y CALIFICACIÓN", fontBold));
            if (evaluacion != null) {
                Table evalTable = new Table(UnitValue.createPercentArray(new float[]{50f, 25f, 25f}))
                        .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(10);
                addHeaderRow(evalTable, new String[]{"Concepto", "Peso (%)", "Nota"}, fontBold);
                evalTable.addCell(dataCell("Instructor del curso (Titulación II)", fontRegular));
                evalTable.addCell(dataCell(String.format("%.0f%%", evalTable != null ? evaluacion.getPesoInstructor() : 60.0), fontRegular));
                evalTable.addCell(dataCell(evaluacion.getNotaInstructor() != null
                        ? String.format("%.2f", evaluacion.getNotaInstructor()) : "—", fontRegular));

                evalTable.addCell(dataCell("Tribunal evaluador", fontRegular));
                evalTable.addCell(dataCell(String.format("%.0f%%", evaluacion.getPesoJurado()), fontRegular));
                evalTable.addCell(dataCell(evaluacion.getNotaJurado() != null
                        ? String.format("%.2f", evaluacion.getNotaJurado()) : "—", fontRegular));

                // Fila de total
                Cell totalLabel = new Cell().add(new Paragraph("NOTA FINAL").setFont(fontBold).setFontSize(10))
                        .setBackgroundColor(UTEQ_BLUE).setFontColor(ColorConstants.WHITE)
                        .setPadding(6).setBorder(Border.NO_BORDER);
                Cell totalPeso = new Cell().add(new Paragraph("100%").setFont(fontBold).setFontSize(10)
                        .setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(UTEQ_BLUE).setPadding(6).setBorder(Border.NO_BORDER);
                Cell totalNota = new Cell().add(new Paragraph(evaluacion.getNotaFinal() != null
                        ? String.format("%.2f / 10", evaluacion.getNotaFinal()) : "—")
                        .setFont(fontBold).setFontSize(10).setFontColor(UTEQ_GOLD))
                        .setBackgroundColor(UTEQ_BLUE).setPadding(6).setBorder(Border.NO_BORDER);
                evalTable.addCell(totalLabel);
                evalTable.addCell(totalPeso);
                evalTable.addCell(totalNota);
                document.add(evalTable);

                // Resultado
                String resultado = nvl(evaluacion.getResultado());
                DeviceRgb resultColor = "APROBADO".equals(resultado)
                        ? new DeviceRgb(0, 128, 0) : new DeviceRgb(180, 0, 0);
                document.add(new Paragraph("RESULTADO: " + resultado)
                        .setFont(fontBold).setFontSize(14).setFontColor(resultColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBorder(new SolidBorder(resultColor, 2)).setPadding(8).setMarginBottom(10));

                if (evaluacion.getObservaciones() != null && !evaluacion.getObservaciones().isBlank()) {
                    document.add(sectionTitle("Observaciones del tribunal:", fontBold));
                    document.add(new Paragraph(evaluacion.getObservaciones())
                            .setFont(fontRegular).setFontSize(9).setBackgroundColor(LIGHT_GRAY)
                            .setPadding(8).setMarginBottom(10));
                }
            } else {
                document.add(new Paragraph("Evaluación pendiente de registro.")
                        .setFont(fontRegular).setFontSize(9).setFontColor(ColorConstants.GRAY));
            }

            // ── Firmas ────────────────────────────────────────────────────────
            document.add(sectionTitle("4. FIRMAS ELECTRÓNICAS", fontBold));
            Table firmasTable = new Table(UnitValue.createPercentArray(new float[]{25f, 25f, 25f, 25f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(15);

            String[] rolesLabel = {"Presidente", "Vocal 1", "Vocal 2", "Tutor"};
            boolean[] firmados  = {
                acta != null && acta.isFirmadaPresidente(),
                acta != null && acta.isFirmadaVocal1(),
                acta != null && acta.isFirmadaVocal2(),
                acta != null && acta.isFirmadaTutor()
            };
            LocalDateTime[] fechasFirma = {
                acta != null ? acta.getFechaFirmaPresidente() : null,
                acta != null ? acta.getFechaFirmaVocal1() : null,
                acta != null ? acta.getFechaFirmaVocal2() : null,
                acta != null ? acta.getFechaFirmaTutor() : null
            };

            for (int i = 0; i < 4; i++) {
                boolean firmado = firmados[i];
                Cell firmaCell = new Cell()
                        .add(new Paragraph(rolesLabel[i]).setFont(fontBold).setFontSize(9)
                                .setTextAlignment(TextAlignment.CENTER))
                        .add(new Paragraph(firmado ? "✓ FIRMADO" : "PENDIENTE")
                                .setFont(fontBold).setFontSize(10)
                                .setFontColor(firmado ? new DeviceRgb(0, 128, 0) : new DeviceRgb(150, 150, 150))
                                .setTextAlignment(TextAlignment.CENTER))
                        .add(new Paragraph(firmado && fechasFirma[i] != null
                                ? fechasFirma[i].format(fmtDt) : " ")
                                .setFont(fontRegular).setFontSize(7)
                                .setTextAlignment(TextAlignment.CENTER))
                        .setBackgroundColor(firmado ? new DeviceRgb(230, 255, 230) : LIGHT_GRAY)
                        .setBorder(new SolidBorder(firmado ? new DeviceRgb(0, 128, 0) : MEDIUM_GRAY, 1))
                        .setPadding(10).setMargin(3);
                firmasTable.addCell(firmaCell);
            }
            document.add(firmasTable);

            // ── Pie de página ─────────────────────────────────────────────────
            document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f))
                    .setStrokeColor(UTEQ_GOLD));
            document.add(new Paragraph("Generado el " + LocalDateTime.now().format(fmtDt)
                    + " | Sistema de Gestión de Pre-Sustentaciones UTEQ | Documento oficial")
                    .setFont(fontRegular).setFontSize(7).setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();

        } catch (IOException e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    // ── Helpers de construcción de tablas ─────────────────────────────────────

    private Paragraph sectionTitle(String text, PdfFont fontBold) {
        return new Paragraph(text).setFont(fontBold).setFontSize(10)
                .setFontColor(UTEQ_BLUE)
                .setBorderBottom(new SolidBorder(UTEQ_GOLD, 1.5f))
                .setMarginTop(8).setMarginBottom(4);
    }

    private void addRow(Table table, String label, String value, PdfFont fontBold, PdfFont fontRegular) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(fontBold).setFontSize(9))
                .setBackgroundColor(LIGHT_GRAY).setBorder(Border.NO_BORDER).setPadding(5));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFont(fontRegular).setFontSize(9))
                .setBorder(Border.NO_BORDER).setPadding(5));
    }

    private void addHeaderRow(Table table, String[] headers, PdfFont fontBold) {
        for (String h : headers) {
            table.addCell(new Cell()
                    .add(new Paragraph(h).setFont(fontBold).setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(UTEQ_BLUE).setPadding(6).setBorder(Border.NO_BORDER));
        }
    }

    private Cell dataCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9))
                .setPadding(5)
                .setBorderBottom(new SolidBorder(MEDIUM_GRAY, 0.5f))
                .setBorderTop(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER);
    }

    private String nvl(String s) {
        return s != null ? s : "—";
    }
}
