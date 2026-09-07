package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.BackupInfoDTO;
import ec.edu.uteq.presustentaciones.dto.RegistrarPruebaRestauracionRequest;
import ec.edu.uteq.presustentaciones.dto.RespaldoConfigDTO;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import ec.edu.uteq.presustentaciones.services.BackupService;
import ec.edu.uteq.presustentaciones.services.WalPitrService;
import ec.edu.uteq.presustentaciones.services.backup.OrigenRespaldo;
import ec.edu.uteq.presustentaciones.services.backup.TipoRespaldo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Apartado "Gestión de Respaldos de Base de Datos" del administrador. Fase 1 del plan
 * (ver {@code docs/basedatos/PLAN-RESPALDOS-RECUPERACION.md}):
 * <ul>
 *   <li>Respaldo FULL bajo demanda + programado (cronograma cron editable).</li>
 *   <li>Retención automática GFS de las copias automáticas.</li>
 *   <li>Panel de estado y bitácora de pruebas de restauración.</li>
 * </ul>
 * Todo el controlador exige {@code BACKUPS_GESTIONAR} (V27), que por defecto solo tiene ADMIN.
 * Prefijo real: {@code /api/v1/backups} (lo añade {@code CustomWebMvcRegistrations}).
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@PreAuthorize("@permisoService.tienePermiso(authentication, 'BACKUPS_GESTIONAR')")
public class BackupController {

    private final BackupService backupService;
    private final WalPitrService walPitrService;

    // ── Copias ──────────────────────────────────────────────────────────────

    /** @return 200 con la lista de respaldos, del más reciente al más antiguo */
    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(ResponseWrapper.success(backupService.listar()));
    }

    /**
     * Genera un respaldo FULL ahora.
     *
     * @param origen opcional: MANUAL (por defecto) o EVENTO
     * @return 200 con los metadatos del respaldo, o 400/409 si {@code pg_dump} falla
     */
    @PostMapping
    public ResponseEntity<?> generar(@RequestParam(defaultValue = "MANUAL") String origen) {
        OrigenRespaldo o = "EVENTO".equalsIgnoreCase(origen) ? OrigenRespaldo.EVENTO : OrigenRespaldo.MANUAL;
        BackupInfoDTO info = backupService.generar(TipoRespaldo.FULL, o);
        return ResponseEntity.ok(ResponseWrapper.success(info, "Respaldo generado correctamente"));
    }

    /** Genera un respaldo DIFERENCIAL (filas cambiadas desde el último FULL). Fase 2. */
    @PostMapping("/diferencial")
    public ResponseEntity<?> generarDiferencial(@RequestParam(defaultValue = "MANUAL") String origen) {
        OrigenRespaldo o = "EVENTO".equalsIgnoreCase(origen) ? OrigenRespaldo.EVENTO : OrigenRespaldo.MANUAL;
        BackupInfoDTO info = backupService.generarDiferencial(o);
        return ResponseEntity.ok(ResponseWrapper.success(info, "Respaldo diferencial generado"));
    }

    /** Descarga un respaldo como archivo adjunto. */
    @GetMapping("/{nombre}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable String nombre) {
        byte[] contenido = backupService.leer(nombre);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(contenido);
    }

    /** Restaura la base desde un respaldo (operación destructiva). */
    @PostMapping("/{nombre}/restaurar")
    public ResponseEntity<?> restaurar(@PathVariable String nombre) {
        backupService.restaurar(nombre);
        return ResponseEntity.ok(ResponseWrapper.success(null,
                "Base de datos restaurada desde el respaldo. Se recomienda reiniciar el backend "
                + "para descartar datos en caché."));
    }

    /** Elimina permanentemente un archivo de respaldo. */
    @DeleteMapping("/{nombre}")
    public ResponseEntity<?> eliminar(@PathVariable String nombre) {
        backupService.eliminar(nombre);
        return ResponseEntity.ok(ResponseWrapper.success(null, "Respaldo eliminado"));
    }

    // ── Panel de estado ─────────────────────────────────────────────────────

    /** Resumen para el panel: última copia, próxima programada, espacio, RPO, última prueba. */
    @GetMapping("/estado")
    public ResponseEntity<?> estado() {
        return ResponseEntity.ok(ResponseWrapper.success(backupService.estado()));
    }

    // ── Cronograma (programación + retención) ────────────────────────────────

    @GetMapping("/config")
    public ResponseEntity<?> obtenerConfig() {
        return ResponseEntity.ok(ResponseWrapper.success(backupService.configDTO()));
    }

    /**
     * Actualiza el cronograma: activo/pausado, expresión cron y política de retención GFS.
     *
     * @return 200 con la config aplicada, o 400 si el cron es inválido
     */
    @PutMapping("/config")
    public ResponseEntity<?> actualizarConfig(@Valid @RequestBody RespaldoConfigDTO dto) {
        return ResponseEntity.ok(ResponseWrapper.success(
                backupService.actualizarConfig(dto), "Cronograma actualizado"));
    }

    /** Aplica la retención GFS ahora mismo. @return 200 con los nombres eliminados */
    @PostMapping("/retencion")
    public ResponseEntity<?> aplicarRetencion() {
        List<String> eliminados = backupService.aplicarRetencion();
        String msg = eliminados.isEmpty()
                ? "Retención aplicada: no había copias para eliminar."
                : "Retención aplicada: " + eliminados.size() + " copia(s) eliminada(s).";
        return ResponseEntity.ok(ResponseWrapper.success(eliminados, msg));
    }

    // ── Bitácora de pruebas de restauración ─────────────────────────────────

    @GetMapping("/pruebas")
    public ResponseEntity<?> listarPruebas() {
        return ResponseEntity.ok(ResponseWrapper.success(backupService.pruebas()));
    }

    @PostMapping("/pruebas")
    public ResponseEntity<?> registrarPrueba(@Valid @RequestBody RegistrarPruebaRestauracionRequest req) {
        return ResponseEntity.ok(ResponseWrapper.success(
                backupService.registrarPrueba(req.getRespaldoNombre(), req.getResultado(),
                        req.getResponsable(), req.getNotas()),
                "Prueba de restauración registrada"));
    }

    // ── Fase 2: WAL / PITR y base física ────────────────────────────────────

    /** Estado del archivado de WAL, del directorio compartido y de las bases físicas. */
    @GetMapping("/wal")
    public ResponseEntity<?> estadoWal() {
        return ResponseEntity.ok(ResponseWrapper.success(walPitrService.estado()));
    }

    /** Cierra el segmento de WAL actual para que se archive de inmediato. */
    @PostMapping("/wal/switch")
    public ResponseEntity<?> switchWal() {
        String wal = walPitrService.forzarSwitchWal();
        return ResponseEntity.ok(ResponseWrapper.success(wal, "Segmento " + wal + " cerrado y en cola de archivado"));
    }

    /** Limpia el WAL archivado más antiguo que la retención configurada. */
    @PostMapping("/wal/limpiar")
    public ResponseEntity<?> limpiarWal() {
        int dias = backupService.config().getRetenerDiasWal();
        int borrados = walPitrService.limpiarWal(dias);
        return ResponseEntity.ok(ResponseWrapper.success(borrados,
                borrados == 0 ? "No había WAL para limpiar." : borrados + " segmento(s) de WAL eliminados."));
    }

    /** Genera un respaldo físico base ({@code pg_basebackup}), la base para PITR. */
    @PostMapping("/bases")
    public ResponseEntity<?> generarBaseFisica() {
        return ResponseEntity.ok(ResponseWrapper.success(
                walPitrService.generarBaseFisica(), "Base física generada"));
    }

    @DeleteMapping("/bases/{nombre}")
    public ResponseEntity<?> eliminarBase(@PathVariable String nombre) {
        walPitrService.eliminarBase(nombre);
        return ResponseEntity.ok(ResponseWrapper.success(null, "Base física eliminada"));
    }
}
