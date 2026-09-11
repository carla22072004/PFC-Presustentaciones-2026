package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Anteproyecto;
import ec.edu.uteq.presustentaciones.services.AnteproyectoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public AnteproyectoController(AnteproyectoService s) {
        this.anteproyectoService = s;
    }

    /**
     * RF-02: Sube el PDF del anteproyecto de una solicitud. El servicio calcula y guarda el
     * SHA-256 del archivo, que despues permite verificar que no fue alterado en disco.
     *
     * @param solicitudId solicitud a la que pertenece el anteproyecto
     * @param archivo     PDF enviado como multipart
     * @return 200 con el anteproyecto registrado
     */
    @PostMapping(value = "/enviar/{solicitudId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Anteproyecto> enviar(@PathVariable Long solicitudId,
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(anteproyectoService.enviarAnteproyecto(solicitudId, archivo));
    }

    /**
     * @param solicitudId solicitud consultada
     * @return 200 con el anteproyecto de esa solicitud, o el error si aun no se subio
     */
    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<?> obtenerPorSolicitud(@PathVariable Long solicitudId) {
        return anteproyectoService.buscarPorSolicitud(solicitudId)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Descarga en linea el PDF del anteproyecto.
     *
     * @param solicitudId solicitud cuyo anteproyecto se abre
     * @return 200 con el PDF, o el estado de error que devuelva el servicio
     */
    @GetMapping("/ver/{solicitudId}")
    public ResponseEntity<Resource> verPdf(@PathVariable Long solicitudId) {
        Anteproyecto ap = anteproyectoService.buscarPorSolicitud(solicitudId)
                .orElseThrow(() -> new RuntimeException("Anteproyecto no encontrado"));
        try {
            Path ruta = Paths.get(uploadDir).resolve(ap.getArchivoPdf()).normalize();
            Resource resource = new UrlResource(ruta.toUri());
            if (!resource.exists() || !resource.isReadable())
                return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + ap.getArchivoPdf() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * RF-02: Recalcula el SHA-256 del archivo en disco y lo compara con el registrado al
     * subirlo, para detectar alteraciones posteriores.
     *
     * @param solicitudId solicitud cuyo anteproyecto se verifica
     * @return 200 con el resultado de la comparacion de hashes
     */
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
                            : "⚠ Advertencia: el archivo puede haber sido modificado."));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // deja que GlobalExceptionHandler lo traduzca a 403, no a 400
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Aprueba el anteproyecto dejando constancia de las observaciones del revisor.
     *
     * @param id            anteproyecto a aprobar
     * @param observaciones comentario del revisor
     * @return 200 con el anteproyecto aprobado
     */
    @PostMapping("/aprobar/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ANTEPROYECTO_REVISAR')")
    public ResponseEntity<Anteproyecto> aprobar(@PathVariable Long id, @RequestParam String observaciones) {
        return ResponseEntity.ok(anteproyectoService.aprobarAnteproyecto(id, observaciones));
    }

    /**
     * Rechaza el anteproyecto indicando que debe corregirse.
     *
     * @param id            anteproyecto a rechazar
     * @param observaciones motivo del rechazo, visible para el estudiante
     * @return 200 con el anteproyecto rechazado
     */
    @PostMapping("/rechazar/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ANTEPROYECTO_REVISAR')")
    public ResponseEntity<Anteproyecto> rechazar(@PathVariable Long id, @RequestParam String observaciones) {
        return ResponseEntity.ok(anteproyectoService.rechazarAnteproyecto(id, observaciones));
    }
}
