package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Anteproyecto;
import ec.edu.uteq.presustentaciones.services.AnteproyectoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/anteproyectos")
public class AnteproyectoController {

    private final AnteproyectoService anteproyectoService;

    @Value("${app.upload.dir:uploads/anteproyectos}")
    private String uploadDir;

    public AnteproyectoController(AnteproyectoService s) { this.anteproyectoService = s; }

    @PostMapping(value = "/enviar/{solicitudId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Anteproyecto> enviar(@PathVariable Long solicitudId,
                                               @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(anteproyectoService.enviarAnteproyecto(solicitudId, archivo));
    }

    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<?> obtenerPorSolicitud(@PathVariable Long solicitudId) {
        return anteproyectoService.buscarPorSolicitud(solicitudId)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ver/{solicitudId}")
    public ResponseEntity<Resource> verPdf(@PathVariable Long solicitudId) {
        Anteproyecto ap = anteproyectoService.buscarPorSolicitud(solicitudId)
                .orElseThrow(() -> new RuntimeException("Anteproyecto no encontrado"));
        try {
            Path ruta = Paths.get(uploadDir).resolve(ap.getArchivoPdf()).normalize();
            Resource resource = new UrlResource(ruta.toUri());
            if (!resource.exists() || !resource.isReadable()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + ap.getArchivoPdf() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) { return ResponseEntity.internalServerError().build(); }
    }

    /** RF-02: Verificar integridad SHA-256 del archivo en disco */
    @GetMapping("/verificar/{solicitudId}")
    public ResponseEntity<Map<String, Object>> verificar(@PathVariable Long solicitudId) {
        try {
            boolean ok = anteproyectoService.verificarIntegridad(solicitudId);
            Anteproyecto ap = anteproyectoService.buscarPorSolicitud(solicitudId).orElseThrow();
            return ResponseEntity.ok(Map.of(
                "solicitudId", solicitudId,
                "integridadOk", ok,
                "sha256Registrado", ap.getSha256Hash() != null ? ap.getSha256Hash() : "—",
                "mensaje", ok ? "✓ Archivo íntegro: el hash SHA-256 coincide."
                              : "⚠ Advertencia: el archivo puede haber sido modificado."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/aprobar/{id}")
    public ResponseEntity<Anteproyecto> aprobar(@PathVariable Long id, @RequestParam String observaciones) {
        return ResponseEntity.ok(anteproyectoService.aprobarAnteproyecto(id, observaciones));
    }

    @PostMapping("/rechazar/{id}")
    public ResponseEntity<Anteproyecto> rechazar(@PathVariable Long id, @RequestParam String observaciones) {
        return ResponseEntity.ok(anteproyectoService.rechazarAnteproyecto(id, observaciones));
    }
}
