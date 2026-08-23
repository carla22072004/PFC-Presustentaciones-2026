package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.repositories.PermisoRepository;
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
}
