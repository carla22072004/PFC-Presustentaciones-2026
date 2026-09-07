package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import ec.edu.uteq.presustentaciones.services.PermisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Datos de la sesión actual que el frontend necesita en caliente. En particular, los
 * permisos del usuario: el JWT solo lleva identidad y rol (por diseño, para que
 * "Gestionar Permisos" aplique sin re-login), así que el panel consulta aquí qué
 * módulos mostrar. Al retirar un permiso a un rol, el módulo correspondiente
 * desaparece del panel en la siguiente carga, sin cerrar sesión.
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final PermisoService permisoService;

    /**
     * @return 200 con la lista de códigos de permiso del usuario autenticado
     */
    @GetMapping("/permisos")
    public ResponseEntity<?> misPermisos(Authentication authentication) {
        return ResponseEntity.ok(ResponseWrapper.success(permisoService.permisosDe(authentication)));
    }
}
