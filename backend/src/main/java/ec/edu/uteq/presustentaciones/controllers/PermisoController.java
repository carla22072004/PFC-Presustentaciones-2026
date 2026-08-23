package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Permiso;
import ec.edu.uteq.presustentaciones.entities.RolUsuario;
import ec.edu.uteq.presustentaciones.repositories.PermisoRepository;
import ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository;
import ec.edu.uteq.presustentaciones.services.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Catálogo de permisos del sistema y asignación de permisos a roles
 * ("Gestionar Permisos" en el panel de administrador). Reemplaza los
 * hasRole/hasAnyRole fijos en código -- ver PermisoService.tienePermiso,
 * invocado desde @PreAuthorize en cada controlador protegido.
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/permisos")
@RequiredArgsConstructor
@PreAuthorize("@permisoService.tienePermiso(authentication, 'ROLES_PERMISOS_GESTIONAR')")
public class PermisoController {

    private final PermisoRepository permisoRepository;
    private final RolUsuarioRepository rolUsuarioRepository;
    private final AuditoriaService auditoriaService;

    /** Catálogo completo de permisos disponibles, agrupado por categoría en el frontend. */
    @GetMapping
    public List<Permiso> listar() {
        return permisoRepository.findAllByOrderByCategoriaAscNombreAsc();
    }

    /**
     * Reemplaza por completo el conjunto de permisos de un rol (checkbox matrix en el
     * frontend: se envía la lista final de códigos marcados, no un delta).
     * Salvaguarda: si el resultado dejaría a NINGÚN rol en el sistema con
     * ROLES_PERMISOS_GESTIONAR, se rechaza -- de lo contrario un admin podría quitarse
     * a sí mismo (y a todos) el acceso para volver a corregirlo, sin salida salvo tocar
     * la base de datos directamente.
     */
    @PutMapping("/rol/{rolId}")
    @Transactional
    public ResponseEntity<?> actualizarPermisosDeRol(@PathVariable Short rolId, @RequestBody List<String> codigosPermisos) {
        RolUsuario rol = rolUsuarioRepository.findById(rolId).orElse(null);
        if (rol == null) {
            return ResponseEntity.notFound().build();
        }

        List<Permiso> permisos = permisoRepository.findByCodigoIn(codigosPermisos);
        if (permisos.size() != codigosPermisos.stream().distinct().count()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uno o más códigos de permiso no existen."));
        }

        boolean incluyeGestionPermisos = codigosPermisos.contains("ROLES_PERMISOS_GESTIONAR");
        if (!incluyeGestionPermisos) {
            List<Short> otrosRolesConEsePermiso = permisoRepository.findRolIdsConPermiso("ROLES_PERMISOS_GESTIONAR")
                    .stream().filter(id -> !id.equals(rolId)).toList();
            if (otrosRolesConEsePermiso.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "No se puede quitar 'Gestionar roles y permisos' de este rol: ningún otro rol lo tendría, " +
                        "y nadie podría volver a asignarlo desde la interfaz."));
            }
        }

        auditoriaService.marcarActorActual();
        permisoRepository.eliminarPermisosDeRol(rolId);
        for (Permiso p : permisos) {
            permisoRepository.asignarPermiso(rolId, p.getId());
        }
        return ResponseEntity.ok(permisoRepository.findCodigosPorRol(rolId));
    }
}
