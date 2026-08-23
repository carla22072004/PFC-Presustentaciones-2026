package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Acta;
import ec.edu.uteq.presustentaciones.services.ActaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/actas")
public class ActaController {

    private final ActaService actaService;

    public ActaController(ActaService actaService) {
        this.actaService = actaService;
    }

    /** RF-11: Genera el acta con PDF real */
    @PostMapping("/generar/{solicitudId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ACTA_GENERAR')")
    public ResponseEntity<?> generarActa(@PathVariable Long solicitudId) {
        try {
            Acta acta = actaService.generarActa(solicitudId);
            return ResponseEntity.ok(acta);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * RF-08: Firma el acta por rol específico.
     * rol: PRESIDENTE, VOCAL_1, VOCAL_2, TUTOR
     */
    @PostMapping("/firmar/{actaId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ACTA_FIRMAR')")
    public ResponseEntity<?> firmarActa(
            @PathVariable Long actaId,
            @RequestParam String rol,
            @RequestParam(required = false) String observacion) {
        try {
            Acta acta = actaService.firmarActa(actaId, rol, observacion);
            return ResponseEntity.ok(acta);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** RF-11: Descarga el PDF del acta */
    @GetMapping("/descargar/{actaId}")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long actaId) {
        try {
            byte[] pdfBytes = actaService.obtenerPdfBytes(actaId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"acta_" + actaId + ".pdf\"")
                    .body(pdfBytes);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Vista en línea del PDF (en navegador) */
    @GetMapping("/ver/{actaId}")
    public ResponseEntity<byte[]> verPdf(@PathVariable Long actaId) {
        try {
            byte[] pdfBytes = actaService.obtenerPdfBytes(actaId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"acta_" + actaId + ".pdf\"")
                    .body(pdfBytes);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public Page<Acta> listar(Pageable pageable) {
        return actaService.listarActas(pageable);
    }

    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<Acta> porSolicitud(@PathVariable Long solicitudId) {
        return actaService.buscarPorSolicitud(solicitudId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
