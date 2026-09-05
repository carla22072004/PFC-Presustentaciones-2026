package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Acta;
import ec.edu.uteq.presustentaciones.services.ActaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import ec.edu.uteq.presustentaciones.dto.CambiarEstadoActaRequest;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/actas")
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

    // ── Módulo 2: gestión e historial de actas ──────────────────────────────

    /** DOCENTE: actas de las pre-sustentaciones en las que es tutor o jurado. */
    @GetMapping("/mis-actas")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ACTAS_VER_PROPIAS')")
    public ResponseEntity<?> misActas(Authentication auth, Pageable pageable) {
        return ResponseEntity.ok(ResponseWrapper.success(actaService.listarMisActas(auth.getName(), pageable)));
    }

    /**
     * COORDINADOR / ADMINISTRADOR: búsqueda y filtrado de todas las actas (permiso ACTAS_VER).
     * El coordinador consulta y cambia estado según el flujo académico; ACTAS_GESTIONAR
     * (solo ADMIN) queda reservado para operaciones administrativas adicionales.
     */
    @GetMapping("/buscar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ACTAS_VER')")
    public ResponseEntity<?> buscar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String carrera,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(ResponseWrapper.success(
                actaService.buscarActas(estado, carrera, desde, hasta, q, pageable)));
    }

    /** Detalle de un acta. El service aplica el control de acceso (previene IDOR). */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> detalle(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(actaService.obtenerDetalle(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /** Historial de trazabilidad (timeline) del acta. Mismo control de acceso que el detalle. */
    @GetMapping("/{id}/historial")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ACTA_HISTORIAL_VER')")
    public ResponseEntity<?> historial(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ResponseWrapper.success(actaService.obtenerHistorial(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /** COORDINADOR / ADMINISTRADOR: cambia el estado del acta (queda en el historial). */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ACTA_ESTADO_CAMBIAR')")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                           @Valid @RequestBody CambiarEstadoActaRequest req) {
        try {
            Acta acta = actaService.cambiarEstado(id, req.getNuevoEstado(), req.getMotivo());
            return ResponseEntity.ok(ResponseWrapper.success(acta, "Estado del acta actualizado"));
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

    /**
     * Hallazgo real de auditoría (2026-09-04): usaba isAuthenticated() a secas, así que
     * ActaServiceImpl.eliminarActa() (que reutiliza validarAcceso(), pensado para LECTURA:
     * admin, jurado, tutor o el propio estudiante dueño) terminaba autorizando también el
     * borrado permanente del acta a cualquiera de esos participantes -- un estudiante podía
     * eliminar el acta oficial de su propia defensa, o un docente jurado/tutor sin ningún
     * permiso administrativo. Se exige ACTAS_GESTIONAR (hoy solo ADMIN), igual que el resto
     * de acciones administrativas de este controlador (generar/firmar/cambiarEstado).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ACTAS_GESTIONAR')")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            actaService.eliminarActa(id);
            return ResponseEntity.ok(ResponseWrapper.success(null, "Acta eliminada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }
}
