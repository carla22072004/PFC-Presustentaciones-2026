package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.BackupInfoDTO;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import ec.edu.uteq.presustentaciones.services.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Apartado "Gestión de Respaldos de Base de Datos" del administrador. Permite generar
 * bajo demanda, listar, descargar, restaurar y eliminar respaldos completos de la base
 * (dumps {@code pg_dump -Fc}). Todo el controlador exige el permiso {@code BACKUPS_GESTIONAR}
 * (V27), que por defecto solo tiene el rol ADMIN.
 *
 * <p>Igual que el resto de controladores, el prefijo real es {@code /api/v1/backups}
 * (lo añade {@code CustomWebMvcRegistrations}).
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@PreAuthorize("@permisoService.tienePermiso(authentication, 'BACKUPS_GESTIONAR')")
public class BackupController {

    private final BackupService backupService;

    /**
     * @return 200 con la lista de respaldos existentes, del más reciente al más antiguo
     */
    @GetMapping
    public ResponseEntity<?> listar() {
        List<BackupInfoDTO> respaldos = backupService.listar();
        return ResponseEntity.ok(ResponseWrapper.success(respaldos));
    }

    /**
     * Genera un respaldo nuevo con la fecha y hora actuales.
     *
     * @return 200 con los metadatos del respaldo recién creado, o 400/409 con el motivo
     *         si {@code pg_dump} falla o no está disponible
     */
    @PostMapping
    public ResponseEntity<?> generar() {
        BackupInfoDTO info = backupService.generar();
        return ResponseEntity.ok(ResponseWrapper.success(info, "Respaldo generado correctamente"));
    }

    /**
     * Descarga un respaldo como archivo adjunto.
     *
     * @param nombre nombre exacto del archivo de respaldo
     * @return 200 con el archivo (.dump), o 400 si el nombre es inválido o no existe
     */
    @GetMapping("/{nombre}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable String nombre) {
        byte[] contenido = backupService.leer(nombre);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(contenido);
    }

    /**
     * Restaura la base de datos a partir de un respaldo. Operación destructiva: reemplaza
     * el contenido actual por el del dump.
     *
     * @param nombre nombre exacto del archivo de respaldo
     * @return 200 al terminar, o 400 con el motivo si el respaldo no existe o falla la restauración
     */
    @PostMapping("/{nombre}/restaurar")
    public ResponseEntity<?> restaurar(@PathVariable String nombre) {
        backupService.restaurar(nombre);
        return ResponseEntity.ok(ResponseWrapper.success(null,
                "Base de datos restaurada desde el respaldo. Se recomienda reiniciar el backend "
                + "para descartar datos en caché."));
    }

    /**
     * @param nombre nombre exacto del archivo de respaldo a eliminar
     * @return 200 al eliminar, o 400 si el nombre es inválido o no existe
     */
    @DeleteMapping("/{nombre}")
    public ResponseEntity<?> eliminar(@PathVariable String nombre) {
        backupService.eliminar(nombre);
        return ResponseEntity.ok(ResponseWrapper.success(null, "Respaldo eliminado"));
    }
}
