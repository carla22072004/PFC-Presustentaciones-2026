package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.repositories.PermisoRepository;
import ec.edu.uteq.presustentaciones.repositories.DocenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Bean invocado desde @PreAuthorize("@permisoService.tienePermiso(authentication, 'CODIGO')")
 * en cada controlador protegido. Reemplaza los hasRole/hasAnyRole fijos en código: el rol
 * del usuario autenticado y sus permisos viven en la base de datos (roles_usuario,
 * permisos, rol_permisos) y son editables desde "Gestionar Roles" / "Gestionar Permisos".
 */
@Service("permisoService")
@RequiredArgsConstructor
public class PermisoService {

    private final PermisoRepository permisoRepository;
    private final DocenteRepository docenteRepository;

    public boolean tienePermiso(Authentication authentication, String codigoPermiso) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String email = authentication.getName();
        if (email == null || "anonymousUser".equals(email)) {
            return false;
        }
        return permisoRepository.usuarioTienePermiso(email, codigoPermiso);
    }

    /**
     * Códigos de permiso del usuario autenticado. El frontend los usa para ocultar los
     * módulos cuyo permiso se ha retirado al rol (sin re-login).
     */
    public java.util.List<String> permisosDe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return java.util.List.of();
        }
        String email = authentication.getName();
        if (email == null || "anonymousUser".equals(email)) {
            return java.util.List.of();
        }
        return permisoRepository.findCodigosPorEmail(email);
    }

    public boolean esPropioDocente(Authentication authentication, Long docenteId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String email = authentication.getName();
        if (email == null || "anonymousUser".equals(email)) {
            return false;
        }
        return docenteRepository.findById(docenteId)
                .map(docente -> docente.getUsuario() != null && email.equals(docente.getUsuario().getEmail()))
                .orElse(false);
    }
}
