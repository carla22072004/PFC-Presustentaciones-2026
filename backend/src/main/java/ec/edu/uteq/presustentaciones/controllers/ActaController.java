package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Acta;
import ec.edu.uteq.presustentaciones.services.ActaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
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
            return ResponseEntity.ok(ResponseWrapper.success(acta, "Acta generada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
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
            return ResponseEntity.ok(ResponseWrapper.success(acta, "Acta firmada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /** RF-11: Descarga el PDF del acta */
    @GetMapping("/descargar/{actaId}")
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ACTAS_VER')")
    public ResponseEntity<?> listar(Pageable pageable) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(actaService.listarActas(pageable)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @GetMapping("/solicitud/{solicitudId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> porSolicitud(@PathVariable Long solicitudId) {
        try {
            return actaService.buscarPorSolicitud(solicitudId)
                    .map(acta -> ResponseEntity.ok(ResponseWrapper.success(acta)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }
}
